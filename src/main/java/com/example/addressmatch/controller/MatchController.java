package com.example.addressmatch.controller;

import com.example.addressmatch.entity.TableD;
import com.example.addressmatch.repository.TableDRepository;
import com.example.addressmatch.service.AddressMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/address")
public class MatchController {

    @Autowired
    private AddressMatchService addressMatchService;

    @Autowired
    private TableDRepository tableDRepo;


    /**
     * 执行性能监控匹配
     */
    @GetMapping("/match/perf")
    public ResponseEntity<Map<String, Object>> runPerformanceMatch() {
        log.info("执行性能监控匹配");

        long startTime = System.currentTimeMillis();
        addressMatchService.performSimpleMatch();
        long endTime = System.currentTimeMillis();

        Map<String, Object> stats = addressMatchService.getSimpleStats();
        stats.put("executionTime", endTime - startTime);
        stats.put("executionTimeSeconds", (endTime - startTime) / 1000.0);

        return ResponseEntity.ok(stats);
    }
    /**
     * 执行匹配
     */
    @GetMapping("/match")
    public ResponseEntity<Map<String, Object>> runMatch() {
        addressMatchService.performSimpleMatch();
        return ResponseEntity.ok(addressMatchService.getSimpleStats());
    }

    /**
     * 获取表D数据（<95%的匹配）
     */
    @GetMapping("/table-d")
    public ResponseEntity<List<TableD>> getTableD() {
        List<TableD> tableDList = tableDRepo.findByStatus("PENDING");
        return ResponseEntity.ok(tableDList);
    }

    /**
     * 表D地址添加到表A
     */
    @PostMapping("/d-to-a")
    public ResponseEntity<String> addDToA(@RequestBody List<Long> ids) {
        int added = addressMatchService.addTableDToTableA(ids);
        return ResponseEntity.ok("已添加" + added + "个地址到表A");
    }

    /**
     * 从表D移除
     */
    @PostMapping("/remove-d")
    public ResponseEntity<String> removeFromD(@RequestBody List<Long> ids) {
        int removed = addressMatchService.removeFromTableD(ids);
        return ResponseEntity.ok("已移除" + removed + "个地址");
    }

    /**
     * 重新匹配
     */
    @PostMapping("/rematch")
    public ResponseEntity<String> rematch() {
        addressMatchService.reMatchAll();
        return ResponseEntity.ok("重新匹配完成");
    }

    /**
     * 获取统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(addressMatchService.getSimpleStats());
    }
}