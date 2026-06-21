package com.pinfan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    private String icon;
    private String address;
    private BigDecimal score;           // 用 BigDecimal，不是 Double
    private Integer price;
    private String phone;
    private String openHours;
    private Integer status;
    private BigDecimal x;               // 经度，BigDecimal
    private BigDecimal y;               // 纬度，BigDecimal
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
