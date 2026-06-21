package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class ShopUpdateDTO {
    @NotNull(message="id不能为空")
    private Long id;

    @Size(max = 100, message = "商家名最多100字")
    private String name;

    private Long categoryId;
    private String icon;
    private String address;
    private Integer price;
    private String phone;
    private String openHours;
    private Integer status;
    private BigDecimal x;
    private BigDecimal y;
}
