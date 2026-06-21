package com.pinfan.controller;

import cn.hutool.core.bean.BeanUtil;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import com.pinfan.dto.CategoryDTO;
import com.pinfan.entity.ShopCategory;
import com.pinfan.service.ShopCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shop/category")
@Tag(name = "02-商家分类", description = "分类增删查")
public class ShopCategoryController {
    @Autowired
    private ShopCategoryService shopCategoryService;

    @PostMapping
    @Operation(summary = "新增分类")
    public R<Void> addCategory(@RequestBody @Valid CategoryDTO categoryDTO) {
        // 先查同名分类是否存在
        boolean exists = shopCategoryService.lambdaQuery()
                .eq(ShopCategory::getName, categoryDTO.getName())
                .exists();
        if (exists) {
            throw new BusinessException(400, "分类名已存在");
        }

        ShopCategory category = new ShopCategory();
        BeanUtil.copyProperties(categoryDTO, category);
        shopCategoryService.save(category);
        return R.ok("分类已新增", null);
    }

    @GetMapping
    @Operation(summary = "查询所有分类（按sort升序）")
    public R<List<ShopCategory>> list() {
        List<ShopCategory> list = shopCategoryService.lambdaQuery()
                .orderByAsc(ShopCategory::getSort)
                .list();
        return R.ok(list);
    }
}
