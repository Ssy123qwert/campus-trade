package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Review;
import com.campustrade.entity.Order;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.ReviewMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public Review createReview(Order order, Long fromUserId, Integer rating, String content) {
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

        // 确定评价对象：如果评价人是买家，则评价卖家；反之亦然
        Long toUserId;
        if (fromUserId.equals(order.getBuyerId())) {
            toUserId = order.getSellerId();
        } else if (fromUserId.equals(order.getSellerId())) {
            toUserId = order.getBuyerId();
        } else {
            throw BusinessException.forbidden("无权评价此订单");
        }

        // 检查是否已评价（同一订单同一人只能评价一次）
        Long count = this.count(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getFromUserId, fromUserId));
        if (count > 0) {
            throw BusinessException.conflict("该订单已评价");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setFromUserId(fromUserId);
        review.setToUserId(toUserId);
        review.setRating(rating);
        review.setContent(content);
        this.save(review);

        // 更新被评价人的评分统计
        updateUserRating(toUserId);

        return review;
    }

    /**
     * 更新被评价人的评分统计
     * 重新计算该用户收到的所有评分总和，写入 User 表
     */
    private void updateUserRating(Long userId) {
        // 查询该用户收到的所有评价
        List<Review> reviews = this.list(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId));
        if (reviews.isEmpty()) return;

        int sum = reviews.stream().mapToInt(Review::getRating).sum();

        // 直接更新 User 表的相关字段
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId)
           .set(User::getTotalRating, sum)
           .set(User::getRatingCount, reviews.size());
        userMapper.update(null, uw);
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
