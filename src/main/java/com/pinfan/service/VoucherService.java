package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.dto.SeckillVoucherCreateDTO;
import com.pinfan.entity.Voucher;

public interface VoucherService extends IService<Voucher> {
    Long createSeckillVoucher(SeckillVoucherCreateDTO dto);
}
