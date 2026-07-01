package com.pinfan.scheduled;

import com.pinfan.entity.Review;
import com.pinfan.entity.ReviewSummary;
import com.pinfan.entity.Shop;
import com.pinfan.service.ReviewService;
import com.pinfan.service.ReviewSummaryService;
import com.pinfan.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ReviewSummaryScheduler {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewSummaryService reviewSummaryService;

    /**
     * 每天凌晨 3 点扫描所有商家，给"有新评论"的店重新生成 AI 摘要
     * cron 6 位：秒 分 时 日 月 星期
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void generateSummaries() {
        log.info("=== AI 摘要定时任务开始 ===");
        long start = System.currentTimeMillis();

        List<Shop> shops = shopService.list();
        int generated = 0;
        int skipped = 0;
        int failed = 0;

        for (Shop shop : shops) {
            if (!shouldRegenerate(shop.getId())) {
                skipped++;
                continue;
            }
            try {
                reviewSummaryService.generateForShop(shop.getId());
                generated++;
                // 避免打满 DeepSeek 限流，每次调用之间小睡一会
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("定时任务被中断");
                break;
            } catch (Exception e) {
                failed++;
                log.error("生成摘要失败: shopId={}", shop.getId(), e);
            }
        }

        long cost = System.currentTimeMillis() - start;
        log.info("=== AI 摘要定时任务完成: 生成 {} 家, 跳过 {} 家, 失败 {} 家, 耗时 {}ms ===",
                generated, skipped, failed, cost);
    }

    /**
     * 判断是否需要重新生成：
     * - 无评论 → 不生成
     * - 无已有摘要 → 生成
     * - 最新评论时间 > 摘要生成时间 → 生成
     * - 否则 → 跳过
     */
    private boolean shouldRegenerate(Long shopId) {
        Review latestReview = reviewService.lambdaQuery()
                .eq(Review::getShopId, shopId)
                .orderByDesc(Review::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (latestReview == null) {
            return false;
        }

        ReviewSummary existing = reviewSummaryService.lambdaQuery()
                .eq(ReviewSummary::getShopId, shopId)
                .one();
        if (existing == null) {
            return true;
        }

        return latestReview.getCreateTime().isAfter(existing.getGeneratedAt());
    }
}