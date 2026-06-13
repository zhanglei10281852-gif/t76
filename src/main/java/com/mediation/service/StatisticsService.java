package com.mediation.service;

import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import com.mediation.entity.ExtensionRequest;
import com.mediation.entity.ExtensionRequest.ExtensionStatus;
import com.mediation.entity.ListingSupervision;
import com.mediation.entity.Mediator;
import com.mediation.entity.Supervision;
import com.mediation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;
    private final SupervisionRepository supervisionRepository;
    private final ExtensionRequestRepository extensionRequestRepository;
    private final ListingSupervisionRepository listingSupervisionRepository;
    private final WarningService warningService;

    public Map<String, Object> avgCycleByType() {
        List<Dispute> closed = disputeRepository.findAllClosedWithDates();
        Map<DisputeType, List<Integer>> cyclesByType = new LinkedHashMap<>();

        for (Dispute d : closed) {
            if (d.getAcceptDate() == null || d.getCloseDate() == null) continue;
            int cycle = (int) ChronoUnit.DAYS.between(d.getAcceptDate(), d.getCloseDate());
            cyclesByType.computeIfAbsent(d.getDisputeType(), k -> new ArrayList<>()).add(cycle);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (DisputeType type : DisputeType.values()) {
            List<Integer> cycles = cyclesByType.getOrDefault(type, Collections.emptyList());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("count", cycles.size());
            if (!cycles.isEmpty()) {
                double avg = cycles.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                info.put("avgDays", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                info.put("minDays", cycles.stream().min(Integer::compareTo).orElse(0));
                info.put("maxDays", cycles.stream().max(Integer::compareTo).orElse(0));
            } else {
                info.put("avgDays", 0);
                info.put("minDays", 0);
                info.put("maxDays", 0);
            }
            result.put(type.name(), info);
        }
        return result;
    }

    public Map<String, Object> overdueRate() {
        List<Dispute> closed = disputeRepository.findAllClosedWithDates();
        int total = closed.size();
        int overdue = 0;

        for (Dispute d : closed) {
            if (d.getAcceptDate() == null || d.getCloseDate() == null) continue;
            int used = (int) ChronoUnit.DAYS.between(d.getAcceptDate(), d.getCloseDate());
            int limit = warningService.calculateTimeLimitDays(d);
            if (used > limit) {
                overdue++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalClosed", total);
        result.put("overdueCount", overdue);
        result.put("overdueRate", total > 0
                ? BigDecimal.valueOf(overdue * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        List<WarningService.WarningItem> activeWarnings = warningService.scanAllWarnings();
        int activeOverdue = (int) activeWarnings.stream().filter(w -> "红牌".equals(w.warningLevel)).count();
        result.put("activeOverdueCount", activeOverdue);
        return result;
    }

    public List<Map<String, Object>> mediatorOverdueRanking() {
        List<Mediator> mediators = mediatorRepository.findAll();
        List<Dispute> closed = disputeRepository.findAllClosedWithDates();

        Map<Long, Integer> mediatorTotal = new HashMap<>();
        Map<Long, Integer> mediatorOverdue = new HashMap<>();
        Map<Long, Integer> mediatorActiveOverdue = new HashMap<>();

        for (Dispute d : closed) {
            if (d.getMediatorId() == null) continue;
            mediatorTotal.merge(d.getMediatorId(), 1, Integer::sum);
            if (d.getAcceptDate() != null && d.getCloseDate() != null) {
                int used = (int) ChronoUnit.DAYS.between(d.getAcceptDate(), d.getCloseDate());
                int limit = warningService.calculateTimeLimitDays(d);
                if (used > limit) {
                    mediatorOverdue.merge(d.getMediatorId(), 1, Integer::sum);
                }
            }
        }

        List<WarningService.WarningItem> active = warningService.getRedWarnings();
        for (WarningService.WarningItem w : active) {
            if (w.mediatorId != null) {
                mediatorActiveOverdue.merge(w.mediatorId, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Mediator m : mediators) {
            int total = mediatorTotal.getOrDefault(m.getId(), 0);
            int overdue = mediatorOverdue.getOrDefault(m.getId(), 0);
            int activeOverdue = mediatorActiveOverdue.getOrDefault(m.getId(), 0);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("mediatorId", m.getId());
            info.put("mediatorName", m.getName());
            info.put("organization", m.getOrganization());
            info.put("totalClosed", total);
            info.put("overdueClosed", overdue);
            info.put("activeOverdue", activeOverdue);
            info.put("totalOverdue", overdue + activeOverdue);
            info.put("overdueRate", total + activeOverdue > 0
                    ? BigDecimal.valueOf((overdue + activeOverdue) * 100.0 / (total + activeOverdue)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            ranking.add(info);
        }

        ranking.sort((a, b) -> Integer.compare(
                ((Number) b.get("totalOverdue")).intValue(),
                ((Number) a.get("totalOverdue")).intValue()));
        return ranking;
    }

    public Map<String, Object> supervisionMonthlyTrend(int months) {
        YearMonth now = YearMonth.now();
        List<String> labels = new ArrayList<>();
        List<Integer> supervisionCounts = new ArrayList<>();
        List<Integer> replyCounts = new ArrayList<>();
        List<Integer> listingCounts = new ArrayList<>();

        List<Supervision> allSupervisions = supervisionRepository.findAll();
        List<ListingSupervision> allListings = listingSupervisionRepository.findAll();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            labels.add(ym.toString());

            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            int sCount = 0;
            int rCount = 0;
            for (Supervision s : allSupervisions) {
                if (s.getCreatedAt() == null) continue;
                LocalDate sd = s.getCreatedAt().toLocalDate();
                if (!sd.isBefore(start) && !sd.isAfter(end)) {
                    sCount++;
                    if (s.getReplyTime() != null) {
                        LocalDate rd = s.getReplyTime().toLocalDate();
                        if (!rd.isBefore(start) && !rd.isAfter(end)) {
                            rCount++;
                        }
                    }
                }
            }

            int lCount = 0;
            for (ListingSupervision ls : allListings) {
                if (ls.getUpgradeTime() == null) continue;
                LocalDate ld = ls.getUpgradeTime().toLocalDate();
                if (!ld.isBefore(start) && !ld.isAfter(end)) {
                    lCount++;
                }
            }

            supervisionCounts.add(sCount);
            replyCounts.add(rCount);
            listingCounts.add(lCount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", labels);
        result.put("supervisionCounts", supervisionCounts);
        result.put("replyCounts", replyCounts);
        result.put("listingCounts", listingCounts);
        return result;
    }

    public Map<String, Object> extensionApprovalRate() {
        List<ExtensionRequest> all = extensionRequestRepository.findAll();
        long total = all.size();
        long approved = extensionRequestRepository.countByStatus(ExtensionStatus.已批准);
        long rejected = extensionRequestRepository.countByStatus(ExtensionStatus.已拒绝);
        long pending = extensionRequestRepository.countByStatus(ExtensionStatus.待审批);
        long processed = approved + rejected;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", total);
        result.put("approvedCount", approved);
        result.put("rejectedCount", rejected);
        result.put("pendingCount", pending);
        result.put("approvalRate", processed > 0
                ? BigDecimal.valueOf(approved * 100.0 / processed).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.put("rejectionRate", processed > 0
                ? BigDecimal.valueOf(rejected * 100.0 / processed).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        Map<DisputeType, long[]> byType = new HashMap<>();
        for (ExtensionRequest r : all) {
            Optional<Dispute> dOpt = disputeRepository.findById(r.getDisputeId());
            if (dOpt.isEmpty()) continue;
            DisputeType type = dOpt.get().getDisputeType();
            byType.computeIfAbsent(type, k -> new long[3]);
            byType.get(type)[0]++;
            if (r.getStatus() == ExtensionStatus.已批准) byType.get(type)[1]++;
            if (r.getStatus() == ExtensionStatus.已拒绝) byType.get(type)[2]++;
        }

        Map<String, Object> typeRate = new LinkedHashMap<>();
        for (DisputeType type : DisputeType.values()) {
            long[] arr = byType.getOrDefault(type, new long[3]);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("total", arr[0]);
            info.put("approved", arr[1]);
            info.put("rejected", arr[2]);
            long p = arr[1] + arr[2];
            info.put("approvalRate", p > 0
                    ? BigDecimal.valueOf(arr[1] * 100.0 / p).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            typeRate.put(type.name(), info);
        }
        result.put("byType", typeRate);

        return result;
    }

    public Map<String, Object> listingSupervisionStats() {
        long total = listingSupervisionRepository.count();
        long pending = listingSupervisionRepository.countByStatus(ListingSupervision.ListingStatus.待处理);
        long processing = listingSupervisionRepository.countByStatus(ListingSupervision.ListingStatus.处理中);
        long completed = listingSupervisionRepository.countByStatus(ListingSupervision.ListingStatus.已完成);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalListing", total);
        result.put("pendingCount", pending);
        result.put("processingCount", processing);
        result.put("completedCount", completed);

        List<ListingSupervision> all = listingSupervisionRepository.findAll();
        Map<String, Integer> actionCounts = new LinkedHashMap<>();
        actionCounts.put("更换调解员", 0);
        actionCounts.put("召开协调会", 0);
        actionCounts.put("终止调解", 0);
        for (ListingSupervision ls : all) {
            if (ls.getAction() != null) {
                actionCounts.merge(ls.getAction().name(), 1, Integer::sum);
            }
        }
        result.put("actionDistribution", actionCounts);

        List<DisputeStatus> activeStatuses = List.of(DisputeStatus.已受理, DisputeStatus.调解中);
        List<Dispute> activeListing = disputeRepository.findListingSupervisedDisputes(activeStatuses);
        result.put("activeListingCount", activeListing.size());

        return result;
    }

    public Map<String, Object> comprehensiveDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgCycleByType", avgCycleByType());
        result.put("overdueRate", overdueRate());
        result.put("mediatorOverdueRanking", mediatorOverdueRanking());
        result.put("supervisionMonthlyTrend", supervisionMonthlyTrend(6));
        result.put("extensionApprovalRate", extensionApprovalRate());
        result.put("listingSupervisionStats", listingSupervisionStats());
        return result;
    }
}
