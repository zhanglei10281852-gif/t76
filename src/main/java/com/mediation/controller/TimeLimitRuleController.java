package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.TimeLimitRule;
import com.mediation.repository.TimeLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/time-limit-rules")
@RequiredArgsConstructor
public class TimeLimitRuleController {

    private final TimeLimitRuleRepository timeLimitRuleRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TimeLimitRule rule) {
        if (rule.getDisputeType() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "纠纷类型不能为空"));
        }
        if (rule.getLimitDays() == null || rule.getLimitDays() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "时限天数必须大于0"));
        }
        if (timeLimitRuleRepository.existsByDisputeType(rule.getDisputeType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "该纠纷类型的时限规则已存在"));
        }
        TimeLimitRule saved = timeLimitRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<TimeLimitRule>> list() {
        return ResponseEntity.ok(timeLimitRuleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<TimeLimitRule> opt = timeLimitRuleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @GetMapping("/type/{disputeType}")
    public ResponseEntity<?> getByType(@PathVariable String disputeType) {
        try {
            Dispute.DisputeType type = Dispute.DisputeType.valueOf(disputeType);
            Optional<TimeLimitRule> opt = timeLimitRuleRepository.findByDisputeType(type);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(opt.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的纠纷类型"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TimeLimitRule rule) {
        Optional<TimeLimitRule> opt = timeLimitRuleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TimeLimitRule existing = opt.get();
        if (rule.getLimitDays() != null && rule.getLimitDays() > 0) {
            existing.setLimitDays(rule.getLimitDays());
        }
        if (rule.getAmountThreshold() != null) {
            existing.setAmountThreshold(rule.getAmountThreshold());
        }
        if (rule.getAmountExtraDays() != null) {
            existing.setAmountExtraDays(rule.getAmountExtraDays());
        }
        if (rule.getMaxExtensionDays() != null) {
            existing.setMaxExtensionDays(rule.getMaxExtensionDays());
        }
        if (rule.getRemark() != null) {
            existing.setRemark(rule.getRemark());
        }
        TimeLimitRule saved = timeLimitRuleRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!timeLimitRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        timeLimitRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
