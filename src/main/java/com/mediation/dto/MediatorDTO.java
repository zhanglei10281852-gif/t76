package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MediatorDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "身份证号不能为空")
    @Size(min = 18, max = 18, message = "身份证号必须为18位")
    private String idCard;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "调解组织不能为空")
    private String organization;

    private String speciality;

    private String level;
}
