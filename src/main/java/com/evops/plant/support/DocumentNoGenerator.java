package com.evops.plant.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 单据号生成：批次号 PF-B-####、采收单号 PF-H-####。
 * 基于 H2 持久化序列，重启不重置；配合唯一约束保证编号唯一。
 */
@Component
public class DocumentNoGenerator {

    private final JdbcTemplate jdbcTemplate;

    public DocumentNoGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextBatchNo() {
        return "PF-B-" + String.format("%04d", nextValue("seq_pf_batch_no"));
    }

    public String nextHarvestNo() {
        return "PF-H-" + String.format("%04d", nextValue("seq_pf_harvest_no"));
    }

    private long nextValue(String sequenceName) {
        return jdbcTemplate.queryForObject("SELECT NEXT VALUE FOR " + sequenceName, Long.class);
    }
}
