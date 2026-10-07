package com.evops.plant.controller;

import com.evops.common.ApiResponse;
import com.evops.plant.domain.PfBatch;
import com.evops.plant.domain.PfCareLog;
import com.evops.plant.domain.PfHarvest;
import com.evops.plant.domain.PfQcRecord;
import com.evops.plant.domain.PfZoneTransfer;
import com.evops.plant.dto.BatchCreateRequest;
import com.evops.plant.dto.CareLogRequest;
import com.evops.plant.dto.HarvestRequest;
import com.evops.plant.dto.QcRequest;
import com.evops.plant.dto.SowRequest;
import com.evops.plant.dto.TransferRequest;
import com.evops.plant.service.LedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 种植台账：建档 → 播种 → 养护 → 转棚 → 采收 → 质检。
 * 销售结算后续再接，本阶段不提供相关接口。
 */
@RestController
@RequestMapping("/api/pf/batches")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /** 建档 */
    @PostMapping
    public ApiResponse<PfBatch> createBatch(@Valid @RequestBody BatchCreateRequest req) {
        return ApiResponse.ok(ledgerService.createBatch(req));
    }

    @GetMapping
    public ApiResponse<List<PfBatch>> listBatches() {
        return ApiResponse.ok(ledgerService.listBatches());
    }

    @GetMapping("/{id}")
    public ApiResponse<PfBatch> getBatch(@PathVariable Long id) {
        return ApiResponse.ok(ledgerService.getBatch(id));
    }

    /** 播种 */
    @PostMapping("/{id}/sow")
    public ApiResponse<PfBatch> sow(@PathVariable Long id, @Valid @RequestBody SowRequest req) {
        return ApiResponse.ok(ledgerService.sow(id, req));
    }

    /** 养护：光照 / 营养液循环记录 */
    @PostMapping("/{id}/care-logs")
    public ApiResponse<PfCareLog> addCareLog(@PathVariable Long id, @Valid @RequestBody CareLogRequest req) {
        return ApiResponse.ok(ledgerService.addCareLog(id, req));
    }

    @GetMapping("/{id}/care-logs")
    public ApiResponse<List<PfCareLog>> listCareLogs(@PathVariable Long id) {
        return ApiResponse.ok(ledgerService.listCareLogs(id));
    }

    /** 转棚（留痕） */
    @PostMapping("/{id}/transfers")
    public ApiResponse<PfZoneTransfer> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest req) {
        return ApiResponse.ok(ledgerService.transfer(id, req));
    }

    @GetMapping("/{id}/transfers")
    public ApiResponse<List<PfZoneTransfer>> listTransfers(@PathVariable Long id) {
        return ApiResponse.ok(ledgerService.listTransfers(id));
    }

    /** 采收：等级 / 重量 / 去向 */
    @PostMapping("/{id}/harvests")
    public ApiResponse<PfHarvest> harvest(@PathVariable Long id, @Valid @RequestBody HarvestRequest req) {
        return ApiResponse.ok(ledgerService.harvest(id, req));
    }

    @GetMapping("/{id}/harvests")
    public ApiResponse<List<PfHarvest>> listHarvests(@PathVariable Long id) {
        return ApiResponse.ok(ledgerService.listHarvests(id));
    }

    /** 质检 */
    @PostMapping("/{id}/qc")
    public ApiResponse<PfQcRecord> qualityCheck(@PathVariable Long id, @Valid @RequestBody QcRequest req) {
        return ApiResponse.ok(ledgerService.qualityCheck(id, req));
    }
}
