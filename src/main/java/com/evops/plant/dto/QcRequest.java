package com.evops.plant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/** 质检：PASSED / FAILED */
@Data
public class QcRequest {
    @NotBlank(message = "质检结论不能为空（PASSED/FAILED）")
    private String result;
    private String issues;
}
