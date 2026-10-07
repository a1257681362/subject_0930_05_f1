package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CultivarRequest {
    @NotBlank(message = "品种编码不能为空")
    private String cultivarCode;
    @NotBlank(message = "品种名称不能为空")
    private String cultivarName;
    private String category;
    private Integer defaultGrowDays;
    private String remark;
}
