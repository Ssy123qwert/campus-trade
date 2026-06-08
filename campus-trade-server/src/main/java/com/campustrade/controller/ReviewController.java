package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.R;
import com.campustrade.entity.Order;
import com.campustrade.entity.Review;
import com.campustrade.service.OrderService;
import com.campustrade.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public R<Review> create(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        Integer rating = Integer.valueOf(params.get("rating").toString());
        String content = (String) params.get("content");

        Order order = orderService.getById(orderId);
        return R.ok(reviewService.createReview(order, rating, content));
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
