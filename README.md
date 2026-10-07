# EVOPS 基础工作区

这是 springboot 的骨架项目。

技术栈：Java 8、Spring Boot 2.7、MyBatis-Plus、H2 本地文件数据库、Shiro、Thymeleaf、Quartz。

启动前准备：

1. 执行 `mvn -q -DskipTests compile` 验证骨架构建。
2. 执行 `mvn spring-boot:run` 启动应用；Spring Boot 会自动执行 `src/main/resources/schema.sql`（全部语句幂等，可重复启动）。
3. H2 数据文件默认写入工作区 `data/evops`。如需修改路径，编辑 `application.yml` 中的 `jdbc:h2:file:` 连接串。

根目录的 `..\..\docs\schema\evops.sql` 是交付副本，应与工作区 `src/main/resources/schema.sql` 保持一致。

## 植物工厂种植台账（com.evops.plant）

温室分区记录光照、营养液循环与设备分区；种植批次关联品种、播种盘与计划采收日；
采收单记录等级、重量与去向；转棚全程留痕。批次号（`PF-B-####`）与采收单号
（`PF-H-####`）由 H2 序列生成并受唯一约束保护，重启不重置。销售结算后续再接。

业务主线与批次状态：`DRAFT 建档 → SOWN 播种/养护 → HARVESTED 采收 → QC_PASSED/QC_FAILED 质检`。

接口（除 `/api/health` 外需登录，账户 `bootstrap/bootstrap`，支持 HTTP Basic）：

| 阶段 | 接口 |
| --- | --- |
| 建档 | `POST/GET /api/pf/zones`、`/api/pf/cultivars`、`/api/pf/trays` |
| 批次建档 | `POST /api/pf/batches`、`GET /api/pf/batches`、`GET /api/pf/batches/{id}` |
| 播种 | `POST /api/pf/batches/{id}/sow` |
| 养护（光照/营养液） | `POST/GET /api/pf/batches/{id}/care-logs` |
| 转棚留痕 | `POST/GET /api/pf/batches/{id}/transfers` |
| 采收（等级/重量/去向） | `POST/GET /api/pf/batches/{id}/harvests` |
| 质检 | `POST /api/pf/batches/{id}/qc` |

### 起步台账定时任务

`StartupLedgerJob` 在应用启动完成后检查，并每 5 分钟巡检一次名为
**“植物工厂种植批次台账起步”**（固定批次号 `PF-B-START-0001`）的批次：

- 已存在：本次不写入任何记录（不多写）；
- 不存在：按编码判存后补齐起步分区/品种/播种盘，再创建起步批次；
- 任务以 SYSTEM 操作员（id=1）身份执行，配合 `AuditMetaObjectHandler`，
  `create_by/update_by` 绝不允许为空（不丢操作者）。

### 测试

- `LedgerFlowIntegrationTest`：建档→播种→养护→转棚→采收→质检全链路与编号唯一性。
- `StartupIdempotencyTest`：文件库两次真实重启，起步记录各仅 1 条、操作者为 SYSTEM。
