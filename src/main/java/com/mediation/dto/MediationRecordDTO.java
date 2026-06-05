package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MediationRecordDTO {

    @NotNull(message = "纠纷案件ID不能为空")
    private Long disputeId;

    @NotNull(message = "调解日期不能为空")
    private LocalDate mediationDate;

    @NotBlank(message = "调解地点不能为空")
    private String location;

    private String attendees;

    @NotBlank(message = "调解内容不能为空")
    private String content;

    private String outcome;
}
