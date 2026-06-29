package com.pinfan.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.entity.Shop;
import com.pinfan.mapper.ShopMapper;
import com.pinfan.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
                             implements ShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Shop queryById(Long id) {
        String key = "cache:shop:" + id;

        // 查 Redis
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 情况1：缓存命中真实数据 → 直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            log.info("缓存命中: {}", key);
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 情况2：缓存命中"空值"（shopJson == "" 但不是 null）→ 直接 404，不打 DB
        if (shopJson != null) {
            log.info("缓存命中空值（穿透防御生效）: {}", key);
            throw new BusinessException(404, "商家不存在");
        }

        // 情况3：shopJson == null，缓存真未命中，查 DB
        log.info("缓存未命中，查 DB: {}", key);
        Shop shop = getById(id);
        if (shop == null) {
            // ⚠️ 缓存穿透防御：DB 也查不到，写空值缓存，TTL 短（2 分钟）
            stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
            throw new BusinessException(404, "商家不存在");
        }

        // 写正常缓存，TTL 30 分钟
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);

        return shop;
    }

    @Override
    public void updateShop(Shop shop) {
        if (shop.getId() == null) {
            throw new BusinessException(400, "id不能为空");
        }
        // 先更新数据库
        updateById(shop);
        // 再删除缓存
        String key = "cache:shop:" + shop.getId();
        stringRedisTemplate.delete(key);
        log.info("更新商家并删除缓存: {}", key);
    }
}
