package com.evops.plant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全流程 HTTP 集成测试：建档 → 播种 → 养护 → 转棚 → 采收 → 质检。
 * 起步批次由启动定时任务自动创建（SYSTEM 操作者），业务接口以 bootstrap 身份调用。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerFlowIntegrationTest {

    private static final String AUTH = "Basic Ym9vdHN0cmFwOmJvb3RzdHJhcA=="; // bootstrap:bootstrap

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullFlow_create_sow_care_transfer_harvest_qc() throws Exception {
        // ---- 建档：分区 / 品种 / 播种盘 ----
        long zoneId = postJson("/api/pf/zones",
                "{\"zoneCode\":\"Z-A1\",\"zoneName\":\"一号栽培区\",\"zoneRole\":\"GROW\","
                        + "\"lightingMode\":\"LED-全光谱\",\"dailyLightMinutes\":960,\"ppfd\":220,"
                        + "\"nutrientMode\":\"NFT\",\"cycleIntervalMin\":30,\"cycleDurationMin\":5,"
                        + "\"ecTarget\":1.8,\"phTarget\":6.0}");
        long zoneBId = postJson("/api/pf/zones",
                "{\"zoneCode\":\"Z-B1\",\"zoneName\":\"二号栽培区\",\"zoneRole\":\"GROW\","
                        + "\"lightingMode\":\"LED-红蓝\",\"nutrientMode\":\"DWC\"}");
        long cultivarId = postJson("/api/pf/cultivars",
                "{\"cultivarCode\":\"C-BASIL-01\",\"cultivarName\":\"罗勒\",\"category\":\"草本\","
                        + "\"defaultGrowDays\":35}");
        long trayId = postJson("/api/pf/trays",
                "{\"trayCode\":\"T-001\",\"trayName\":\"50孔穴盘\",\"spec\":\"50孔\",\"capacity\":50}");

        // 重复编码建档应被拒绝
        mockMvc.perform(post("/api/pf/trays")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trayCode\":\"T-001\",\"trayName\":\"重复盘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        // ---- 批次建档 ----
        long batchId = postJson("/api/pf/batches",
                "{\"batchName\":\"罗勒第一批\",\"cultivarId\":" + cultivarId + ",\"trayId\":" + trayId
                        + ",\"plannedHarvestDate\":\"2026-11-06\",\"seedQty\":50}");

        // 批次号自动生成且唯一
        mockMvc.perform(get("/api/pf/batches/" + batchId).header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.createBy").value(2)); // bootstrap 操作员

        // 未播种不能养护
        mockMvc.perform(post("/api/pf/batches/" + batchId + "/care-logs")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logType\":\"LIGHT\",\"lightMinutes\":900}"))
                .andExpect(jsonPath("$.success").value(false));

        // ---- 播种 ----
        mockMvc.perform(post("/api/pf/batches/" + batchId + "/sow")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":" + zoneId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SOWN"))
                .andExpect(jsonPath("$.data.zoneId").value(zoneId))
                .andExpect(jsonPath("$.data.sownBy").value(2));

        // ---- 养护：光照 + 营养液循环 ----
        long lightLogId = postJson("/api/pf/batches/" + batchId + "/care-logs",
                "{\"logType\":\"LIGHT\",\"lightMinutes\":960,\"ppfd\":220,\"note\":\"全日照补光\"}");
        long nutrientLogId = postJson("/api/pf/batches/" + batchId + "/care-logs",
                "{\"logType\":\"NUTRIENT\",\"ecValue\":1.75,\"phValue\":6.1,\"volumeMl\":5000}");
        assertTrue(lightLogId > 0);
        assertTrue(nutrientLogId > 0);

        // ---- 转棚（留痕）----
        mockMvc.perform(post("/api/pf/batches/" + batchId + "/transfers")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toZoneId\":" + zoneBId + ",\"reason\":\"进入结果期换区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fromZoneId").value(zoneId))
                .andExpect(jsonPath("$.data.toZoneId").value(zoneBId))
                .andExpect(jsonPath("$.data.operatorId").value(2));

        mockMvc.perform(get("/api/pf/batches/" + batchId + "/transfers").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // ---- 采收：等级 / 重量 / 去向 ----
        long harvestId = postJson("/api/pf/batches/" + batchId + "/harvests",
                "{\"grade\":\"A\",\"grossWeightKg\":12.500,\"netWeightKg\":11.200,"
                        + "\"destination\":\"冷库A\",\"remark\":\"首批采收\"}");
        mockMvc.perform(get("/api/pf/batches/" + batchId).header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.status").value("HARVESTED"));
        mockMvc.perform(get("/api/pf/batches/" + batchId + "/harvests").header("Authorization", AUTH))
                .andExpect(jsonPath("$.data[0].harvestNo").isNotEmpty())
                .andExpect(jsonPath("$.data[0].grade").value("A"))
                .andExpect(jsonPath("$.data[0].destination").value("冷库A"));

        // 非法等级
        mockMvc.perform(post("/api/pf/batches/" + batchId + "/harvests")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"X\",\"grossWeightKg\":1,\"destination\":\"冷库A\"}"))
                .andExpect(jsonPath("$.success").value(false));

        // ---- 质检 ----
        mockMvc.perform(post("/api/pf/batches/" + batchId + "/qc")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASSED\",\"issues\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PASSED"))
                .andExpect(jsonPath("$.data.harvestId").value(harvestId))
                .andExpect(jsonPath("$.data.inspectorId").value(2));
        mockMvc.perform(get("/api/pf/batches/" + batchId).header("Authorization", AUTH))
                .andExpect(jsonPath("$.data.status").value("QC_PASSED"));

        // 采收单/批次编号唯一性：再建一个批次并采收，编号必须不同
        long tray2Id = postJson("/api/pf/trays",
                "{\"trayCode\":\"T-002\",\"trayName\":\"72孔穴盘2\",\"spec\":\"72孔\",\"capacity\":72}");
        long batch2Id = postJson("/api/pf/batches",
                "{\"batchName\":\"罗勒第二批\",\"cultivarId\":" + cultivarId + ",\"trayId\":" + tray2Id + "}");
        mockMvc.perform(post("/api/pf/batches/" + batch2Id + "/sow")
                        .header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":" + zoneId + "}")).andExpect(status().isOk());
        long harvest2Id = postJson("/api/pf/batches/" + batch2Id + "/harvests",
                "{\"grade\":\"B\",\"grossWeightKg\":8.000,\"destination\":\"加工间\"}");
        MvcResult r1 = mockMvc.perform(get("/api/pf/batches/" + batchId + "/harvests")
                .header("Authorization", AUTH)).andReturn();
        MvcResult r2 = mockMvc.perform(get("/api/pf/batches/" + batch2Id + "/harvests")
                .header("Authorization", AUTH)).andReturn();
        String no1 = objectMapper.readTree(r1.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").get(0).path("harvestNo").asText();
        String no2 = objectMapper.readTree(r2.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").get(0).path("harvestNo").asText();
        assertNotNull(no1);
        assertTrue(no1.startsWith("PF-H-"));
        assertTrue(no2.startsWith("PF-H-"));
        assertEquals(true, !no1.equals(no2), "采收单号必须唯一");
        assertTrue(harvest2Id > 0);

        // 起步批次（定时任务创建）与手工批次的批次号不得冲突
        MvcResult rb = mockMvc.perform(get("/api/pf/batches").header("Authorization", AUTH)).andReturn();
        JsonNode batches = objectMapper
                .readTree(rb.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        boolean startBatchFound = false;
        for (JsonNode b : batches) {
            if ("植物工厂种植批次台账起步".equals(b.path("batchName").asText())) {
                startBatchFound = true;
                assertEquals("PF-B-START-0001", b.path("batchNo").asText());
                assertEquals(1, b.path("createBy").asInt(), "起步批次操作者必须是 SYSTEM，不能丢失");
            }
        }
        assertTrue(startBatchFound, "启动后定时任务应已创建起步批次");
    }

    private long postJson(String path, String jsonBody) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        BigDecimal id = data.path("id").decimalValue();
        return id.longValue();
    }
}
