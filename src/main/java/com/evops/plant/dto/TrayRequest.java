package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TrayRequest {
    @NotBlank(message = "播种盘编码不能为空")
    private String trayCode;
    @NotBlank(message = "播种盘名称不能为空")
    private String trayName;
    private String spec;
    private Integer capacity;
}
