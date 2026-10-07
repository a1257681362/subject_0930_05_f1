package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 采收单：等级、重量与去向。采收单号唯一。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_harvest")
public class PfHarvest extends BaseEntity {
    private String harvestNo;
    private Long batchId;
    /** 等级 A / B / C */
    private String grade;
    private BigDecimal grossWeightKg;
    private BigDecimal netWeightKg;
    /** 去向，如 冷库A */
    private String destination;
    private LocalDateTime harvestTime;
    private Long operatorId;
    private String remark;
}
