package com.evops.plant;

import com.evops.EvopsApplication;
import com.evops.plant.job.StartupLedgerJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 模拟定时任务随应用两次重启：
 * - 每次启动 ApplicationReadyEvent 触发检查，运行期间定时方法再手动触发一次；
 * - 两次重启后，起步批次/分区/品种/播种盘都只能有一条，不得多写；
 * - 起步批次 create_by 必须是 SYSTEM(1)，不能丢操作者。
 */
class StartupIdempotencyTest {

    @TempDir
    Path tempDir;

    @Test
    void twoRestartsCreateExactlyOneStartupBatch() throws Exception {
        Path dbFile = tempDir.resolve("idem");
        String url = "jdbc:h2:file:" + dbFile.toAbsolutePath() + ";AUTO_SERVER=TRUE";

        // 第一次启动：应完成起步建档
        runOnce(url);
        assertCounts(url, 1, 1, 1, 1, "首次启动后");

        // 第二次启动（重启定时任务）：已存在，必须零写入
        runOnce(url);
        assertCounts(url, 1, 1, 1, 1, "重启后");

        // 操作者不丢失
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT create_by, status FROM pf_batch WHERE batch_no = 'PF-B-START-0001'")) {
                rs.next();
                assertEquals(1, rs.getLong("create_by"), "起步批次 create_by 必须是 SYSTEM(1)");
                assertEquals("DRAFT", rs.getString("status"));
            }
        }
    }

    private void runOnce(String url) {
        // 命令行参数优先级高于 application.yml，确保两次“重启”都落在同一个临时文件库
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(EvopsApplication.class)
                .run(
                        "--spring.datasource.url=" + url,
                        "--server.port=0");
        try {
            // 模拟运行期间定时巡检再触发一次：起步批次已存在时不得产生任何写入
            StartupLedgerJob job = ctx.getBean(StartupLedgerJob.class);
            job.checkOnSchedule();
            job.checkOnSchedule();
        } finally {
            ctx.close();
        }
    }

    private void assertCounts(String url, long batchCount, long zoneCount,
                              long cultivarCount, long trayCount, String phase) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement st = conn.createStatement()) {
            assertEquals(batchCount, count(st,
                    "SELECT COUNT(*) FROM pf_batch WHERE batch_no = 'PF-B-START-0001'"),
                    phase + " 起步批次数量错误（多写/漏写）");
            assertEquals(zoneCount, count(st, "SELECT COUNT(*) FROM pf_zone WHERE zone_code = 'Z-START'"),
                    phase + " 分区数量错误");
            assertEquals(cultivarCount, count(st, "SELECT COUNT(*) FROM pf_cultivar WHERE cultivar_code = 'C-LETTUCE-01'"),
                    phase + " 品种数量错误");
            assertEquals(trayCount, count(st, "SELECT COUNT(*) FROM pf_seed_tray WHERE tray_code = 'T-START-01'"),
                    phase + " 播种盘数量错误");
            // 起步阶段不应产生任何养护/转棚/采收/质检记录
            assertEquals(0L, count(st, "SELECT COUNT(*) FROM pf_care_log"), phase + " 不应有养护记录");
            assertEquals(0L, count(st, "SELECT COUNT(*) FROM pf_zone_transfer"), phase + " 不应有转棚记录");
            assertEquals(0L, count(st, "SELECT COUNT(*) FROM pf_harvest"), phase + " 不应有采收记录");
            assertEquals(0L, count(st, "SELECT COUNT(*) FROM pf_qc_record"), phase + " 不应有质检记录");
        }
    }

    private long count(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
