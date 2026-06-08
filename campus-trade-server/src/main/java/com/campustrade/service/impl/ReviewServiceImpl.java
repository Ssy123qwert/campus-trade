package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Review;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.ReviewMapper;
import com.campustrade.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Override
    @Transactional
    public Review createReview(Order order, Integer rating, String content) {
        if (order == null) {
            throw BusinessException.badRequest("订单不存在");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw BusinessException.badRequest("评分需在1-5之间");
        }
        if (content != null && content.length() > 500) {
            throw BusinessException.badRequest("评价内容不超过500字");
        }
        if (order.getStatus() != 3) {
            throw BusinessException.badRequest("只能对已完成的订单进行评价");
        }

        // 检查是否已评价
        Long count = this.count(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getFromUserId, order.getBuyerId()));
        if (count > 0) {
            throw BusinessException.conflict("该订单已评价");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setFromUserId(order.getBuyerId());
        review.setToUserId(order.getSellerId());
        review.setRating(rating);
        review.setContent(content);
        this.save(review);
        return review;
    }

    @Override
    public Double getPositiveRate(Long userId) {
        Long total = this.count(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId));
        if (total == 0) return -1.0; // 无评价

        Long positive = this.count(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId)
                .ge(Review::getRating, 4));
        return Math.round((double) positive / total * 10000.0) / 100.0;
    }
}
