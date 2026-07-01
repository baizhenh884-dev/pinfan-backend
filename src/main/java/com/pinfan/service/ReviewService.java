package com.pinfan.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.dto.ReviewCreateDTO;
import com.pinfan.entity.Review;

public interface ReviewService extends IService<Review> {
    void addReview(ReviewCreateDTO dto, Long userId);
    Page<Review> queryByShop(Long shopId, Long current, Long size);
}
