package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public R<Order> create(@RequestParam Long productId, @RequestParam Long buyerId) {
        Order order = orderService.create(productId, buyerId);
        return R.ok(order);
    }

    @GetMapping("/my")
    public R<List<Order>> myOrders(@RequestParam Long userId,
                                    @RequestParam(defaultValue = "buyer") String type) {
        List<Order> list;
        if ("seller".equals(type)) {
            list = orderService.lambdaQuery()
                    .eq(Order::getSellerId, userId)
                    .orderByDesc(Order::getCreateTime)
                    .list();
        } else {
            list = orderService.lambdaQuery()
                    .eq(Order::getBuyerId, userId)
                    .orderByDesc(Order::getCreateTime)
                    .list();
        }
        return R.ok(list);
    }

    @PutMapping("/status")
    public R<?> updateStatus(@RequestParam Long id, @RequestParam Integer status,
                              @RequestParam Long userId) {
        Order order = orderService.getById(id);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        // 权限校验：只有卖家能发货，只有买家能确认收货
        if (!order.getSellerId().equals(userId) && !order.getBuyerId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此订单");
        }
        order.setStatus(status);
        orderService.updateById(order);
        return R.ok();
    }

    @PostMapping("/pay")
    public R<Order> pay(@RequestParam Long orderId, @RequestParam Long userId) {
        Order order = orderService.pay(orderId, userId);
        return R.ok(order);
    }

    @GetMapping("/check")
    public R<Boolean> check(@RequestParam Long productId, @RequestParam Long buyerId) {
        long count = orderService.lambdaQuery()
                .eq(Order::getProductId, productId)
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getStatus, 0)
                .count();
        return R.ok(count > 0);
    }
}
