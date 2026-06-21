package com.pinfan.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CategoryDTO {
    @NotBlank(message = "分类名不能为空")
    @Size(max = 15, message = "分类名最多15字")
    private String name;

    @Size(max = 255, message = "图标URL最多255字")
    private String icon;

    //数据库里默认0
    private Integer sort;
}
