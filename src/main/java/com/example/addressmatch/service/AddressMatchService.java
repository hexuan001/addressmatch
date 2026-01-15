package com.example.addressmatch.service;

import com.example.addressmatch.entity.*;
import com.example.addressmatch.model.AddressTreeNode;
import com.example.addressmatch.model.MatchCandidate;
import com.example.addressmatch.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class AddressMatchService {

    @Autowired private TableARepository tableARepo;
    @Autowired private TableBRepository tableBRepo;
    @Autowired private TableCRepository tableCRepo;
    @Autowired private TableDRepository tableDRepo;
    @Autowired private AddressTreeBuilder treeBuilder;
    @Autowired private MultiGranularityMatcher matcher;

    @Transactional
    public void performSimpleMatch() {
        long totalStart = System.currentTimeMillis();
        log.info("=== 开始地址匹配（缓存优化版） ===");

        try {
            // ================ 清理缓存 ================
            matcher.clearCache();
            log.info("已清理匹配器缓存");
            // ==============================================

            // 阶段1：清空历史数据
            long stage1Start = System.currentTimeMillis();
            tableCRepo.deleteAll();
            tableDRepo.deleteAll();
            long stage1Time = System.currentTimeMillis() - stage1Start;
            log.info("阶段1-清空数据: {}ms", stage1Time);

            // 阶段2：加载数据
            long stage2Start = System.currentTimeMillis();
            List<TableA> tableAList = tableARepo.findAll();
            List<TableB> tableBList = tableBRepo.findAll();
            long stage2Time = System.currentTimeMillis() - stage2Start;
            log.info("阶段2-加载数据: {}ms, 表A:{}条, 表B:{}条",
                    stage2Time, tableAList.size(), tableBList.size());

            // 阶段3：构建地址树
            long stage3Start = System.currentTimeMillis();
            treeBuilder.clearTree();
            treeBuilder.buildAddressTree(tableAList);
            AddressTreeNode root = treeBuilder.getRoot();
            long stage3Time = System.currentTimeMillis() - stage3Start;
            log.info("阶段3-构建地址树: {}ms", stage3Time);

            // 准备结果集合
            List<TableC> tableCResults = new ArrayList<>();
            List<TableD> tableDResults = new ArrayList<>();

            // 阶段4：匹配计算（带缓存）
            long stage4Start = System.currentTimeMillis();
            int processed = 0;
            int successMatch = 0;
            int failedMatch = 0;

            // 每处理一定数量记录一次时间
            int logInterval = Math.max(10, tableBList.size() / 10);

            for (TableB tableB : tableBList) {
                processed++;
                Long bId = tableB.getId();
                String addressB = tableB.getAddressB();

                try {
                    // 单条匹配开始时间
                    long matchStart = System.currentTimeMillis();
                    List<MatchCandidate> candidates = matcher.matchAddress(addressB, root);
                    long matchTime = System.currentTimeMillis() - matchStart;

                    // 记录慢查询（超过100ms）
                    if (matchTime > 100) {
                        log.warn("慢匹配: {}ms, 地址: {}", matchTime, addressB);
                    }

                    if (candidates != null && !candidates.isEmpty()) {
                        MatchCandidate best = candidates.get(0);

                        if (best.getScore() >= 0.95) {
                            // 成功匹配
                            TableC result = new TableC();
                            result.setAId(best.getTableA().getId());
                            result.setAddressB(addressB);
                            result.setMatchScore(best.getScore());
                            tableCResults.add(result);
                            successMatch++;
                        } else {
                            // 低分匹配
                            TableD failed = new TableD();
                            failed.setBId(bId);
                            failed.setAddressB(addressB);
                            failed.setStatus("PENDING");
                            tableDResults.add(failed);
                            failedMatch++;
                        }
                    } else {
                        // 无匹配
                        TableD failed = new TableD();
                        failed.setBId(bId);
                        failed.setAddressB(addressB);
                        failed.setStatus("PENDING");
                        tableDResults.add(failed);
                        failedMatch++;
                    }

                    // 定期输出进度
                    if (processed % logInterval == 0) {
                        long currentTime = System.currentTimeMillis() - stage4Start;
                        double speed = processed * 1000.0 / currentTime;
                        log.info("匹配进度: {}/{} ({}%), 耗时: {}ms, 速度: {:.1f}条/秒",
                                processed, tableBList.size(),
                                (processed * 100) / tableBList.size(),
                                currentTime, speed);
                    }

                } catch (Exception e) {
                    log.error("处理失败 BID: {}", bId, e);
                    TableD failed = new TableD();
                    failed.setBId(bId);
                    failed.setAddressB(addressB);
                    failed.setStatus("PENDING");
                    tableDResults.add(failed);
                    failedMatch++;
                }
            }

            long stage4Time = System.currentTimeMillis() - stage4Start;
            log.info("阶段4-匹配计算完成: {}ms", stage4Time);
            log.info("匹配统计: 成功={}, 失败={}, 平均速度={:.1f}条/秒",
                    successMatch, failedMatch, tableBList.size() * 1000.0 / stage4Time);

            // ================ 新增：输出缓存统计 ================
            Map<String, Object> cacheStats = matcher.getCacheStats();
            log.info("缓存统计: 解析缓存={}条, 匹配缓存={}条",
                    cacheStats.get("parseCacheSize"), cacheStats.get("matchCacheSize"));
            // ==================================================

            // 阶段5：批量保存
            long stage5Start = System.currentTimeMillis();
            if (!tableCResults.isEmpty()) {
                tableCRepo.saveAll(tableCResults);
                log.info("保存表C: {}条", tableCResults.size());
            }
            if (!tableDResults.isEmpty()) {
                tableDRepo.saveAll(tableDResults);
                log.info("保存表D: {}条", tableDResults.size());
            }
            long stage5Time = System.currentTimeMillis() - stage5Start;
            log.info("阶段5-批量保存: {}ms", stage5Time);

            // 总耗时统计
            long totalTime = System.currentTimeMillis() - totalStart;
            log.info("=== 匹配完成 ===");
            log.info("总耗时: {}ms ({:.1f}秒)", totalTime, totalTime / 1000.0);
            log.info("各阶段耗时占比:");
            log.info("  清空数据: {}ms ({}%)", stage1Time, (stage1Time * 100) / totalTime);
            log.info("  加载数据: {}ms ({}%)", stage2Time, (stage2Time * 100) / totalTime);
            log.info("  构建地址树: {}ms ({}%)", stage3Time, (stage3Time * 100) / totalTime);
            log.info("  匹配计算: {}ms ({}%)", stage4Time, (stage4Time * 100) / totalTime);
            log.info("  批量保存: {}ms ({}%)", stage5Time, (stage5Time * 100) / totalTime);
            log.info("平均速度: {:.1f}条/秒", tableBList.size() * 1000.0 / totalTime);

        } catch (Exception e) {
            log.error("匹配流程异常", e);
            throw new RuntimeException("地址匹配失败", e);
        }
    }

    /**
     * 重新匹配（在表A更新后）
     */
    @Transactional
    public void reMatchAll() {
        log.info("重新执行匹配...");
        performSimpleMatch();
    }

    /**
     * 将表D地址添加到表A（人工确认后）
     */
    @Transactional
    public int addTableDToTableA(List<Long> tableDIds) {
        int added = 0;

        for (Long id : tableDIds) {
            TableD tableD = tableDRepo.findById(id).orElse(null);
            if (tableD != null && "PENDING".equals(tableD.getStatus())) {
                // 添加到表A
                TableA newAddress = new TableA();
                newAddress.setAddressA(tableD.getAddressB());
                tableARepo.save(newAddress);

                // 更新表D状态
                tableD.setStatus("ADDED");
                tableDRepo.save(tableD);
                added++;
            }
        }

        if (added > 0) {
            log.info("已添加 {} 个地址到表A", added);
        }

        return added;
    }

    /**
     * 从表D移除地址（人工驳回）
     */
    @Transactional
    public int removeFromTableD(List<Long> tableDIds) {
        int removed = 0;

        for (Long id : tableDIds) {
            TableD tableD = tableDRepo.findById(id).orElse(null);
            if (tableD != null && "PENDING".equals(tableD.getStatus())) {
                tableD.setStatus("REJECTED");
                tableDRepo.save(tableD);
                removed++;
            }
        }

        if (removed > 0) {
            log.info("已从表D移除 {} 个地址", removed);
        }

        return removed;
    }

    /**
     * 获取简单统计
     */
    public Map<String, Object> getSimpleStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalB = tableBRepo.count();
        long inTableC = tableCRepo.count();
        long inTableD = tableDRepo.countByStatus("PENDING");

        stats.put("totalBRecords", totalB);
        stats.put("inTableC", inTableC);
        stats.put("inTableD", inTableD);
        stats.put("matchRate", String.format("%.2f%%", totalB > 0 ? (double) inTableC / totalB * 100 : 0));

        return stats;
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        return matcher.getCacheStats();
    }

    /**
     * 清理缓存
     */
    public void clearMatcherCache() {
        matcher.clearCache();
        log.info("已手动清理匹配器缓存");
    }
}
