package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {
    // 按商家查菜品列表（先查 Redis，没有再查 DB）
    List<Dish> queryListByShop(Long shopId);

    // 写
    void addDish(Dish dish);
    void updateDish(Dish dish);
    void changeStatus(Long id, Integer status);
}
