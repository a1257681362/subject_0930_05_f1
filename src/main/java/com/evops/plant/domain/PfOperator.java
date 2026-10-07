package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_operator")
public class PfOperator extends BaseEntity {
    private String operatorCode;
    private String operatorName;
    private String status;
}
