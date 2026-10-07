package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 养护记录：LIGHT 光照 / NUTRIENT 营养液循环 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_care_log")
public class PfCareLog extends BaseEntity {
    private Long batchId;
    private String logType;
    private Integer lightMinutes;
    private Integer ppfd;
    private BigDecimal ecValue;
    private BigDecimal phValue;
    private Integer volumeMl;
    private LocalDateTime measuredTime;
    private String note;
    private Long operatorId;
}
