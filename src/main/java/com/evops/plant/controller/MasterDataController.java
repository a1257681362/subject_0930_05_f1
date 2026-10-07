package com.evops.plant.controller;

import com.evops.common.ApiResponse;
import com.evops.plant.domain.PfCultivar;
import com.evops.plant.domain.PfSeedTray;
import com.evops.plant.domain.PfZone;
import com.evops.plant.dto.CultivarRequest;
import com.evops.plant.dto.TrayRequest;
import com.evops.plant.dto.ZoneRequest;
import com.evops.plant.service.MasterDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 建档：温室分区、品种、播种盘 */
@RestController
@RequestMapping("/api/pf")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @PostMapping("/zones")
    public ApiResponse<PfZone> createZone(@Valid @RequestBody ZoneRequest req) {
        return ApiResponse.ok(masterDataService.createZone(req));
    }

    @GetMapping("/zones")
    public ApiResponse<List<PfZone>> listZones() {
        return ApiResponse.ok(masterDataService.listZones());
    }

    @PostMapping("/cultivars")
    public ApiResponse<PfCultivar> createCultivar(@Valid @RequestBody CultivarRequest req) {
        return ApiResponse.ok(masterDataService.createCultivar(req));
    }

    @GetMapping("/cultivars")
    public ApiResponse<List<PfCultivar>> listCultivars() {
        return ApiResponse.ok(masterDataService.listCultivars());
    }

    @PostMapping("/trays")
    public ApiResponse<PfSeedTray> createTray(@Valid @RequestBody TrayRequest req) {
        return ApiResponse.ok(masterDataService.createTray(req));
    }

    @GetMapping("/trays")
    public ApiResponse<List<PfSeedTray>> listTrays() {
        return ApiResponse.ok(masterDataService.listTrays());
    }
}
