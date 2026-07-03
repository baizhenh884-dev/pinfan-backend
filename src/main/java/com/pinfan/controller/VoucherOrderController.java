package com.pinfan.controller;

import com.pinfan.common.result.R;
import com.pinfan.service.VoucherOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voucher-order")
@Tag(name = "08-抢券", description = "5版秒杀实现对比")
@Slf4j
public class VoucherOrderController {

    @Autowired
    private VoucherOrderService voucherOrderService;

    @PostMapping("/seckill-v1/{voucherId}")
    @Operation(summary = "V1 synchronized 单机锁")
    public R<Long> seckillV1(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucherV1(voucherId);
        return R.ok("抢购成功", orderId);
    }

    @PostMapping("/seckill-v2/{voucherId}")
    @Operation(summary = "V2 乐观锁 CAS + AOP事务修复")
    public R<Long> seckillV2(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucherV2(voucherId);
        return R.ok("抢购成功", orderId);
    }

    @PostMapping("/seckill-v3/{voucherId}")
    @Operation(summary = "V3 Redis 预扣库存 + Lua 脚本")
    public R<Long> seckillV3(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucherV3(voucherId);
        return R.ok("抢购成功", orderId);
    }

    @PostMapping("/seckill-v4/{voucherId}")
    @Operation(summary = "V4 Redisson 分布式锁")
    public R<Long> seckillV4(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucherV4(voucherId);
        return R.ok("抢购成功", orderId);
    }

    @PostMapping("/seckill-v5/{voucherId}")
    @Operation(summary = "V5 Redis Stream 异步落库")
    public R<Long> seckillV5(@PathVariable Long voucherId) {
        Long orderId = voucherOrderService.seckillVoucherV5(voucherId);
        return R.ok("抢购成功", orderId);
    }
}
