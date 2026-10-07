package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 质检单：关联采收单与批次 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_qc_record")
public class PfQcRecord extends BaseEntity {
    private Long batchId;
    private Long harvestId;
    /** PASSED 合格 / FAILED 不合格 */
    private String result;
    private String issues;
    private LocalDateTime qcTime;
    private Long inspectorId;
}
