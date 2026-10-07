package com.evops.plant.job;

import com.evops.plant.domain.PfBatch;
import com.evops.plant.domain.PfCultivar;
import com.evops.plant.domain.PfSeedTray;
import com.evops.plant.domain.PfZone;
import com.evops.plant.dto.BatchCreateRequest;
import com.evops.plant.dto.CultivarRequest;
import com.evops.plant.dto.TrayRequest;
import com.evops.plant.dto.ZoneRequest;
import com.evops.plant.service.LedgerService;
import com.evops.plant.service.MasterDataService;
import com.evops.plant.support.OperatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 植物工厂种植批次台账起步 —— 定时检查任务。
 *
 * 幂等铁律：
 * 1. 名为“植物工厂种植批次台账起步”的批次已存在 → 本次不写入任何记录；
 * 2. 不存在 → 补齐分区/品种/播种盘（各自按编码判存）并创建起步批次（固定批次号）；
 * 3. 全程以 SYSTEM 操作员身份执行，create_by/update_by 不得为空、不得丢失操作者。
 */
@Component
public class StartupLedgerJob {

    private static final Logger log = LoggerFactory.getLogger(StartupLedgerJob.class);

    static final String ZONE_CODE = "Z-START";
    static final String CULTIVAR_CODE = "C-LETTUCE-01";
    static final String TRAY_CODE = "T-START-01";

    private final MasterDataService masterDataService;
    private final LedgerService ledgerService;

    /** 进程内串行保护，避免启动触发与定时触发并发重复建档 */
    private final Object lock = new Object();

    public StartupLedgerJob(MasterDataService masterDataService, LedgerService ledgerService) {
        this.masterDataService = masterDataService;
        this.ledgerService = ledgerService;
    }

    /** 启动完成后先检查一次（重启定时任务的“重启后检查”） */
    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        checkStartupBatch();
    }

    /** 之后每 5 分钟巡检一次，已存在则零写入 */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void checkOnSchedule() {
        checkStartupBatch();
    }

    public void checkStartupBatch() {
        synchronized (lock) {
            OperatorContext.setOperatorId(OperatorContext.SYSTEM_OPERATOR_ID);
            try {
                doCheck();
            } catch (Exception e) {
                log.error("起步台账定时检查失败", e);
            } finally {
                OperatorContext.clear();
            }
        }
    }

    /**
     * 先查后写：批次存在即返回，不触发任何写库动作。
     * 不加事务：分区/品种/播种盘均按编码判存，即便中途失败，下次检查也能续建，不产生重复记录。
     */
    void doCheck() {
        PfBatch existing = ledgerService.findBatchByName(LedgerService.START_BATCH_NAME);
        if (existing != null) {
            log.info("起步批次已存在：batchNo={}, status={}, createBy={}，本次不写入任何记录",
                    existing.getBatchNo(), existing.getStatus(), existing.getCreateBy());
            return;
        }

        PfZone zone = ensureZone();
        PfCultivar cultivar = ensureCultivar();
        PfSeedTray tray = ensureTray();

        BatchCreateRequest req = new BatchCreateRequest();
        req.setBatchName(LedgerService.START_BATCH_NAME);
        req.setCultivarId(cultivar.getId());
        req.setTrayId(tray.getId());
        req.setSeedQty(72);
        req.setRemark("定时任务创建的植物工厂起步批次");
        if (cultivar.getDefaultGrowDays() != null) {
            req.setPlannedHarvestDate(LocalDate.now().plusDays(cultivar.getDefaultGrowDays()));
        }
        PfBatch batch = ledgerService.createBatch(req, LedgerService.START_BATCH_NO);
        log.info("起步批次建档完成：batchNo={}, id={}, zone={}, operator=SYSTEM",
                batch.getBatchNo(), batch.getId(), zone.getZoneCode());
    }

    private PfZone ensureZone() {
        PfZone zone = masterDataService.findZoneByCode(ZONE_CODE);
        if (zone != null) {
            return zone;
        }
        ZoneRequest req = new ZoneRequest();
        req.setZoneCode(ZONE_CODE);
        req.setZoneName("起步栽培区");
        req.setZoneRole("GROW");
        req.setLightingMode("LED-全光谱");
        req.setDailyLightMinutes(960);
        req.setPpfd(220);
        req.setNutrientMode("NFT");
        req.setCycleIntervalMin(30);
        req.setCycleDurationMin(5);
        return masterDataService.createZone(req);
    }

    private PfCultivar ensureCultivar() {
        PfCultivar cultivar = masterDataService.findCultivarByCode(CULTIVAR_CODE);
        if (cultivar != null) {
            return cultivar;
        }
        CultivarRequest req = new CultivarRequest();
        req.setCultivarCode(CULTIVAR_CODE);
        req.setCultivarName("奶油生菜");
        req.setCategory("叶菜");
        req.setDefaultGrowDays(30);
        return masterDataService.createCultivar(req);
    }

    private PfSeedTray ensureTray() {
        PfSeedTray tray = masterDataService.findTrayByCode(TRAY_CODE);
        if (tray != null) {
            return tray;
        }
        TrayRequest req = new TrayRequest();
        req.setTrayCode(TRAY_CODE);
        req.setTrayName("起步72孔穴盘");
        req.setSpec("72孔穴盘");
        req.setCapacity(72);
        return masterDataService.createTray(req);
    }
}
