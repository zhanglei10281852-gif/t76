package com.mediation.service;

import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Mediator;
import com.mediation.entity.TimeLimitRule;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.MediatorRepository;
import com.mediation.repository.TimeLimitRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WarningService {

    private final DisputeRepository disputeRepository;
    private final TimeLimitRuleRepository timeLimitRuleRepository;
    private final MediatorRepository mediatorRepository;

    public static class WarningItem {
        public Long disputeId;
        public String caseNo;
        public Dispute.DisputeType disputeType;
        public String applicantName;
        public String respondentName;
        public Long mediatorId;
        public String mediatorName;
        public DisputeStatus status;
        public Integer timeLimitDays;
        public Integer extensionDays;
        public Integer usedDays;
        public Integer remainingDays;
        public String warningLevel;
        public BigDecimal amount;
        public LocalDate acceptDate;
        public Integer supervisionCount;
        public Boolean listingSupervised;
    }

    public Integer calculateTimeLimitDays(Dispute dispute) {
        if (dispute.getTimeLimitDays() != null) {
            int base = dispute.getTimeLimitDays();
            int ext = dispute.getExtensionDays() != null ? dispute.getExtensionDays() : 0;
            return base + ext;
        }
        int base = getBaseLimit(dispute.getDisputeType());
        int extra = 0;
        if (dispute.getAmount() != null && dispute.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            Optional<TimeLimitRule> ruleOpt = timeLimitRuleRepository.findByDisputeType(dispute.getDisputeType());
            extra = ruleOpt.map(r -> r.getAmountExtraDays() != null ? r.getAmountExtraDays() : 15).orElse(15);
        }
        int ext = dispute.getExtensionDays() != null ? dispute.getExtensionDays() : 0;
        return base + extra + ext;
    }

    private int getBaseLimit(Dispute.DisputeType type) {
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

    public List<WarningItem> scanAllWarnings() {
        List<DisputeStatus> activeStatuses = List.of(DisputeStatus.已受理, DisputeStatus.调解中);
        List<Dispute> activeDisputes = disputeRepository.findActiveDisputes(activeStatuses);

        List<WarningItem> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        Map<Long, String> mediatorCache = new HashMap<>();

        for (Dispute d : activeDisputes) {
            if (d.getAcceptDate() == null) continue;

            int totalLimit = calculateTimeLimitDays(d);
            long used = ChronoUnit.DAYS.between(d.getAcceptDate(), today);
            int usedDays = (int) used;
            int remainingDays = totalLimit - usedDays;

            String level;
            if (remainingDays < 0) {
                level = "红牌";
            } else if (remainingDays <= 3) {
                level = "黄牌";
            } else {
                continue;
            }

            WarningItem item = new WarningItem();
            item.disputeId = d.getId();
            item.caseNo = d.getCaseNo();
            item.disputeType = d.getDisputeType();
            item.applicantName = d.getApplicantName();
            item.respondentName = d.getRespondentName();
            item.mediatorId = d.getMediatorId();
            if (d.getMediatorId() != null) {
                if (mediatorCache.containsKey(d.getMediatorId())) {
                    item.mediatorName = mediatorCache.get(d.getMediatorId());
                } else {
                    Optional<Mediator> mOpt = mediatorRepository.findById(d.getMediatorId());
                    if (mOpt.isPresent()) {
                        item.mediatorName = mOpt.get().getName();
                        mediatorCache.put(d.getMediatorId(), item.mediatorName);
                    }
                }
            }
            item.status = d.getStatus();
            item.timeLimitDays = totalLimit;
            item.extensionDays = d.getExtensionDays() != null ? d.getExtensionDays() : 0;
            item.usedDays = usedDays;
            item.remainingDays = remainingDays;
            item.warningLevel = level;
            item.amount = d.getAmount();
            item.acceptDate = d.getAcceptDate();
            item.supervisionCount = d.getSupervisionCount() != null ? d.getSupervisionCount() : 0;
            item.listingSupervised = d.getListingSupervised() != null && d.getListingSupervised();

            result.add(item);
        }

        result.sort((a, b) -> {
            int levelCmp = a.warningLevel.compareTo(b.warningLevel);
            if (levelCmp != 0) return levelCmp;
            return Integer.compare(a.remainingDays, b.remainingDays);
        });

        return result;
    }

    public List<WarningItem> getYellowWarnings() {
        return scanAllWarnings().stream()
                .filter(w -> "黄牌".equals(w.warningLevel))
                .toList();
    }

    public List<WarningItem> getRedWarnings() {
        return scanAllWarnings().stream()
                .filter(w -> "红牌".equals(w.warningLevel))
                .toList();
    }

    public WarningItem getDisputeWarning(Long disputeId) {
        Optional<Dispute> opt = disputeRepository.findById(disputeId);
        if (opt.isEmpty()) return null;
        Dispute d = opt.get();
        if (d.getAcceptDate() == null) return null;
        if (d.getStatus() != DisputeStatus.已受理 && d.getStatus() != DisputeStatus.调解中) return null;

        LocalDate today = LocalDate.now();
        int totalLimit = calculateTimeLimitDays(d);
        long used = ChronoUnit.DAYS.between(d.getAcceptDate(), today);
        int usedDays = (int) used;
        int remainingDays = totalLimit - usedDays;

        String level;
        if (remainingDays < 0) {
            level = "红牌";
        } else if (remainingDays <= 3) {
            level = "黄牌";
        } else {
            level = "正常";
        }

        WarningItem item = new WarningItem();
        item.disputeId = d.getId();
        item.caseNo = d.getCaseNo();
        item.disputeType = d.getDisputeType();
        item.applicantName = d.getApplicantName();
        item.respondentName = d.getRespondentName();
        item.mediatorId = d.getMediatorId();
        if (d.getMediatorId() != null) {
            Optional<Mediator> mOpt = mediatorRepository.findById(d.getMediatorId());
            mOpt.ifPresent(mediator -> item.mediatorName = mediator.getName());
        }
        item.status = d.getStatus();
        item.timeLimitDays = totalLimit;
        item.extensionDays = d.getExtensionDays() != null ? d.getExtensionDays() : 0;
        item.usedDays = usedDays;
        item.remainingDays = remainingDays;
        item.warningLevel = level;
        item.amount = d.getAmount();
        item.acceptDate = d.getAcceptDate();
        item.supervisionCount = d.getSupervisionCount() != null ? d.getSupervisionCount() : 0;
        item.listingSupervised = d.getListingSupervised() != null && d.getListingSupervised();

        return item;
    }
}
