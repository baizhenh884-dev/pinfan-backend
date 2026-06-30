package com.pinfan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NearbyShopVO {
    private Long id;
    private String name;
    private String address;
    private String icon;
    private BigDecimal score;
    private Integer price;
    private Double distance;   // km
}
