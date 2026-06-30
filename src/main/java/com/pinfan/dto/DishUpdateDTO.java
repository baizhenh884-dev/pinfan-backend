package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class DishUpdateDTO {
    @NotNull(message = "id不能为空")
    private Long id;

    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @Min(value = 0)
    private Integer price;

    @Size(max = 255)
    private String image;

    @Size(max = 50)
    private String category;

    private Integer status;
}
