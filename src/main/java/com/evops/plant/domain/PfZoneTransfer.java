package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 转棚留痕：批次从哪个分区转到哪个分区、谁经办 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_zone_transfer")
public class PfZoneTransfer extends BaseEntity {
    private Long batchId;
    private Long fromZoneId;
    private Long toZoneId;
    private String reason;
    private LocalDateTime transferTime;
    private Long operatorId;
}
