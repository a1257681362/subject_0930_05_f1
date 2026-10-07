package com.evops.plant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.plant.domain.*;
import com.evops.plant.dto.BatchCreateRequest;
import com.evops.plant.dto.CareLogRequest;
import com.evops.plant.dto.HarvestRequest;
import com.evops.plant.dto.QcRequest;
import com.evops.plant.dto.SowRequest;
import com.evops.plant.dto.TransferRequest;
import com.evops.plant.enums.BatchStatus;
import com.evops.plant.enums.CareLogType;
import com.evops.plant.enums.QcResult;
import com.evops.plant.mapper.*;
import com.evops.plant.support.DocumentNoGenerator;
import com.evops.plant.support.OperatorContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 种植台账主流程：建档 → 播种 → 养护 →（转棚留痕）→ 采收 → 质检。
 * 批次号、采收单号由序列生成并受唯一约束保护；每一步都记录操作者。
 */
@Service
public class LedgerService {

    /** 起步台账固定批次号（定时任务幂等锚点） */
    public static final String START_BATCH_NO = "PF-B-START-0001";
    public static final String START_BATCH_NAME = "植物工厂种植批次台账起步";

    private final PfBatchMapper batchMapper;
    private final PfCultivarMapper cultivarMapper;
    private final PfSeedTrayMapper trayMapper;
    private final PfZoneMapper zoneMapper;
    private final PfZoneTransferMapper transferMapper;
    private final PfCareLogMapper careLogMapper;
    private final PfHarvestMapper harvestMapper;
    private final PfQcRecordMapper qcMapper;
    private final DocumentNoGenerator documentNoGenerator;

    public LedgerService(PfBatchMapper batchMapper,
                         PfCultivarMapper cultivarMapper,
                         PfSeedTrayMapper trayMapper,
                         PfZoneMapper zoneMapper,
                         PfZoneTransferMapper transferMapper,
                         PfCareLogMapper careLogMapper,
                         PfHarvestMapper harvestMapper,
                         PfQcRecordMapper qcMapper,
                         DocumentNoGenerator documentNoGenerator) {
        this.batchMapper = batchMapper;
        this.cultivarMapper = cultivarMapper;
        this.trayMapper = trayMapper;
        this.zoneMapper = zoneMapper;
        this.transferMapper = transferMapper;
        this.careLogMapper = careLogMapper;
        this.harvestMapper = harvestMapper;
        this.qcMapper = qcMapper;
        this.documentNoGenerator = documentNoGenerator;
    }

    // ---------------- 建档 ----------------

    @Transactional
    public PfBatch createBatch(BatchCreateRequest req) {
        return createBatch(req, documentNoGenerator.nextBatchNo());
    }

    /** 指定批次号建档（起步台账使用固定号；编号唯一，重复直接拒绝） */
    @Transactional
    public PfBatch createBatch(BatchCreateRequest req, String batchNo) {
        if (batchMapper.selectCount(new QueryWrapper<PfBatch>().eq("batch_no", batchNo)) > 0) {
            throw new IllegalArgumentException("批次号已存在：" + batchNo);
        }
        PfCultivar cultivar = cultivarMapper.selectById(req.getCultivarId());
        if (cultivar == null) {
            throw new IllegalArgumentException("品种不存在：" + req.getCultivarId());
        }
        PfSeedTray tray = trayMapper.selectById(req.getTrayId());
        if (tray == null) {
            throw new IllegalArgumentException("播种盘不存在：" + req.getTrayId());
        }
        PfBatch batch = new PfBatch();
        batch.setBatchNo(batchNo);
        batch.setBatchName(req.getBatchName());
        batch.setCultivarId(req.getCultivarId());
        batch.setTrayId(req.getTrayId());
        batch.setPlannedHarvestDate(req.getPlannedHarvestDate());
        batch.setSeedQty(req.getSeedQty());
        batch.setStatus(BatchStatus.DRAFT.name());
        batch.setRemark(req.getRemark());
        batchMapper.insert(batch);
        return batch;
    }

    public PfBatch getBatch(Long id) {
        PfBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在：" + id);
        }
        return batch;
    }

    public PfBatch findBatchByName(String batchName) {
        return batchMapper.selectOne(new QueryWrapper<PfBatch>().eq("batch_name", batchName));
    }

    public PfBatch findBatchByNo(String batchNo) {
        return batchMapper.selectOne(new QueryWrapper<PfBatch>().eq("batch_no", batchNo));
    }

    public List<PfBatch> listBatches() {
        return batchMapper.selectList(new QueryWrapper<PfBatch>().orderByDesc("id"));
    }

    // ---------------- 播种 ----------------

    @Transactional
    public PfBatch sow(Long batchId, SowRequest req) {
        PfBatch batch = getBatch(batchId);
        if (!BatchStatus.DRAFT.name().equals(batch.getStatus())) {
            throw new IllegalArgumentException("只有建档状态的批次才能播种，当前状态：" + batch.getStatus());
        }
        PfZone zone = zoneMapper.selectById(req.getZoneId());
        if (zone == null || !"ACTIVE".equals(zone.getStatus())) {
            throw new IllegalArgumentException("播种分区不存在或不可用：" + req.getZoneId());
        }
        PfSeedTray tray = trayMapper.selectById(batch.getTrayId());
        if (!"IDLE".equals(tray.getStatus())) {
            throw new IllegalArgumentException("播种盘已在使用中：" + tray.getTrayCode());
        }

        long operatorId = OperatorContext.currentOperatorId();
        LocalDateTime now = LocalDateTime.now();
        batch.setZoneId(zone.getId());
        batch.setStatus(BatchStatus.SOWN.name());
        batch.setSownTime(now);
        batch.setSownBy(operatorId);
        if (req.getSeedQty() != null) {
            batch.setSeedQty(req.getSeedQty());
        }
        batchMapper.updateById(batch);

        tray.setStatus("IN_USE");
        trayMapper.updateById(tray);
        return batch;
    }

    // ---------------- 养护 ----------------

    @Transactional
    public PfCareLog addCareLog(Long batchId, CareLogRequest req) {
        PfBatch batch = getBatch(batchId);
        if (!BatchStatus.SOWN.name().equals(batch.getStatus())) {
            throw new IllegalArgumentException("只有养护中（已播种未采收）的批次才能登记养护记录");
        }
        CareLogType type;
        try {
            type = CareLogType.valueOf(req.getLogType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("养护记录类型必须是 LIGHT 或 NUTRIENT");
        }
        PfCareLog log = new PfCareLog();
        log.setBatchId(batchId);
        log.setLogType(type.name());
        log.setLightMinutes(req.getLightMinutes());
        log.setPpfd(req.getPpfd());
        log.setEcValue(req.getEcValue());
        log.setPhValue(req.getPhValue());
        log.setVolumeMl(req.getVolumeMl());
        log.setMeasuredTime(req.getMeasuredTime() == null ? LocalDateTime.now() : req.getMeasuredTime());
        log.setNote(req.getNote());
        log.setOperatorId(OperatorContext.currentOperatorId());
        careLogMapper.insert(log);
        return log;
    }

    public List<PfCareLog> listCareLogs(Long batchId) {
        return careLogMapper.selectList(
                new QueryWrapper<PfCareLog>().eq("batch_id", batchId).orderByAsc("measured_time"));
    }

    // ---------------- 转棚（留痕） ----------------

    @Transactional
    public PfZoneTransfer transfer(Long batchId, TransferRequest req) {
        PfBatch batch = getBatch(batchId);
        if (!BatchStatus.SOWN.name().equals(batch.getStatus())) {
            throw new IllegalArgumentException("只有养护中的批次才能转棚，当前状态：" + batch.getStatus());
        }
        PfZone toZone = zoneMapper.selectById(req.getToZoneId());
        if (toZone == null || !"ACTIVE".equals(toZone.getStatus())) {
            throw new IllegalArgumentException("目标分区不存在或不可用：" + req.getToZoneId());
        }
        Long fromZoneId = batch.getZoneId();
        if (fromZoneId != null && fromZoneId.equals(req.getToZoneId())) {
            throw new IllegalArgumentException("批次已在目标分区，无需转棚");
        }

        PfZoneTransfer transfer = new PfZoneTransfer();
        transfer.setBatchId(batchId);
        transfer.setFromZoneId(fromZoneId);
        transfer.setToZoneId(req.getToZoneId());
        transfer.setReason(req.getReason());
        transfer.setTransferTime(LocalDateTime.now());
        transfer.setOperatorId(OperatorContext.currentOperatorId());
        transferMapper.insert(transfer);

        batch.setZoneId(req.getToZoneId());
        batchMapper.updateById(batch);
        return transfer;
    }

    public List<PfZoneTransfer> listTransfers(Long batchId) {
        return transferMapper.selectList(
                new QueryWrapper<PfZoneTransfer>().eq("batch_id", batchId).orderByAsc("transfer_time"));
    }

    // ---------------- 采收 ----------------

    @Transactional
    public PfHarvest harvest(Long batchId, HarvestRequest req) {
        PfBatch batch = getBatch(batchId);
        if (!BatchStatus.SOWN.name().equals(batch.getStatus())) {
            throw new IllegalArgumentException("只有养护中的批次才能采收，当前状态：" + batch.getStatus());
        }
        String grade = req.getGrade().trim().toUpperCase();
        if (!Arrays.asList("A", "B", "C").contains(grade)) {
            throw new IllegalArgumentException("采收等级必须是 A/B/C");
        }

        PfHarvest harvest = new PfHarvest();
        harvest.setHarvestNo(documentNoGenerator.nextHarvestNo());
        harvest.setBatchId(batchId);
        harvest.setGrade(grade);
        harvest.setGrossWeightKg(req.getGrossWeightKg());
        harvest.setNetWeightKg(req.getNetWeightKg());
        harvest.setDestination(req.getDestination().trim());
        harvest.setHarvestTime(req.getHarvestTime() == null ? LocalDateTime.now() : req.getHarvestTime());
        harvest.setOperatorId(OperatorContext.currentOperatorId());
        harvest.setRemark(req.getRemark());
        harvestMapper.insert(harvest);

        batch.setStatus(BatchStatus.HARVESTED.name());
        batchMapper.updateById(batch);
        return harvest;
    }

    public List<PfHarvest> listHarvests(Long batchId) {
        return harvestMapper.selectList(
                new QueryWrapper<PfHarvest>().eq("batch_id", batchId).orderByDesc("harvest_time"));
    }

    // ---------------- 质检 ----------------

    @Transactional
    public PfQcRecord qualityCheck(Long batchId, QcRequest req) {
        PfBatch batch = getBatch(batchId);
        if (!BatchStatus.HARVESTED.name().equals(batch.getStatus())) {
            throw new IllegalArgumentException("只有已采收的批次才能质检，当前状态：" + batch.getStatus());
        }
        PfHarvest harvest = harvestMapper.selectOne(
                new QueryWrapper<PfHarvest>().eq("batch_id", batchId).orderByDesc("harvest_time").last("limit 1"));
        if (harvest == null) {
            throw new IllegalArgumentException("批次缺少采收单，无法质检");
        }
        QcResult result;
        try {
            result = QcResult.valueOf(req.getResult().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("质检结论必须是 PASSED 或 FAILED");
        }

        PfQcRecord qc = new PfQcRecord();
        qc.setBatchId(batchId);
        qc.setHarvestId(harvest.getId());
        qc.setResult(result.name());
        qc.setIssues(req.getIssues());
        qc.setQcTime(LocalDateTime.now());
        qc.setInspectorId(OperatorContext.currentOperatorId());
        qcMapper.insert(qc);

        batch.setStatus(result == QcResult.PASSED
                ? BatchStatus.QC_PASSED.name()
                : BatchStatus.QC_FAILED.name());
        batchMapper.updateById(batch);
        return qc;
    }
}
