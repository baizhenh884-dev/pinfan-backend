package com.pinfan.controller;

import com.pinfan.common.result.R;
import com.pinfan.dto.SeckillVoucherCreateDTO;
import com.pinfan.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/voucher")
@Tag(name = "07-优惠券")
@Slf4j
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @PostMapping("/seckill")
    @Operation(summary = "【管理员】创建秒杀券")
    public R<Long> createSeckill(@RequestBody @Valid SeckillVoucherCreateDTO dto) {
        Long id = voucherService.createSeckillVoucher(dto);
        return R.ok("秒杀券已创建", id);
    }
}
