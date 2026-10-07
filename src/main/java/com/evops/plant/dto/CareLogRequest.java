package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 养护记录：LIGHT 光照 / NUTRIENT 营养液循环 */
@Data
public class CareLogRequest {
    @NotBlank(message = "养护记录类型不能为空（LIGHT/NUTRIENT）")
    private String logType;
    private Integer lightMinutes;
    private Integer ppfd;
    private BigDecimal ecValue;
    private BigDecimal phValue;
    private Integer volumeMl;
    private LocalDateTime measuredTime;
    private String note;
}
