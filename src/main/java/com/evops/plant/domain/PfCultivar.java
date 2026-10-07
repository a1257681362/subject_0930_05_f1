package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_cultivar")
public class PfCultivar extends BaseEntity {
    private String cultivarCode;
    private String cultivarName;
    /** 叶菜 / 草本 / 果菜 */
    private String category;
    /** 默认种植周期（天） */
    private Integer defaultGrowDays;
    private String remark;
}
