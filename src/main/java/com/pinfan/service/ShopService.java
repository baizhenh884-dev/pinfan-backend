package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.entity.Shop;

public interface ShopService extends IService<Shop> {
    Shop queryById(Long id);

    void updateShop(Shop shop);

    Shop queryByIdWithMutex(Long id);

    /** 把指定商家以"逻辑过期"格式预热进 Redis（无 TTL，过期时间写在数据里） */
    void saveShop2Redis(Long id, Long expireSeconds);

    /** 用"逻辑过期"方案查商家 */
    Shop queryByIdWithLogicalExpire(Long id);
}
