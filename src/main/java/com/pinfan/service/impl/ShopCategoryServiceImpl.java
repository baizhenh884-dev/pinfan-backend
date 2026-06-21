package com.pinfan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.entity.ShopCategory;
import com.pinfan.mapper.ShopCategoryMapper;
import com.pinfan.service.ShopCategoryService;
import org.springframework.stereotype.Service;

@Service
public class ShopCategoryServiceImpl extends ServiceImpl<ShopCategoryMapper, ShopCategory>
                                     implements ShopCategoryService {
}
