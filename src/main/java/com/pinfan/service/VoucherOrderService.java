package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.entity.VoucherOrder;

public interface VoucherOrderService extends IService<VoucherOrder> {
    Long seckillVoucherV1(Long voucherId);

    Long seckillVoucherV2(Long voucherId);          // 外层入口
    Long createVoucherOrder(Long voucherId);        // 内层 @Transactional 方法

    Long seckillVoucherV3(Long voucherId);
    Long createOrderFromRedis(Long voucherId, Long userId);   // V3-V5 共用的 DB 落库方法

    Long seckillVoucherV4(Long voucherId);

    Long seckillVoucherV5(Long voucherId);
}
