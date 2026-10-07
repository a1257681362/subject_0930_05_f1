package com.evops.plant.enums;

/** 批次状态：建档 → 播种 → 采收 → 质检 */
public enum BatchStatus {
    DRAFT,
    SOWN,
    HARVESTED,
    QC_PASSED,
    QC_FAILED
}
