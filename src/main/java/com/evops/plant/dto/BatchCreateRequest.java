package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BatchCreateRequest {
    @NotBlank(message = "批次名称不能为空")
    private String batchName;
    @NotNull(message = "品种不能为空")
    private Long cultivarId;
    @NotNull(message = "播种盘不能为空")
    private Long trayId;
    private LocalDate plannedHarvestDate;
    private Integer seedQty;
    private String remark;
}
