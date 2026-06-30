package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class DishCreateDTO {
    @NotNull(message = "请选择商家")
    private Long shopId;

    @NotBlank(message = "菜品名不能为空")
    @Size(max = 100, message = "菜品名最多100字")
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负")
    private Integer price;

    @Size(max = 255)
    private String image;

    @Size(max = 50)
    private String category;
}
