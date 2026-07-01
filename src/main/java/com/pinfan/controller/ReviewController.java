package com.pinfan.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import com.pinfan.dto.ReviewCreateDTO;
import com.pinfan.entity.Review;
import com.pinfan.entity.ReviewSummary;
import com.pinfan.entity.User;
import com.pinfan.scheduled.ReviewSummaryScheduler;
import com.pinfan.service.ReviewService;
import com.pinfan.service.ReviewSummaryService;
import com.pinfan.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/review")
@Tag(name = "06-评论", description = "评论提交和查询")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewSummaryService reviewSummaryService;

    @Autowired
    private ReviewSummaryScheduler reviewSummaryScheduler;

    @PostMapping
    @Operation(summary = "提交评论")
    public R<Void> add(@RequestBody @Valid ReviewCreateDTO dto) {
        // 从 ThreadLocal 拿当前登录用户
        User user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        reviewService.addReview(dto, user.getId());
        return R.ok("评论已提交", null);
    }

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "查某店评论分页")
    public R<Page<Review>> list(@PathVariable Long shopId,
                                @RequestParam(defaultValue = "1") Long current,
                                @RequestParam(defaultValue = "10") Long size) {
        return R.ok(reviewService.queryByShop(shopId, current, size));
    }

    @PostMapping("/summary/{shopId}/refresh")
    @Operation(summary = "【手动】触发生成商家 AI 评论摘要")
    public R<Void> refreshSummary(@PathVariable Long shopId) {
        reviewSummaryService.generateForShop(shopId);
        return R.ok("摘要已刷新", null);
    }

    @GetMapping("/summary/{shopId}")
    @Operation(summary = "查商家 AI 评论摘要")
    public R<ReviewSummary> getSummary(@PathVariable Long shopId) {
        ReviewSummary summary = reviewSummaryService.querySummary(shopId);
        if (summary == null) {
            throw new BusinessException(404, "该店暂无 AI 摘要，请先触发生成");
        }
        return R.ok(summary);
    }

    @PostMapping("/summary/scheduled/run")
    @Operation(summary = "【调试】立刻执行定时任务的批量生成逻辑")
    public R<Void> runScheduled() {
        reviewSummaryScheduler.generateSummaries();
        return R.ok("批量生成完成", null);
    }
}