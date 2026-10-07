package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 温室分区：光照、营养液循环、设备分区
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_zone")
public class PfZone extends BaseEntity {
    private String zoneCode;
    private String zoneName;
    /** NURSERY 育苗 / GROW 栽培 */
    private String zoneRole;
    /** 光照模式，如 LED-全光谱 */
    private String lightingMode;
    /** 日光照时长（分钟） */
    private Integer dailyLightMinutes;
    /** 光照强度 PPFD（μmol/m²/s） */
    private Integer ppfd;
    /** 营养液循环模式，如 NFT / DWC */
    private String nutrientMode;
    /** 循环间隔（分钟） */
    private Integer cycleIntervalMin;
    /** 每次循环时长（分钟） */
    private Integer cycleDurationMin;
    private BigDecimal ecTarget;
    private BigDecimal phTarget;
    private String status;
}
