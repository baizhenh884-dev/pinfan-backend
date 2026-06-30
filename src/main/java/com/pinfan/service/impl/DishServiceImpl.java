package com.pinfan.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.entity.Dish;
import com.pinfan.mapper.DishMapper;
import com.pinfan.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static long randomTtl(long baseSeconds, long maxJitterSeconds) {
        return baseSeconds + ThreadLocalRandom.current().nextLong(maxJitterSeconds);
    }

    // 公共方法：根据 shopId 删除该店菜品列表缓存
    private void deleteShopListCache(Long shopId) {
        String key = "cache:dish:list:" + shopId;
        stringRedisTemplate.delete(key);
        log.info("删除菜品列表缓存: {}", key);
    }

    @Override
    public List<Dish> queryListByShop(Long shopId) {
        String key = "cache:dish:list:" + shopId;

        // 查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            log.info("菜品列表缓存命中: {}", key);
            // 反序列化数组用 toList，不是 toBean
            return JSONUtil.toList(json, Dish.class);
        }

        // 查 DB
        log.info("菜品列表缓存未命中，查 DB: {}", key);
        List<Dish> list = lambdaQuery()
                .eq(Dish::getShopId, shopId)
                .orderByDesc(Dish::getStatus)
                .orderByAsc(Dish::getCategory)
                .list();

        // 即使是空 list 也写，防穿透
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(list),
                randomTtl(1800, 300), TimeUnit.SECONDS);

        return list;
    }

    @Override
    public void addDish(Dish dish) {
        save(dish);
        deleteShopListCache(dish.getShopId());
    }

    @Override
    public void updateDish(Dish dish) {
        if (dish.getId() == null) {
            throw new BusinessException(400, "id不能为空");
        }
        // 反查 shopId
        Dish existing = getById(dish.getId());
        if (existing == null) {
            throw new BusinessException(404, "菜品不存在");
        }

        updateById(dish);

        deleteShopListCache(existing.getShopId());
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        Dish existing = getById(id);
        if (existing == null) {
            throw new BusinessException(404, "菜品不存在");
        }

        Dish update = new Dish();
        update.setId(id);
        update.setStatus(status);
        updateById(update);

        deleteShopListCache(existing.getShopId());
    }
}

