package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HarvestRequest {
    @NotBlank(message = "采收等级不能为空（A/B/C）")
    private String grade;
    @NotNull(message = "毛重不能为空")
    @Positive(message = "毛重必须大于 0")
    private BigDecimal grossWeightKg;
    private BigDecimal netWeightKg;
    @NotBlank(message = "采收去向不能为空")
    private String destination;
    private LocalDateTime harvestTime;
    private String remark;
}
