package com.mediation.controller;

import com.mediation.service.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningService warningService;

    @GetMapping("/scan")
    public ResponseEntity<List<WarningService.WarningItem>> scanAll() {
        return ResponseEntity.ok(warningService.scanAllWarnings());
    }

    @GetMapping("/yellow")
    public ResponseEntity<List<WarningService.WarningItem>> yellowList() {
        return ResponseEntity.ok(warningService.getYellowWarnings());
    }

    @GetMapping("/red")
    public ResponseEntity<List<WarningService.WarningItem>> redList() {
        return ResponseEntity.ok(warningService.getRedWarnings());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<?> getByLevel(@PathVariable String level) {
        if ("黄牌".equals(level)) {
            return ResponseEntity.ok(warningService.getYellowWarnings());
        } else if ("红牌".equals(level)) {
            return ResponseEntity.ok(warningService.getRedWarnings());
        } else if ("全部".equals(level) || "all".equalsIgnoreCase(level)) {
            return ResponseEntity.ok(warningService.scanAllWarnings());
        }
        return ResponseEntity.badRequest().body(Map.of("error", "无效的预警等级"));
    }

    @GetMapping("/dispute/{disputeId}")
    public ResponseEntity<?> getByDispute(@PathVariable Long disputeId) {
        WarningService.WarningItem item = warningService.getDisputeWarning(disputeId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        List<WarningService.WarningItem> all = warningService.scanAllWarnings();
        long yellow = all.stream().filter(w -> "黄牌".equals(w.warningLevel)).count();
        long red = all.stream().filter(w -> "红牌".equals(w.warningLevel)).count();
        long listing = all.stream().filter(w -> Boolean.TRUE.equals(w.listingSupervised)).count();
        long supervised = all.stream().filter(w -> w.supervisionCount != null && w.supervisionCount > 0).count();

        Map<String, Object> result = Map.of(
                "totalWarning", all.size(),
                "yellowCount", yellow,
                "redCount", red,
                "listingCount", listing,
                "supervisedCount", supervised
        );
        return ResponseEntity.ok(result);
    }
}
