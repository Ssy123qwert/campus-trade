package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.R;
import com.campustrade.entity.Order;
import com.campustrade.entity.Review;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.OrderService;
import com.campustrade.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价管理 Controller
 *
 * 安全说明：创建评价时，评价人从 JWT 获取
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final OrderService orderService;
    private final SecurityUtil securityUtil;

    @PostMapping("/create")
    public R<Review> create(@RequestBody Map<String, Object> params) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) throw BusinessException.unauthorized("请先登录");

        Long orderId = Long.valueOf(params.get("orderId").toString());
        Integer rating = Integer.valueOf(params.get("rating").toString());
        String content = (String) params.get("content");

        Order order = orderService.getById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");

        // 只能评价自己的订单
        if (!order.getBuyerId().equals(currentUserId) && !order.getSellerId().equals(currentUserId)) {
            throw BusinessException.forbidden("无权评价此订单");
        }

        return R.ok(reviewService.createReview(order, currentUserId, rating, content));
    }

    @GetMapping("/user/{userId}")
    public R<Page<Review>> getByUser(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Page<Review> result = reviewService.page(
                new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getToUserId, userId)
                        .orderByDesc(Review::getCreateTime));
        return R.ok(result);
    }

    @GetMapping("/rate/{userId}")
    public R<Double> getRate(@PathVariable Long userId) {
        return R.ok(reviewService.getPositiveRate(userId));
    }
}
