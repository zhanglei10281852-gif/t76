package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DisputeDTO {

    @NotNull(message = "纠纷类型不能为空")
    private String disputeType;

    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    @NotBlank(message = "申请人电话不能为空")
    private String applicantPhone;

    @NotBlank(message = "申请人身份证号不能为空")
    private String applicantIdCard;

    @NotBlank(message = "被申请人姓名不能为空")
    private String respondentName;

    private String respondentPhone;

    private String description;

    private BigDecimal amount;
}
