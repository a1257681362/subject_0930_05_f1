package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class ZoneRequest {
    @NotBlank(message = "分区编码不能为空")
    private String zoneCode;
    @NotBlank(message = "分区名称不能为空")
    private String zoneName;
    /** NURSERY 育苗 / GROW 栽培 */
    private String zoneRole;
    private String lightingMode;
    private Integer dailyLightMinutes;
    private Integer ppfd;
    private String nutrientMode;
    private Integer cycleIntervalMin;
    private Integer cycleDurationMin;
    private BigDecimal ecTarget;
    private BigDecimal phTarget;
}
