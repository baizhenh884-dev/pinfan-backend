package com.pinfan.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import com.pinfan.dto.LocationDTO;
import com.pinfan.dto.NearbyShopVO;
import com.pinfan.dto.ShopCreateDTO;
import com.pinfan.dto.ShopUpdateDTO;
import com.pinfan.entity.Shop;
import com.pinfan.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/shop")
@Tag(name = "03-商家", description = "商家增删改查")
@Slf4j
public class ShopController {
    @Autowired
    private ShopService shopService;

    @PostMapping
    @Operation(summary = "新增商家")
    public R<Void> addShop(@RequestBody @Valid ShopCreateDTO shopCreateDTO) {
        Shop shop = new Shop();
        BeanUtil.copyProperties(shopCreateDTO, shop);
        shopService.save(shop);
        return R.ok("商家已新增", null);
    }

    @PutMapping
    @Operation(summary = "修改商家（部分更新，null 字段不会改）")
    public R<Void> updateShop(@RequestBody @Valid ShopUpdateDTO shopUpdateDTO) {
        Shop shop = new Shop();
        BeanUtil.copyProperties(shopUpdateDTO, shop);
        shopService.updateShop(shop);   // MyBatis-Plus：null 字段不会被改
        return R.ok("已更新", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "商家详情（逻辑过期方案）")
    public R<Shop> detail(@PathVariable Long id) {
        Shop shop = shopService.queryByIdWithLogicalExpire(id);
        if (shop == null) {
            throw new BusinessException(404, "商家未预热或不存在");
        }
        return R.ok(shop);
    }

    @PostMapping("/{id}/warm-up")
    @Operation(summary = "预热商家到逻辑过期缓存（测试用）")
    public R<Void> warmUp(@PathVariable Long id,
                          @RequestParam(defaultValue = "1800") Long expireSeconds) {
        shopService.saveShop2Redis(id, expireSeconds);
        return R.ok("预热完成", null);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询商家（可按分类筛选）")
    public R<Page<Shop>> page(@RequestParam(defaultValue = "1") Long current,
                              @RequestParam(defaultValue = "10") Long size,
                              @RequestParam(required = false) Long categoryId) {
        Page<Shop> page = shopService.lambdaQuery()
                .eq(categoryId != null, Shop::getCategoryId, categoryId)  // 仅当 categoryId 不为 null 才加 WHERE
                .orderByDesc(Shop::getScore)                              // 评分降序
                .page(new Page<>(current, size));
        return R.ok(page);
    }

    @PostMapping("/admin/migrate-geo")
    @Operation(summary = "【管理员】把所有商家位置同步到 Redis GEO")
    public R<Void> migrateGeo() {
        shopService.migrateAllShopsToGeo();
        return R.ok("迁移完成", null);
    }

    @PostMapping("/{id}/location")
    @Operation(summary = "录入/更新商家位置（同步 DB + Redis GEO）")
    public R<Void> updateLocation(@PathVariable Long id,
                                  @RequestBody @Valid LocationDTO dto) {
        shopService.updateLocation(id, dto.getX(), dto.getY());
        return R.ok("位置已更新", null);
    }

    @GetMapping("/nearby")
    @Operation(summary = "查附近商家")
    public R<List<NearbyShopVO>> nearby(
            @RequestParam BigDecimal lng,
            @RequestParam BigDecimal lat,
            @RequestParam(defaultValue = "1.0") Double distance) {
        return R.ok(shopService.queryNearby(lng, lat, distance));
    }
}
