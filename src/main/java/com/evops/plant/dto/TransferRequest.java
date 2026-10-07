package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 转棚：目标分区必填，全程留痕 */
@Data
public class TransferRequest {
    @NotNull(message = "目标分区不能为空")
    private Long toZoneId;
    private String reason;
}
