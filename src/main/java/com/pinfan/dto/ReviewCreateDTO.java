package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ReviewCreateDTO {
    @NotNull(message = "商家ID不能为空")
    private Long shopId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分至少1星")
    @Max(value = 5, message = "评分最多5星")
    private Integer rating;

    @Size(max = 500, message = "评论最多500字")
    private String content;
}
