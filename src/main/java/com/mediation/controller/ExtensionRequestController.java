package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.ExtensionRequest;
import com.mediation.entity.ExtensionRequest.ExtensionStatus;
import com.mediation.entity.TimeLimitRule;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.ExtensionRequestRepository;
import com.mediation.repository.TimeLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/extension-requests")
@RequiredArgsConstructor
public class ExtensionRequestController {

    private final ExtensionRequestRepository extensionRequestRepository;
    private final DisputeRepository disputeRepository;
    private final TimeLimitRuleRepository timeLimitRuleRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        Long mediatorId = body.get("mediatorId") != null ? Long.valueOf(body.get("mediatorId").toString()) : null;
        Integer requestDays = body.get("requestDays") != null ? Integer.valueOf(body.get("requestDays").toString()) : null;
        String reason = (String) body.get("reason");

        if (disputeId == null || mediatorId == null || requestDays == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "案件ID、调解员ID、申请天数不能为空"));
        }
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "申请理由不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dispute dispute = disputeOpt.get();

        if (Boolean.TRUE.equals(dispute.getExtensionApplied())) {
            return ResponseEntity.badRequest().body(Map.of("error", "该案件已申请过延期，每案限延期一次"));
        }

        if (requestDays <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "申请天数必须大于0"));
        }

        int maxExtensionDays = 15;
        Optional<TimeLimitRule> ruleOpt = timeLimitRuleRepository.findByDisputeType(dispute.getDisputeType());
        if (ruleOpt.isPresent() && ruleOpt.get().getMaxExtensionDays() != null) {
            maxExtensionDays = ruleOpt.get().getMaxExtensionDays();
        }
        if (requestDays > maxExtensionDays) {
            return ResponseEntity.badRequest().body(Map.of("error", "申请天数不能超过最大延长期限" + maxExtensionDays + "天"));
        }

        ExtensionRequest request = ExtensionRequest.builder()
                .disputeId(disputeId)
                .mediatorId(mediatorId)
                .requestDays(requestDays)
                .reason(reason)
                .build();

        ExtensionRequest saved = extensionRequestRepository.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<ExtensionRequest>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long disputeId,
            @RequestParam(required = false) Long mediatorId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            try {
                ExtensionStatus es = ExtensionStatus.valueOf(status);
                return ResponseEntity.ok(extensionRequestRepository.findByStatus(es, pageable));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (disputeId != null) {
            List<ExtensionRequest> list = extensionRequestRepository.findByDisputeIdOrderByCreatedAtDesc(disputeId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(list, pageable, list.size()));
        }
        if (mediatorId != null) {
            List<ExtensionRequest> list = extensionRequestRepository.findByMediatorIdOrderByCreatedAtDesc(mediatorId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(list, pageable, list.size()));
        }
        return ResponseEntity.ok(extensionRequestRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<ExtensionRequest> opt = extensionRequestRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = body.get("approverId") != null ? Long.valueOf(body.get("approverId").toString()) : null;
        String approverName = (String) body.get("approverName");
        String remark = (String) body.get("approveRemark");
        Integer approvedDays = body.get("approvedDays") != null ? Integer.valueOf(body.get("approvedDays").toString()) : null;

        Optional<ExtensionRequest> opt = extensionRequestRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ExtensionRequest request = opt.get();
        if (request.getStatus() != ExtensionStatus.待审批) {
            return ResponseEntity.badRequest().body(Map.of("error", "该申请已处理"));
        }

        request.setStatus(ExtensionStatus.已批准);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApproveRemark(remark);
        request.setApprovedDays(approvedDays != null ? approvedDays : request.getRequestDays());
        request.setApproveTime(LocalDateTime.now());
        extensionRequestRepository.save(request);

        Optional<Dispute> disputeOpt = disputeRepository.findById(request.getDisputeId());
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            int extDays = dispute.getExtensionDays() == null ? 0 : dispute.getExtensionDays();
            dispute.setExtensionDays(extDays + request.getApprovedDays());
            dispute.setExtensionApplied(true);
            disputeRepository.save(dispute);
        }

        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long approverId = body.get("approverId") != null ? Long.valueOf(body.get("approverId").toString()) : null;
        String approverName = (String) body.get("approverName");
        String remark = (String) body.get("approveRemark");

        Optional<ExtensionRequest> opt = extensionRequestRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ExtensionRequest request = opt.get();
        if (request.getStatus() != ExtensionStatus.待审批) {
            return ResponseEntity.badRequest().body(Map.of("error", "该申请已处理"));
        }

        request.setStatus(ExtensionStatus.已拒绝);
        request.setApproverId(approverId);
        request.setApproverName(approverName);
        request.setApproveRemark(remark);
        request.setApproveTime(LocalDateTime.now());
        extensionRequestRepository.save(request);

        Optional<Dispute> disputeOpt = disputeRepository.findById(request.getDisputeId());
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            dispute.setExtensionApplied(true);
            disputeRepository.save(dispute);
        }

        return ResponseEntity.ok(request);
    }
}
