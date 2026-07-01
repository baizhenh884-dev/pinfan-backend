package com.pinfan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_summary")
public class ReviewSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String pros;
    private String cons;
    private String suitableFor;
    private Integer reviewCount;
    private LocalDateTime generatedAt;
    private String modelName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
