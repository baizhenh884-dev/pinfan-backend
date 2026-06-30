package com.pinfan.controller;

import cn.hutool.core.bean.BeanUtil;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import com.pinfan.dto.DishCreateDTO;
import com.pinfan.dto.DishUpdateDTO;
import com.pinfan.entity.Dish;
import com.pinfan.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/dish")
@Tag(name = "05-菜品")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @PostMapping
    @Operation(summary = "新增菜品")
    public R<Void> add(@RequestBody @Valid DishCreateDTO dishCreateDTO) {
        Dish dish = new Dish();
        BeanUtil.copyProperties(dishCreateDTO, dish);
        dishService.addDish(dish);
        return R.ok("菜品已增加", null);
    }

    @PutMapping
    @Operation(summary = "修改菜品（部分更新）")
    public R<Void> update(@RequestBody @Valid DishUpdateDTO dto) {
        Dish dish = new Dish();
        BeanUtil.copyProperties(dto, dish);
        dishService.updateDish(dish);
        return R.ok("已更新", null);
    }

    @GetMapping("/list")
    @Operation(summary = "按商家查菜品列表")
    public R<List<Dish>> list(@RequestParam Long shopId) {
        List<Dish> list = dishService.queryListByShop(shopId);
        return R.ok(list);
    }

    @PutMapping("/{id}/status/{status}")
    @Operation(summary = "启用 / 停售（status: 1在售 0停售）")
    public R<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "status 只能是 0 或 1");
        }
        dishService.changeStatus(id, status);
        return R.ok(status == 1 ? "已上架" : "已停售", null);
    }
}
