package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class ShopCreateDTO {
    @NotBlank(message = "商家名不能为空")
    @Size(max = 100, message = "商家名最多100字")
    private String name;

    @NotNull(message="请选择分类")
    private Long categoryId;

    @Size(max=255)
    private String icon;

    @Size(max=255)
    private String address;

    private Integer price;

    @Size(max=20)
    private String phone;

    @Size(max=50)
    private String openHours;

    private BigDecimal x;
    private BigDecimal y;
}
