package com.pinfan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinfan.entity.ReviewSummary;

public interface ReviewSummaryService extends IService<ReviewSummary> {
    // 触发生成新摘要（手动/定时任务调用）
    void generateForShop(Long shopId);

    // 查摘要（缓存优先，前端调）
    ReviewSummary querySummary(Long shopId);
}
