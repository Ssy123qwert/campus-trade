package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Review;
import com.campustrade.entity.Order;

public interface ReviewService extends IService<Review> {
    /**
     * 创建评价
     * @param order 订单
     * @param fromUserId 评价人（买方或卖方）
     * @param rating 评分 1-5
     * @param content 评价内容
     */
    Review createReview(Order order, Long fromUserId, Integer rating, String content);
    Double getPositiveRate(Long userId);
}
