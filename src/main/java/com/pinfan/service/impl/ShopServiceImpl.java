package com.pinfan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.entity.Shop;
import com.pinfan.mapper.ShopMapper;
import com.pinfan.service.ShopService;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
                             implements ShopService {
}
