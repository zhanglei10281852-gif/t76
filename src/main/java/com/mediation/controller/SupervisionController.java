package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.ListingSupervision;
import com.mediation.entity.Supervision;
import com.mediation.entity.Supervision.SupervisionLevel;
import com.mediation.entity.Supervision.SupervisionStatus;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.ListingSupervisionRepository;
import com.mediation.repository.SupervisionRepository;
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
@RequestMapping("/api/supervisions")
@RequiredArgsConstructor
public class SupervisionController {

    private final SupervisionRepository supervisionRepository;
    private final DisputeRepository disputeRepository;
    private final ListingSupervisionRepository listingSupervisionRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        String levelStr = (String) body.get("level");
        String reason = (String) body.get("reason");
        String requiredCloseDateStr = (String) body.get("requiredCloseDate");
        Long supervisorId = body.get("supervisorId") != null ? Long.valueOf(body.get("supervisorId").toString()) : null;
        String supervisorName = (String) body.get("supervisorName");

        if (disputeId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "案件ID不能为空"));
        }
        if (levelStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "督办级别不能为空"));
        }
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "督办原因不能为空"));
        }
        if (requiredCloseDateStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "要求办结日期不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() == Dispute.DisputeStatus.调解成功
            || dispute.getStatus() == Dispute.DisputeStatus.调解失败
            || dispute.getStatus() == Dispute.DisputeStatus.已撤回
            || dispute.getStatus() == Dispute.DisputeStatus.已终止) {
            return ResponseEntity.badRequest().body(Map.of("error", "已结案的案件不能发起督办"));
        }

        SupervisionLevel level;
        try {
            level = SupervisionLevel.valueOf(levelStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的督办级别"));
        }

        Supervision supervision = Supervision.builder()
                .disputeId(disputeId)
                .mediatorId(dispute.getMediatorId())
                .level(level)
                .supervisorId(supervisorId)
                .supervisorName(supervisorName)
                .reason(reason)
                .requiredCloseDate(java.time.LocalDate.parse(requiredCloseDateStr))
                .build();

        Supervision saved = supervisionRepository.save(supervision);

        int newCount = dispute.getSupervisionCount() == null ? 1 : dispute.getSupervisionCount() + 1;
        dispute.setSupervisionCount(newCount);
        disputeRepository.save(dispute);

        if (newCount >= 2 && !dispute.getListingSupervised()) {
            if (!listingSupervisionRepository.existsByDisputeId(disputeId)) {
                ListingSupervision ls = ListingSupervision.builder()
                        .disputeId(disputeId)
                        .upgradeReason("该案件已被督办" + newCount + "次，自动升级为挂牌督办")
                        .build();
                listingSupervisionRepository.save(ls);
                dispute.setListingSupervised(true);
                disputeRepository.save(dispute);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Supervision>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long disputeId,
            @RequestParam(required = false) Long mediatorId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            try {
                SupervisionStatus ss = SupervisionStatus.valueOf(status);
                return ResponseEntity.ok(supervisionRepository.findByStatus(ss, pageable));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (disputeId != null) {
            List<Supervision> list = supervisionRepository.findByDisputeIdOrderByCreatedAtDesc(disputeId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(list, pageable, list.size()));
        }
        if (mediatorId != null) {
            List<Supervision> list = supervisionRepository.findByMediatorIdOrderByCreatedAtDesc(mediatorId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(list, pageable, list.size()));
        }
        return ResponseEntity.ok(supervisionRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Supervision> opt = supervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<?> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String expectedDateStr = (String) body.get("replyExpectedDate");
        String progress = (String) body.get("replyProgress");
        String delayReason = (String) body.get("replyDelayReason");

        if (expectedDateStr == null || progress == null || progress.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "预计完成日期和当前进展不能为空"));
        }

        Optional<Supervision> opt = supervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Supervision supervision = opt.get();
        if (supervision.getStatus() != SupervisionStatus.待回复) {
            return ResponseEntity.badRequest().body(Map.of("error", "该督办已回复或已解除"));
        }

        supervision.setReplyExpectedDate(java.time.LocalDate.parse(expectedDateStr));
        supervision.setReplyProgress(progress);
        supervision.setReplyDelayReason(delayReason);
        supervision.setReplyTime(LocalDateTime.now());
        supervision.setStatus(SupervisionStatus.已回复);

        Supervision saved = supervisionRepository.save(supervision);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<?> dismiss(@PathVariable Long id) {
        Optional<Supervision> opt = supervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Supervision supervision = opt.get();
        supervision.setStatus(SupervisionStatus.已解除);
        Supervision saved = supervisionRepository.save(supervision);
        return ResponseEntity.ok(saved);
    }
}
