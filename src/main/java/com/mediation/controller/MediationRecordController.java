package com.mediation.controller;

import com.mediation.dto.MediationRecordDTO;
import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.MediationRecord;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.MediationRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class MediationRecordController {

    private final MediationRecordRepository recordRepository;
    private final DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MediationRecordDTO dto) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(dto.getDisputeId());
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "纠纷案件不存在"));
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.调解中) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有调解中的案件才能添加调解记录"));
        }

        MediationRecord record = MediationRecord.builder()
                .disputeId(dto.getDisputeId())
                .mediationDate(dto.getMediationDate())
                .location(dto.getLocation())
                .attendees(dto.getAttendees())
                .content(dto.getContent())
                .outcome(dto.getOutcome())
                .build();

        MediationRecord saved = recordRepository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<?> listByDispute(@RequestParam Long disputeId) {
        List<MediationRecord> records = recordRepository.findByDisputeIdOrderByMediationDateDesc(disputeId);
        return ResponseEntity.ok(records);
    }
}
