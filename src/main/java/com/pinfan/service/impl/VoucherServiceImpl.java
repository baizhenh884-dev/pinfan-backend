package com.pinfan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.dto.SeckillVoucherCreateDTO;
import com.pinfan.entity.SeckillVoucher;
import com.pinfan.entity.Voucher;
import com.pinfan.mapper.VoucherMapper;
import com.pinfan.service.SeckillVoucherService;
import com.pinfan.service.VoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher>
                                implements VoucherService {
    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public Long createSeckillVoucher(SeckillVoucherCreateDTO dto) {

        // 存 voucher 表
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(dto, voucher);
        voucher.setType(1);       // 1 秒杀券
        voucher.setStatus(1);     // 上架
        save(voucher);            // save 完 voucher.id 会被回填

        // 存 seckill_voucher 表
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());   // ⚠️ 必须手动 set（IdType.INPUT）
        seckillVoucher.setStock(dto.getStock());
        seckillVoucher.setBeginTime(dto.getBeginTime());
        seckillVoucher.setEndTime(dto.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 同步库存到 Redis，供 V3-V5 秒杀入口快速判断和预扣库存
        stringRedisTemplate.opsForValue().set(
                "seckill:stock:" + voucher.getId(),
                dto.getStock().toString()
        );

        log.info("创建秒杀券成功: id={}, title={}, stock={}", voucher.getId(), voucher.getTitle(), dto.getStock());
        return voucher.getId();

    }
}
