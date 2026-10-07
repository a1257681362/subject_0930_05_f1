package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 播种：确定批次入区与播种量 */
@Data
public class SowRequest {
    @NotNull(message = "播种分区不能为空")
    private Long zoneId;
    private Integer seedQty;
}
