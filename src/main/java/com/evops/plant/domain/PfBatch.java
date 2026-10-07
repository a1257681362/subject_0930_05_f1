package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 种植批次：关联品种、播种盘与计划采收日。批次号唯一。
 * 状态：DRAFT 建档 → SOWN 播种 → HARVESTED 采收 → QC_PASSED / QC_FAILED 质检
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_batch")
public class PfBatch extends BaseEntity {
    private String batchNo;
    private String batchName;
    private Long cultivarId;
    private Long trayId;
    /** 播种时确定所在分区 */
    private Long zoneId;
    private LocalDate plannedHarvestDate;
    private Integer seedQty;
    private String status;
    private LocalDateTime sownTime;
    private Long sownBy;
    private String remark;
}
