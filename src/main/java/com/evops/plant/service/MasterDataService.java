package com.evops.plant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.plant.domain.PfCultivar;
import com.evops.plant.domain.PfSeedTray;
import com.evops.plant.domain.PfZone;
import com.evops.plant.dto.CultivarRequest;
import com.evops.plant.dto.TrayRequest;
import com.evops.plant.dto.ZoneRequest;
import com.evops.plant.mapper.PfCultivarMapper;
import com.evops.plant.mapper.PfSeedTrayMapper;
import com.evops.plant.mapper.PfZoneMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 建档：温室分区（光照/营养液循环/设备分区）、品种、播种盘。
 */
@Service
public class MasterDataService {

    private final PfZoneMapper zoneMapper;
    private final PfCultivarMapper cultivarMapper;
    private final PfSeedTrayMapper trayMapper;

    public MasterDataService(PfZoneMapper zoneMapper,
                             PfCultivarMapper cultivarMapper,
                             PfSeedTrayMapper trayMapper) {
        this.zoneMapper = zoneMapper;
        this.cultivarMapper = cultivarMapper;
        this.trayMapper = trayMapper;
    }

    // ---- 分区 ----

    @Transactional
    public PfZone createZone(ZoneRequest req) {
        if (zoneMapper.selectCount(new QueryWrapper<PfZone>().eq("zone_code", req.getZoneCode())) > 0) {
            throw new IllegalArgumentException("分区编码已存在：" + req.getZoneCode());
        }
        PfZone zone = new PfZone();
        zone.setZoneCode(req.getZoneCode());
        zone.setZoneName(req.getZoneName());
        zone.setZoneRole(req.getZoneRole() == null ? "GROW" : req.getZoneRole());
        zone.setLightingMode(req.getLightingMode());
        zone.setDailyLightMinutes(req.getDailyLightMinutes());
        zone.setPpfd(req.getPpfd());
        zone.setNutrientMode(req.getNutrientMode());
        zone.setCycleIntervalMin(req.getCycleIntervalMin());
        zone.setCycleDurationMin(req.getCycleDurationMin());
        zone.setEcTarget(req.getEcTarget());
        zone.setPhTarget(req.getPhTarget());
        zone.setStatus("ACTIVE");
        zoneMapper.insert(zone);
        return zone;
    }

    public PfZone findZoneByCode(String zoneCode) {
        return zoneMapper.selectOne(new QueryWrapper<PfZone>().eq("zone_code", zoneCode));
    }

    public List<PfZone> listZones() {
        return zoneMapper.selectList(new QueryWrapper<PfZone>().orderByAsc("zone_code"));
    }

    // ---- 品种 ----

    @Transactional
    public PfCultivar createCultivar(CultivarRequest req) {
        if (cultivarMapper.selectCount(
                new QueryWrapper<PfCultivar>().eq("cultivar_code", req.getCultivarCode())) > 0) {
            throw new IllegalArgumentException("品种编码已存在：" + req.getCultivarCode());
        }
        PfCultivar cultivar = new PfCultivar();
        cultivar.setCultivarCode(req.getCultivarCode());
        cultivar.setCultivarName(req.getCultivarName());
        cultivar.setCategory(req.getCategory());
        cultivar.setDefaultGrowDays(req.getDefaultGrowDays());
        cultivar.setRemark(req.getRemark());
        cultivarMapper.insert(cultivar);
        return cultivar;
    }

    public PfCultivar findCultivarByCode(String cultivarCode) {
        return cultivarMapper.selectOne(new QueryWrapper<PfCultivar>().eq("cultivar_code", cultivarCode));
    }

    public List<PfCultivar> listCultivars() {
        return cultivarMapper.selectList(new QueryWrapper<PfCultivar>().orderByAsc("cultivar_code"));
    }

    // ---- 播种盘 ----

    @Transactional
    public PfSeedTray createTray(TrayRequest req) {
        if (trayMapper.selectCount(new QueryWrapper<PfSeedTray>().eq("tray_code", req.getTrayCode())) > 0) {
            throw new IllegalArgumentException("播种盘编码已存在：" + req.getTrayCode());
        }
        PfSeedTray tray = new PfSeedTray();
        tray.setTrayCode(req.getTrayCode());
        tray.setTrayName(req.getTrayName());
        tray.setSpec(req.getSpec());
        tray.setCapacity(req.getCapacity());
        tray.setStatus("IDLE");
        trayMapper.insert(tray);
        return tray;
    }

    public PfSeedTray findTrayByCode(String trayCode) {
        return trayMapper.selectOne(new QueryWrapper<PfSeedTray>().eq("tray_code", trayCode));
    }

    public List<PfSeedTray> listTrays() {
        return trayMapper.selectList(new QueryWrapper<PfSeedTray>().orderByAsc("tray_code"));
    }
}
