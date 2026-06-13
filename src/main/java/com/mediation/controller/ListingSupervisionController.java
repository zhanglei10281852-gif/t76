package com.mediation.controller;

import com.mediation.entity.Dispute;
import com.mediation.entity.ListingSupervision;
import com.mediation.entity.ListingSupervision.ListingAction;
import com.mediation.entity.ListingSupervision.ListingStatus;
import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.ListingSupervisionRepository;
import com.mediation.repository.MediatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/listing-supervisions")
@RequiredArgsConstructor
public class ListingSupervisionController {

    private final ListingSupervisionRepository listingSupervisionRepository;
    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long disputeId = body.get("disputeId") != null ? Long.valueOf(body.get("disputeId").toString()) : null;
        Long leaderId = body.get("leaderId") != null ? Long.valueOf(body.get("leaderId").toString()) : null;
        String leaderName = (String) body.get("leaderName");
        String upgradeReason = (String) body.get("upgradeReason");

        if (disputeId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "案件ID不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (listingSupervisionRepository.existsByDisputeId(disputeId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "该案件已挂牌督办"));
        }

        ListingSupervision ls = ListingSupervision.builder()
                .disputeId(disputeId)
                .leaderId(leaderId)
                .leaderName(leaderName)
                .upgradeReason(upgradeReason != null ? upgradeReason : "人工升级为挂牌督办")
                .build();

        ListingSupervision saved = listingSupervisionRepository.save(ls);

        Dispute dispute = disputeOpt.get();
        dispute.setListingSupervised(true);
        disputeRepository.save(dispute);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<Page<ListingSupervision>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            try {
                ListingStatus ls = ListingStatus.valueOf(status);
                return ResponseEntity.ok(listingSupervisionRepository.findByStatus(ls, pageable));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(listingSupervisionRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<ListingSupervision> opt = listingSupervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @GetMapping("/dispute/{disputeId}")
    public ResponseEntity<?> getByDispute(@PathVariable Long disputeId) {
        Optional<ListingSupervision> opt = listingSupervisionRepository.findByDisputeId(disputeId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @PutMapping("/{id}/change-mediator")
    public ResponseEntity<?> changeMediator(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long newMediatorId = body.get("newMediatorId") != null ? Long.valueOf(body.get("newMediatorId").toString()) : null;
        String newMediatorName = (String) body.get("newMediatorName");
        String remark = (String) body.get("actionRemark");

        if (newMediatorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "新调解员ID不能为空"));
        }

        Optional<ListingSupervision> opt = listingSupervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ListingSupervision ls = opt.get();

        Optional<Mediator> mediatorOpt = mediatorRepository.findById(newMediatorId);
        if (mediatorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "调解员不存在"));
        }
        Mediator mediator = mediatorOpt.get();
        if (mediator.getStatus() != MediatorStatus.在岗) {
            return ResponseEntity.badRequest().body(Map.of("error", "该调解员不在岗"));
        }

        ls.setAction(ListingAction.更换调解员);
        ls.setNewMediatorId(newMediatorId);
        ls.setNewMediatorName(newMediatorName != null ? newMediatorName : mediator.getName());
        ls.setActionRemark(remark);
        ls.setActionTime(LocalDateTime.now());
        ls.setStatus(ListingStatus.处理中);
        listingSupervisionRepository.save(ls);

        Optional<Dispute> disputeOpt = disputeRepository.findById(ls.getDisputeId());
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            dispute.setMediatorId(newMediatorId);
            disputeRepository.save(dispute);

            mediator.setCaseCount(mediator.getCaseCount() + 1);
            mediatorRepository.save(mediator);
        }

        return ResponseEntity.ok(ls);
    }

    @PutMapping("/{id}/hold-meeting")
    public ResponseEntity<?> holdMeeting(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String meetingDateStr = (String) body.get("meetingDate");
        String attendees = (String) body.get("meetingAttendees");
        String resolution = (String) body.get("meetingResolution");
        String remark = (String) body.get("actionRemark");

        if (meetingDateStr == null || resolution == null || resolution.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "会议日期和决议事项不能为空"));
        }

        Optional<ListingSupervision> opt = listingSupervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ListingSupervision ls = opt.get();

        ls.setAction(ListingAction.召开协调会);
        ls.setMeetingDate(java.time.LocalDate.parse(meetingDateStr));
        ls.setMeetingAttendees(attendees);
        ls.setMeetingResolution(resolution);
        ls.setActionRemark(remark);
        ls.setActionTime(LocalDateTime.now());
        ls.setStatus(ListingStatus.处理中);
        listingSupervisionRepository.save(ls);

        return ResponseEntity.ok(ls);
    }

    @PutMapping("/{id}/terminate")
    public ResponseEntity<?> terminate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String remark = (String) body.get("actionRemark");

        Optional<ListingSupervision> opt = listingSupervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ListingSupervision ls = opt.get();

        ls.setAction(ListingAction.终止调解);
        ls.setActionRemark(remark);
        ls.setActionTime(LocalDateTime.now());
        ls.setStatus(ListingStatus.已完成);
        listingSupervisionRepository.save(ls);

        Optional<Dispute> disputeOpt = disputeRepository.findById(ls.getDisputeId());
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            dispute.setStatus(Dispute.DisputeStatus.已终止);
            dispute.setCloseDate(java.time.LocalDate.now());
            dispute.setResult(remark != null ? remark : "挂牌督办终止调解，转其他途径处理");
            disputeRepository.save(dispute);
        }

        return ResponseEntity.ok(ls);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id) {
        Optional<ListingSupervision> opt = listingSupervisionRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ListingSupervision ls = opt.get();
        ls.setStatus(ListingStatus.已完成);
        listingSupervisionRepository.save(ls);
        return ResponseEntity.ok(ls);
    }
}
