package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Order;
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
        if (order == null) {
            return R.fail("商品不存在或已售出");
        }
        return R.ok(order);
    }

    @GetMapping("/my")
    public R<List<Order>> myOrders(@RequestParam Long userId, @RequestParam(defaultValue = "buyer") String type) {
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
    public R<?> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        Order order = orderService.getById(id);
        if (order != null) {
            order.setStatus(status);
            orderService.updateById(order);
        }
        return R.ok();
    }

    @PostMapping("/pay")
    public R<Order> pay(@RequestParam Long orderId, @RequestParam Long userId) {
        Order order = orderService.pay(orderId, userId);
        if (order == null) {
            return R.fail("支付失败，订单不存在或已支付");
        }
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
