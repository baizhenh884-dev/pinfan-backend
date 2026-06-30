package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class LocationDTO {
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围 -180 ~ 180")
    @DecimalMax(value = "180.0", message = "经度范围 -180 ~ 180")
    private BigDecimal x;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围 -90 ~ 90")
    @DecimalMax(value = "90.0", message = "纬度范围 -90 ~ 90")
    private BigDecimal y;
}
