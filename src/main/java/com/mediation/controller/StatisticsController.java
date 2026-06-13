package com.mediation.controller;

import com.mediation.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/avg-cycle-by-type")
    public ResponseEntity<Map<String, Object>> avgCycleByType() {
        return ResponseEntity.ok(statisticsService.avgCycleByType());
    }

    @GetMapping("/overdue-rate")
    public ResponseEntity<Map<String, Object>> overdueRate() {
        return ResponseEntity.ok(statisticsService.overdueRate());
    }

    @GetMapping("/mediator-overdue-ranking")
    public ResponseEntity<?> mediatorOverdueRanking() {
        return ResponseEntity.ok(statisticsService.mediatorOverdueRanking());
    }

    @GetMapping("/supervision-monthly-trend")
    public ResponseEntity<Map<String, Object>> supervisionMonthlyTrend(
            @RequestParam(defaultValue = "6") int months) {
        if (months <= 0 || months > 24) months = 6;
        return ResponseEntity.ok(statisticsService.supervisionMonthlyTrend(months));
    }

    @GetMapping("/extension-approval-rate")
    public ResponseEntity<Map<String, Object>> extensionApprovalRate() {
        return ResponseEntity.ok(statisticsService.extensionApprovalRate());
    }

    @GetMapping("/listing-supervision-stats")
    public ResponseEntity<Map<String, Object>> listingSupervisionStats() {
        return ResponseEntity.ok(statisticsService.listingSupervisionStats());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(statisticsService.comprehensiveDashboard());
    }
}
