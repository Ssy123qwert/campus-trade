package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Review;
import com.campustrade.entity.Order;

public interface ReviewService extends IService<Review> {
    Review createReview(Order order, Integer rating, String content);
    Double getPositiveRate(Long userId);
}
