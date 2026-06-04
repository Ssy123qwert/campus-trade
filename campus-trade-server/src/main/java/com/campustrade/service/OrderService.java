package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Order;

public interface OrderService extends IService<Order> {
    Order create(Long productId, Long buyerId);
    Order pay(Long orderId, Long userId);
}
