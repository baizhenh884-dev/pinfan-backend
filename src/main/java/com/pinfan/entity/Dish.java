package com.pinfan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish")
public class Dish {
    private Long id;                  // @TableId(type = IdType.AUTO)
    private Long shopId;
    private String name;
    private String description;
    private Integer price;
    private String image;
    private String category;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;          // @TableLogic
}
