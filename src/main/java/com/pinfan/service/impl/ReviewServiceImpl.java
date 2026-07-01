package com.pinfan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.dto.ReviewCreateDTO;
import com.pinfan.entity.Review;
import com.pinfan.mapper.ReviewMapper;
import com.pinfan.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {
    @Override
    public void addReview(ReviewCreateDTO dto, Long userId) {
        Review review = new Review();
        BeanUtil.copyProperties(dto, review);
        review.setUserId(userId);
        save(review);
        log.info("用户 {} 给商家 {} 提交评论：{}星", userId, dto.getShopId(), dto.getRating());
    }

    @Override
    public Page<Review> queryByShop(Long shopId, Long current, Long size) {
        return lambdaQuery()
                .eq(Review::getShopId, shopId)
                .orderByDesc(Review::getCreateTime)   // 新评论在前
                .page(new Page<>(current, size));
    }
}
