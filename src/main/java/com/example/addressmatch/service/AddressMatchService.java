package com.example.addressmatch.service;

import com.example.addressmatch.entity.*;
import com.example.addressmatch.model.AddressTreeNode;
import com.example.addressmatch.model.MatchCandidate;
import com.example.addressmatch.repository.TableARepository;
import com.example.addressmatch.repository.TableBRepository;
import com.example.addressmatch.repository.TableCRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddressMatchService {

    @Autowired
    private TableARepository tableARepo;

    @Autowired
    private TableBRepository tableBRepo;

    @Autowired
    private TableCRepository tableCRepo;

    @Autowired
    private AddressTreeBuilder treeBuilder;

    @Autowired
    private MultiGranularityMatcher matcher;

    @Autowired
    private Executor taskExecutor;

    @Transactional
    public void performCompleteMatch() {
        log.info("开始执行完整的地址匹配流程...");

        try {
            tableCRepo.deleteAll();
            log.info("已清空历史匹配结果");

            List<TableA> tableAList = tableARepo.findAll();
            List<TableB> tableBList = tableBRepo.findAll();

            log.info("数据加载完成，表A: {}条，表B: {}条", tableAList.size(), tableBList.size());

            treeBuilder.clearTree();
            treeBuilder.buildAddressTree(tableAList);
            AddressTreeNode root = treeBuilder.getRoot();

            List<TableC> allResults = new ArrayList<>();

            List<CompletableFuture<List<TableC>>> futures = new ArrayList<>();

            int batchSize = 1000;
            for (int i = 0; i < tableBList.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, tableBList.size());

                CompletableFuture<List<TableC>> future = CompletableFuture.supplyAsync(() -> {
                    return processBatch(tableBList.subList(start, end), root);
                }, taskExecutor);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<List<TableC>> future : futures) {
                try {
                    List<TableC> batchResults = future.get();
                    allResults.addAll(batchResults);
                } catch (Exception e) {
                    log.error("处理批次失败", e);
                }
            }

            tableCRepo.saveAll(allResults);

            log.info("地址匹配完成！处理{}条记录，找到{}个匹配",
                    tableBList.size(), allResults.size());

        } catch (Exception e) {
            log.error("地址匹配失败", e);
            throw new RuntimeException("地址匹配失败", e);
        }
    }

    private List<TableC> processBatch(List<TableB> batch, AddressTreeNode root) {
        List<TableC> batchResults = Collections.synchronizedList(new ArrayList<>());

        batch.parallelStream().forEach(b -> {
            try {
                List<MatchCandidate> candidates = matcher.matchAddress(b.getAddressB(), root);

                if (!candidates.isEmpty()) {
                    MatchCandidate best = candidates.get(0);
                    if (best.getScore() >= 0.95) {
                        TableC result = new TableC();
                        result.setAId(best.getTableA().getId());
                        result.setAddressB(b.getAddressB());
                        result.setMatchScore(best.getScore());
                        batchResults.add(result);
                    }
                }
            } catch (Exception e) {
                log.error("处理地址匹配失败: {}", b.getAddressB(), e);
            }
        });

        return batchResults;
    }

    public Map<String, Object> getMatchStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalBMatch = tableBRepo.count();
        long matchedCount = tableCRepo.count();
        double matchRate = totalBMatch > 0 ? (double) matchedCount / totalBMatch : 0;

        stats.put("totalBRecords", totalBMatch);
        stats.put("matchedRecords", matchedCount);
        stats.put("matchRate", String.format("%.2f%%", matchRate * 100));

        return stats;
    }
}