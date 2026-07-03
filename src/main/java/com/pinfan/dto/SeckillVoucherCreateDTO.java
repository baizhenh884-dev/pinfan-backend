package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class SeckillVoucherCreateDTO {
    @NotNull(message = "商家ID不能为空")
    private Long shopId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100)
    private String title;

    @Size(max = 255)
    private String subTitle;

    private String rules;

    @NotNull
    @Min(0)
    private Integer payValue;      // 支付金额（分）

    @NotNull
    @Min(1)
    private Integer actualValue;   // 面值（分）

    @NotNull
    @Min(1)
    private Integer stock;    //库存

    @NotNull
    private LocalDateTime beginTime;

    @NotNull
    private LocalDateTime endTime;
}
