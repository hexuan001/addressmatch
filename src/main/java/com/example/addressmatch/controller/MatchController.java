package com.example.addressmatch.controller;

import com.example.addressmatch.service.AddressMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/address")
public class MatchController {

    @Autowired
    private AddressMatchService addressMatchService;

    @RequestMapping(value = "/match", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> performMatch() {
        try {
            addressMatchService.performCompleteMatch();
            return ResponseEntity.ok("地址匹配完成！");
        } catch (Exception e) {
            log.error("地址匹配失败", e);
            return ResponseEntity.status(500).body("匹配失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/statistics", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = addressMatchService.getMatchStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}