package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.ProgressLog;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.ProgressLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/progress-logs")
@RequiredArgsConstructor
public class ProgressLogController {

    private final ProgressLogRepository progressLogRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        Long mediatorId = body.get("mediatorId") != null ? Long.valueOf(body.get("mediatorId").toString()) : null;
        String logDateStr = (String) body.get("logDate");
        String content = (String) body.get("content");

        if (disputeId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "案件ID不能为空"));
        }
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "进展内容不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProgressLog log = ProgressLog.builder()
                .disputeId(disputeId)
                .mediatorId(mediatorId)
                .logDate(logDateStr != null ? java.time.LocalDate.parse(logDateStr) : java.time.LocalDate.now())
                .content(content)
                .build();

        ProgressLog saved = progressLogRepository.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/dispute/{disputeId}")
    public ResponseEntity<?> getByDispute(@PathVariable Long disputeId) {
        List<ProgressLog> logs = progressLogRepository.findByDisputeIdOrderByLogDateDescCreatedAtDesc(disputeId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/mediator/{mediatorId}")
    public ResponseEntity<?> getByMediator(@PathVariable Long mediatorId) {
        List<ProgressLog> logs = progressLogRepository.findByMediatorIdOrderByLogDateDesc(mediatorId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<ProgressLog> opt = progressLogRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!progressLogRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        progressLogRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
