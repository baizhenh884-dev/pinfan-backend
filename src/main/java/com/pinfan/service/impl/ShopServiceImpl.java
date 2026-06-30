package com.pinfan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.dto.NearbyShopVO;
import com.pinfan.entity.Shop;
import com.pinfan.mapper.ShopMapper;
import com.pinfan.service.ShopService;
import com.pinfan.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
                             implements ShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 异步更新缓存的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 雪崩防御：给 TTL 加 0~maxJitter 秒随机偏移，让 key 分散过期，避免同一时刻 DB 被冲爆。
     * 例：randomTtl(1800, 300) → 1800~2100 秒之间随机值。
     */
    private static long randomTtl(long baseSeconds, long maxJitterSeconds) {
        return baseSeconds + ThreadLocalRandom.current().nextLong(maxJitterSeconds);
    }

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
            // 穿透防御：DB 查不到，写空值缓存，TTL 短（2分钟基础 + 0~30秒随机抖动）
            stringRedisTemplate.opsForValue().set(key, "", randomTtl(120, 30), TimeUnit.SECONDS);
            throw new BusinessException(404, "商家不存在");
        }

        // 写正常缓存，TTL 30分钟基础 + 0~5分钟随机抖动（雪崩防御）
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), randomTtl(1800, 300), TimeUnit.SECONDS);

        return shop;
    }

    @Override
    public void updateShop(Shop shop) {
        if (shop.getId() == null) throw new BusinessException(400, "id不能为空");

        updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + shop.getId());

        // 如果本次更新带了坐标,顺便同步 GEO
        if (shop.getX() != null && shop.getY() != null) {
            stringRedisTemplate.opsForGeo().add(
                    "geo:shop",
                    new Point(shop.getX().doubleValue(), shop.getY().doubleValue()),
                    shop.getId().toString()
            );
        }
    }

    private boolean tryLock(String key) {
        // key 不存在才能设置成功，返回 true 表示抢到锁
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 10, TimeUnit.SECONDS);   // 锁 TTL 10 秒
        return Boolean.TRUE.equals(ok);   // 防 null
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
        log.info("释放锁: {}", key);
    }

    @Override
    public Shop queryByIdWithMutex(Long id) {
        String key = "cache:shop:" + id;

        // === 第一次查缓存 ===
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)) {
            log.info("缓存命中: {}", key);
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if (shopJson != null) {
            log.info("缓存命中空值（穿透防御）: {}", key);
            throw new BusinessException(404, "商家不存在");
        }

        // === 缓存 miss，开始抢锁 ===
        String lockKey = "lock:shop:" + id;
        boolean isLock = tryLock(lockKey);

        try {
            if (!isLock) {
                // 没抢到锁，等 50ms 再重试整个流程
                log.info("没抢到锁，等待重试: {}", lockKey);
                Thread.sleep(50);
                return queryByIdWithMutex(id);   // 递归
            }

            // 抢到锁了。【双重检查】：其他线程可能在我等待期间已经写好缓存
            log.info("抢到锁: {}", lockKey);
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                return JSONUtil.toBean(shopJson, Shop.class);
            }
            if (shopJson != null) {
                throw new BusinessException(404, "商家不存在");
            }

            // 查 DB + 写缓存
            log.info("查 DB: {}", id);
            Shop shop = getById(id);
            if (shop == null) {
                // 穿透 + 雪崩双防御
                stringRedisTemplate.opsForValue().set(key, "", randomTtl(120, 30), TimeUnit.SECONDS);
                throw new BusinessException(404, "商家不存在");
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), randomTtl(1800, 300), TimeUnit.SECONDS);
            return shop;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "缓存查询被中断");
        } finally {
            // 无论成功失败，必须释放锁
            if (isLock) {
                unlock(lockKey);
            }
        }
    }

    // ============================================================
    // 逻辑过期方案
    // ============================================================

    @Override
    public void saveShop2Redis(Long id, Long expireSeconds) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BusinessException(404, "商家不存在");
        }
        // 包装：业务数据 + 逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(randomTtl(expireSeconds, 300)));
        // 注意：set 不带 TTL —— Redis 物理永不过期
        stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(redisData));
        log.info("预热缓存: cache:shop:{}, 过期时间: {}", id, redisData.getExpireTime());
    }

    @Override
    public Shop queryByIdWithLogicalExpire(Long id) {
        String key = "cache:shop:" + id;

        // 1. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            log.warn("逻辑过期缓存未预热: {}", key);
            return null;
        }

        // 2. 反序列化 RedisData
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // data 字段反序列化时是 JSONObject，要再 toBean 一次
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 3. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            log.info("逻辑未过期，命中: {}", key);
            return shop;
        }

        // 4. 已过期，抢锁 + 异步更新
        log.info("逻辑已过期，准备异步更新: {}", key);
        String lockKey = "lock:shop:" + id;
        if (tryLock(lockKey)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShop2Redis(id, 30 * 60L);   // 30 分钟逻辑过期
                } catch (Exception e) {
                    log.error("异步缓存更新失败", e);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        // 5. 无论是否抢到锁，立刻返回旧数据（不阻塞用户）
        return shop;
    }

    @Override
    public void migrateAllShopsToGeo() {
        // 查所有有坐标的商家
        List<Shop> shops = lambdaQuery()
                .isNotNull(Shop::getX)
                .isNotNull(Shop::getY)
                .list();

        String key = "geo:shop";
        int count = 0;
        for (Shop shop : shops) {
            stringRedisTemplate.opsForGeo().add(
                    key,
                    new Point(shop.getX().doubleValue(), shop.getY().doubleValue()),
                    shop.getId().toString()
            );
            count++;
        }
        log.info("GEO 数据迁移完成，写入 {} 个商家", count);
    }

    public void updateLocation(Long id, BigDecimal x, BigDecimal y) {
        if (getById(id) == null) {
            throw new BusinessException(404, "商家不存在");
        }
        Shop update = new Shop();
        update.setId(id);
        update.setX(x);
        update.setY(y);
        updateShop(update);
    }

    @Override
    public List<NearbyShopVO> queryNearby(BigDecimal lng, BigDecimal lat, Double distance) {
        // Redis GEOSEARCH
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate
                .opsForGeo()
                .search(
                        "geo:shop",
                        GeoReference.fromCoordinate(lng.doubleValue(), lat.doubleValue()),
                        new Distance(distance, RedisGeoCommands.DistanceUnit.KILOMETERS),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .includeDistance()    // 要距离信息
                                .sortAscending()
                );

        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        // 提取 shopId 列表 和 shopId->距离Map
        List<Long> shopIds = new ArrayList<>();
        Map<Long, Double> distanceMap = new HashMap<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results.getContent()) {
            Long shopId = Long.valueOf(result.getContent().getName());
            shopIds.add(shopId);
            distanceMap.put(shopId, result.getDistance().getValue());
        }

        // 批量查 DB
        List<Shop> shops = listByIds(shopIds);
        Map<Long, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, s -> s));

        // 组装 VO
        List<NearbyShopVO> vos = new ArrayList<>();
        for (Long id : shopIds) {
            Shop shop = shopMap.get(id);
            if (shop == null) continue;   // DB 里可能已逻辑删但 GEO 还有，跳过
            NearbyShopVO vo = new NearbyShopVO();
            BeanUtil.copyProperties(shop, vo);
            vo.setDistance(distanceMap.get(id));
            vos.add(vo);
        }

        log.info("附近商家查询: ({}, {}) 半径 {}km，返回 {} 家", lng, lat, distance, vos.size());
        return vos;
    }
}
