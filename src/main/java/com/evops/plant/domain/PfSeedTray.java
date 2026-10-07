package com.evops.plant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_seed_tray")
public class PfSeedTray extends BaseEntity {
    private String trayCode;
    private String trayName;
    /** 规格，如 72 孔穴盘 */
    private String spec;
    private Integer capacity;
    /** IDLE 空闲 / IN_USE 使用中 */
    private String status;
}
