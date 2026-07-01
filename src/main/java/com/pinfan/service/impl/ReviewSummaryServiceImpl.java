package com.pinfan.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.entity.Review;
import com.pinfan.entity.ReviewSummary;
import com.pinfan.entity.Shop;
import com.pinfan.mapper.ReviewSummaryMapper;
import com.pinfan.service.AiService;
import com.pinfan.service.ReviewService;
import com.pinfan.service.ReviewSummaryService;
import com.pinfan.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReviewSummaryServiceImpl extends ServiceImpl<ReviewSummaryMapper, ReviewSummary>
        implements ReviewSummaryService {

    @Autowired
    private AiService aiService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${pinfan.ai.deepseek.model}")

    private String modelName;

    // Prompt 模板

    private static final String PROMPT_TEMPLATE = """

你是一个专业的餐厅评论分析师。以下是「%s」这家店的 %d 条真实用户评论：

---

%s

---

请分析后生成三段式总结。每段控制在 30-80 字，语言自然、有针对性。

要求：

1. "pros"：用户普遍认可的方面（菜品/服务/环境/性价比等）

2. "cons"：用户吐槽的痛点。如果几乎没有负面评价，写"暂无明显槽点"

3. "suitable_for"：适合的用户群体或用餐场景（如"朋友聚餐"、"家庭日常"）

严格返回 JSON 格式，不要包含任何解释文字：

{"pros": "...", "cons": "...", "suitable_for": "..."}

""";

    @Override

    public void generateForShop(Long shopId) {

        // 1. 校验商家存在
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(404, "商家不存在");
        }

        // 2. 查最近 50 条评论
        List<Review> reviews = reviewService.lambdaQuery()
                .eq(Review::getShopId, shopId)
                .orderByDesc(Review::getCreateTime)
                .last("LIMIT 50")
                .list();

        if (reviews.isEmpty()) {
            throw new BusinessException(400, "该店暂无评论，无法生成摘要");
        }

        // 3. 拼评论文本
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Review r : reviews) {
            String content = StrUtil.isBlank(r.getContent()) ? "(仅评分)" : r.getContent();
            sb.append(idx++).append(". [").append(r.getRating()).append("星] ").append(content).append("\n");
        }

        // 4. 调 AI
        String prompt = String.format(PROMPT_TEMPLATE, shop.getName(), reviews.size(), sb.toString());
        log.info("准备生成摘要: shopId={}, 评论数={}", shopId, reviews.size());
        String json = aiService.chatJson(prompt);

        // 5. 解析 JSON
        Map<String, String> parsed = JSONUtil.toBean(json, Map.class);
        String pros = parsed.get("pros");
        String cons = parsed.get("cons");
        String suitableFor = parsed.get("suitable_for");

        // 6. Upsert 到 DB（一店一条，用 shop_id 唯一约束）
        ReviewSummary summary = lambdaQuery().eq(ReviewSummary::getShopId, shopId).one();
        if (summary == null) {
            summary = new ReviewSummary();
            summary.setShopId(shopId);
        }

        summary.setPros(pros);
        summary.setCons(cons);
        summary.setSuitableFor(suitableFor);
        summary.setReviewCount(reviews.size());
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setModelName(modelName);
        saveOrUpdate(summary);

        // 7. 删缓存
        stringRedisTemplate.delete("cache:review-summary:" + shopId);
        log.info("摘要生成完成: shopId={}, pros={}", shopId, pros);
    }

    @Override
    public ReviewSummary querySummary(Long shopId) {
        String key = "cache:review-summary:" + shopId;
        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json)) {
            log.info("摘要缓存命中: shopId={}", shopId);
            return JSONUtil.toBean(json, ReviewSummary.class);
        }

        ReviewSummary summary = lambdaQuery().eq(ReviewSummary::getShopId, shopId).one();
        if (summary == null) {
            return null;   // 从未生成过
        }

        // 缓存 24h + 随机抖动
        long ttl = 86400 + ThreadLocalRandom.current().nextLong(3600);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(summary), ttl, TimeUnit.SECONDS);
        return summary;
    }
}
