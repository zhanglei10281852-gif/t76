package com.mediation.controller;

import com.mediation.dto.MediatorDTO;
import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorLevel;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.repository.MediatorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mediators")
@RequiredArgsConstructor
public class MediatorController {

    private final MediatorRepository mediatorRepository;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MediatorDTO dto) {
        if (mediatorRepository.existsByIdCard(dto.getIdCard())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "该身份证号已存在"));
        }

        Mediator mediator = Mediator.builder()
                .name(dto.getName())
                .idCard(dto.getIdCard())
                .phone(dto.getPhone())
                .organization(dto.getOrganization())
                .speciality(dto.getSpeciality())
                .level(dto.getLevel() != null ? MediatorLevel.valueOf(dto.getLevel()) : MediatorLevel.初级)
                .build();

        Mediator saved = mediatorRepository.save(mediator);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Mediator>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Mediator> result;

        if (status != null && keyword != null) {
            MediatorStatus ms = MediatorStatus.valueOf(status);
            result = mediatorRepository.searchByStatusAndKeyword(ms, keyword, pageable);
        } else if (status != null) {
            MediatorStatus ms = MediatorStatus.valueOf(status);
            result = mediatorRepository.findByStatus(ms, pageable);
        } else if (keyword != null) {
            result = mediatorRepository.searchByKeyword(keyword, pageable);
        } else {
            result = mediatorRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return mediatorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "状态不能为空"));
        }

        try {
            MediatorStatus ms = MediatorStatus.valueOf(newStatus);
            return mediatorRepository.findById(id)
                    .map(mediator -> {
                        mediator.setStatus(ms);
                        mediatorRepository.save(mediator);
                        return ResponseEntity.ok(mediator);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的状态值，可选值：在岗/休假/离职"));
        }
    }
}
