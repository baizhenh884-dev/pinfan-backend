package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.dto.NearbyShopVO;
import com.pinfan.entity.Shop;

import java.math.BigDecimal;
import java.util.List;

public interface ShopService extends IService<Shop> {
    Shop queryById(Long id);

    void updateShop(Shop shop);

    Shop queryByIdWithMutex(Long id);

    /** 把指定商家以"逻辑过期"格式预热进 Redis（无 TTL，过期时间写在数据里） */
    void saveShop2Redis(Long id, Long expireSeconds);

    /** 用"逻辑过期"方案查商家 */
    Shop queryByIdWithLogicalExpire(Long id);

    // 一次性数据迁移：把所有商家的 x/y 写入 Redis GEO
    void migrateAllShopsToGeo();

    void updateLocation(Long id, BigDecimal x, BigDecimal y);

    List<NearbyShopVO> queryNearby(BigDecimal lng, BigDecimal lat, Double distance);
}
