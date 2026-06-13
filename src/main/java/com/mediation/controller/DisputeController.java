package com.mediation.controller;

import com.mediation.dto.DisputeDTO;
import com.mediation.entity.*;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import com.mediation.entity.ListingSupervision.ListingStatus;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.entity.ProgressLog;
import com.mediation.entity.Supervision.SupervisionStatus;
import com.mediation.entity.TimeLimitRule;
import com.mediation.repository.*;
import com.mediation.service.WarningService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;
    private final TimeLimitRuleRepository timeLimitRuleRepository;
    private final SupervisionRepository supervisionRepository;
    private final ProgressLogRepository progressLogRepository;
    private final ListingSupervisionRepository listingSupervisionRepository;
    private final WarningService warningService;

    @PostConstruct
    public void initDefaultRules() {
        Map<DisputeType, Integer> defaultRules = Map.of(
                DisputeType.邻里纠纷, 15,
                DisputeType.婚姻家庭, 30,
                DisputeType.劳动争议, 30,
                DisputeType.合同纠纷, 30,
                DisputeType.损害赔偿, 20,
                DisputeType.土地权属, 45,
                DisputeType.其他, 30
        );
        for (Map.Entry<DisputeType, Integer> entry : defaultRules.entrySet()) {
            if (!timeLimitRuleRepository.existsByDisputeType(entry.getKey())) {
                TimeLimitRule rule = TimeLimitRule.builder()
                        .disputeType(entry.getKey())
                        .limitDays(entry.getValue())
                        .amountThreshold(new BigDecimal("100000"))
                        .amountExtraDays(15)
                        .maxExtensionDays(15)
                        .remark("默认规则")
                        .build();
                timeLimitRuleRepository.save(rule);
            }
        }
    }

    private int getBaseLimitDays(DisputeType type) {
        Optional<TimeLimitRule> ruleOpt = timeLimitRuleRepository.findByDisputeType(type);
        if (ruleOpt.isPresent()) {
            return ruleOpt.get().getLimitDays();
        }
        return switch (type) {
            case 邻里纠纷 -> 15;
            case 婚姻家庭, 劳动争议, 合同纠纷 -> 30;
            case 损害赔偿 -> 20;
            case 土地权属 -> 45;
            default -> 30;
        };
    }

    private int getAmountExtraDays(DisputeType type, BigDecimal amount) {
        if (amount == null) return 0;
        Optional<TimeLimitRule> ruleOpt = timeLimitRuleRepository.findByDisputeType(type);
        BigDecimal threshold = ruleOpt.map(TimeLimitRule::getAmountThreshold)
                .orElse(new BigDecimal("100000"));
        if (amount.compareTo(threshold) > 0) {
            return ruleOpt.map(r -> r.getAmountExtraDays() != null ? r.getAmountExtraDays() : 15)
                    .orElse(15);
        }
        return 0;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DisputeDTO dto) {
        DisputeType disputeType;
        try {
            disputeType = DisputeType.valueOf(dto.getDisputeType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的纠纷类型"));
        }

        String caseNo = generateCaseNo();

        Dispute dispute = Dispute.builder()
                .caseNo(caseNo)
                .disputeType(disputeType)
                .applicantName(dto.getApplicantName())
                .applicantPhone(dto.getApplicantPhone())
                .applicantIdCard(dto.getApplicantIdCard())
                .respondentName(dto.getRespondentName())
                .respondentPhone(dto.getRespondentPhone())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .build();

        Dispute saved = disputeRepository.save(dispute);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Dispute>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String disputeType,
            @RequestParam(required = false) Long mediatorId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Dispute> result;

        if (status != null) {
            DisputeStatus ds = DisputeStatus.valueOf(status);
            result = disputeRepository.findByStatus(ds, pageable);
        } else if (disputeType != null) {
            DisputeType dt = DisputeType.valueOf(disputeType);
            result = disputeRepository.findByDisputeType(dt, pageable);
        } else if (mediatorId != null) {
            result = disputeRepository.findByMediatorId(mediatorId, pageable);
        } else {
            result = disputeRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", dispute.getId());
        response.put("caseNo", dispute.getCaseNo());
        response.put("disputeType", dispute.getDisputeType());
        response.put("applicantName", dispute.getApplicantName());
        response.put("applicantPhone", dispute.getApplicantPhone());
        response.put("applicantIdCard", dispute.getApplicantIdCard());
        response.put("respondentName", dispute.getRespondentName());
        response.put("respondentPhone", dispute.getRespondentPhone());
        response.put("description", dispute.getDescription());
        response.put("amount", dispute.getAmount());
        response.put("mediatorId", dispute.getMediatorId());
        response.put("status", dispute.getStatus());
        response.put("result", dispute.getResult());
        response.put("createdAt", dispute.getCreatedAt());
        response.put("updatedAt", dispute.getUpdatedAt());

        response.put("acceptDate", dispute.getAcceptDate());
        response.put("closeDate", dispute.getCloseDate());
        response.put("timeLimitDays", dispute.getTimeLimitDays());
        response.put("extensionDays", dispute.getExtensionDays());
        response.put("extensionApplied", dispute.getExtensionApplied());
        response.put("supervisionCount", dispute.getSupervisionCount());
        response.put("listingSupervised", dispute.getListingSupervised());

        int totalLimit = 0;
        if (dispute.getAcceptDate() != null && dispute.getTimeLimitDays() != null) {
            totalLimit = dispute.getTimeLimitDays() + (dispute.getExtensionDays() != null ? dispute.getExtensionDays() : 0);
        } else if (dispute.getAcceptDate() != null) {
            totalLimit = getBaseLimitDays(dispute.getDisputeType())
                    + getAmountExtraDays(dispute.getDisputeType(), dispute.getAmount())
                    + (dispute.getExtensionDays() != null ? dispute.getExtensionDays() : 0);
        }
        response.put("totalLimitDays", totalLimit);

        if (dispute.getMediatorId() != null) {
            mediatorRepository.findById(dispute.getMediatorId())
                    .ifPresent(mediator -> response.put("mediatorName", mediator.getName()));
        }

        WarningService.WarningItem warning = warningService.getDisputeWarning(dispute.getId());
        response.put("warningInfo", warning);

        List<ProgressLog> progressLogs = progressLogRepository
                .findByDisputeIdOrderByLogDateDescCreatedAtDesc(dispute.getId());
        response.put("progressTimeline", progressLogs);

        List<Supervision> supervisions = supervisionRepository
                .findByDisputeIdOrderByCreatedAtDesc(dispute.getId());
        response.put("supervisions", supervisions);

        Optional<ListingSupervision> lsOpt = listingSupervisionRepository.findByDisputeId(dispute.getId());
        lsOpt.ifPresent(listingSupervision -> response.put("listingSupervision", listingSupervision));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, Object> body) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.待受理) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有待受理的案件才能受理"));
        }

        LocalDate acceptDate = LocalDate.now();
        if (body != null && body.get("acceptDate") != null) {
            acceptDate = LocalDate.parse(body.get("acceptDate").toString());
        }

        int baseLimit = getBaseLimitDays(dispute.getDisputeType());
        int amountExtra = getAmountExtraDays(dispute.getDisputeType(), dispute.getAmount());
        int totalLimit = baseLimit + amountExtra;

        dispute.setStatus(DisputeStatus.已受理);
        dispute.setAcceptDate(acceptDate);
        dispute.setTimeLimitDays(baseLimit + amountExtra);

        if (body != null && body.get("note") != null) {
            ProgressLog log = ProgressLog.builder()
                    .disputeId(dispute.getId())
                    .logDate(acceptDate)
                    .content("案件已受理。" + body.get("note").toString())
                    .build();
            progressLogRepository.save(log);
        } else {
            ProgressLog log = ProgressLog.builder()
                    .disputeId(dispute.getId())
                    .logDate(acceptDate)
                    .content("案件已受理，办理时限" + totalLimit + "天（基础" + baseLimit + "天"
                            + (amountExtra > 0 ? "，涉及金额超10万延长" + amountExtra + "天" : "") + "）")
                    .build();
            progressLogRepository.save(log);
        }

        Dispute saved = disputeRepository.save(dispute);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long mediatorId = body.get("mediatorId") != null ? Long.valueOf(body.get("mediatorId").toString()) : null;
        String assignNote = (String) body.get("note");

        if (mediatorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "调解员ID不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.已受理) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有已受理的案件才能分配调解员"));
        }

        Optional<Mediator> mediatorOpt = mediatorRepository.findById(mediatorId);
        if (mediatorOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "调解员不存在"));
        }

        Mediator mediator = mediatorOpt.get();
        if (mediator.getStatus() != MediatorStatus.在岗) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "该调解员当前不在岗，无法分配"));
        }

        Long oldMediatorId = dispute.getMediatorId();

        dispute.setMediatorId(mediatorId);
        dispute.setStatus(DisputeStatus.调解中);
        disputeRepository.save(dispute);

        mediator.setCaseCount(mediator.getCaseCount() + 1);
        mediatorRepository.save(mediator);

        if (oldMediatorId != null && !oldMediatorId.equals(mediatorId)) {
            mediatorRepository.findById(oldMediatorId).ifPresent(oldM -> {
                if (oldM.getCaseCount() > 0) {
                    oldM.setCaseCount(oldM.getCaseCount() - 1);
                    mediatorRepository.save(oldM);
                }
            });
        }

        ProgressLog log = ProgressLog.builder()
                .disputeId(dispute.getId())
                .mediatorId(mediatorId)
                .logDate(LocalDate.now())
                .content("案件分配调解员：" + mediator.getName()
                        + (assignNote != null ? "。" + assignNote : "。开始调解程序"))
                .build();
        progressLogRepository.save(log);

        return ResponseEntity.ok(dispute);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String result = (String) body.get("result");
        Boolean success = (Boolean) body.get("success");
        String closeNote = (String) body.get("note");
        LocalDate closeDate = body.get("closeDate") != null
                ? LocalDate.parse(body.get("closeDate").toString()) : LocalDate.now();

        if (result == null || success == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "结果描述和是否成功不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.调解中) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有调解中的案件才能结案"));
        }

        dispute.setResult(result);
        dispute.setStatus(success ? DisputeStatus.调解成功 : DisputeStatus.调解失败);
        dispute.setCloseDate(closeDate);
        disputeRepository.save(dispute);

        supervisionRepository.updateStatusByDisputeId(
                dispute.getId(), SupervisionStatus.已解除, SupervisionStatus.已解除);

        listingSupervisionRepository.findByDisputeId(dispute.getId()).ifPresent(ls -> {
            if (ls.getStatus() != ListingStatus.已完成) {
                ls.setStatus(ListingStatus.已完成);
                listingSupervisionRepository.save(ls);
            }
        });

        String statusText = success ? "调解成功" : "调解失败";
        ProgressLog log = ProgressLog.builder()
                .disputeId(dispute.getId())
                .mediatorId(dispute.getMediatorId())
                .logDate(closeDate)
                .content("案件结案（" + statusText + "）：" + result
                        + (closeNote != null ? "。" + closeNote : ""))
                .build();
        progressLogRepository.save(log);

        return ResponseEntity.ok(dispute);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, Object> body) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.待受理 && dispute.getStatus() != DisputeStatus.已受理
                && dispute.getStatus() != DisputeStatus.调解中) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有待受理、已受理或调解中的案件才能撤回"));
        }

        String withdrawNote = body != null ? (String) body.get("note") : null;
        LocalDate closeDate = LocalDate.now();

        dispute.setStatus(DisputeStatus.已撤回);
        if (dispute.getAcceptDate() != null) {
            dispute.setCloseDate(closeDate);
        }
        if (withdrawNote != null) {
            dispute.setResult(withdrawNote);
        }
        disputeRepository.save(dispute);

        supervisionRepository.updateStatusByDisputeId(
                dispute.getId(), SupervisionStatus.已解除, SupervisionStatus.已解除);

        listingSupervisionRepository.findByDisputeId(dispute.getId()).ifPresent(ls -> {
            if (ls.getStatus() != ListingStatus.已完成) {
                ls.setStatus(ListingStatus.已完成);
                listingSupervisionRepository.save(ls);
            }
        });

        if (dispute.getAcceptDate() != null) {
            ProgressLog log = ProgressLog.builder()
                    .disputeId(dispute.getId())
                    .mediatorId(dispute.getMediatorId())
                    .logDate(closeDate)
                    .content("案件撤回" + (withdrawNote != null ? "：" + withdrawNote : ""))
                    .build();
            progressLogRepository.save(log);
        }

        return ResponseEntity.ok(dispute);
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<?> statsOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = disputeRepository.count();
        stats.put("total", total);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DisputeStatus status : DisputeStatus.values()) {
            byStatus.put(status.name(), disputeRepository.countByStatus(status));
        }
        stats.put("byStatus", byStatus);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (DisputeType type : DisputeType.values()) {
            byType.put(type.name(), disputeRepository.countByDisputeType(type));
        }
        stats.put("byType", byType);

        List<WarningService.WarningItem> warnings = warningService.scanAllWarnings();
        long yellow = warnings.stream().filter(w -> "黄牌".equals(w.warningLevel)).count();
        long red = warnings.stream().filter(w -> "红牌".equals(w.warningLevel)).count();
        stats.put("yellowWarningCount", yellow);
        stats.put("redWarningCount", red);
        stats.put("totalWarningCount", warnings.size());
        stats.put("listingSupervisionCount", listingSupervisionRepository.count());

        return ResponseEntity.ok(stats);
    }

    private String generateCaseNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "RM" + date + random;
    }
}
