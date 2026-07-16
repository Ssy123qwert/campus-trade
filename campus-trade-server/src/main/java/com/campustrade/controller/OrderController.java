package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Order;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单交易 Controller
 *
 * 状态机规则：
 * - 下单(创建) → 支付 → 发货 → 确认收货
 * - 待支付可直接取消
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtil securityUtil;

    @PostMapping("/create")
    public R<Order> create(@RequestParam Long productId) {
        Long buyerId = securityUtil.getCurrentUserId();
        if (buyerId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(orderService.create(productId, buyerId));
    }

    @PostMapping("/pay")
    public R<Order> pay(@RequestParam Long orderId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(orderService.pay(orderId, userId));
    }

    @PostMapping("/cancel")
    public R<Order> cancel(@RequestParam Long orderId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(orderService.cancel(orderId, userId));
    }

    @PostMapping("/ship")
    public R<Order> ship(@RequestParam Long orderId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(orderService.ship(orderId, userId));
    }

    @PostMapping("/confirm")
    public R<Order> confirm(@RequestParam Long orderId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(orderService.confirmReceipt(orderId, userId));
    }

    @GetMapping("/my")
    public R<List<Order>> myOrders(@RequestParam(defaultValue = "buyer") String type) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");

        List<Order> list = orderService.lambdaQuery()
                .eq("buyer".equals(type) ? Order::getBuyerId : Order::getSellerId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();
        return R.ok(list);
    }

    @GetMapping("/check")
    public R<Boolean> check(@RequestParam Long productId) {
        Long buyerId = securityUtil.getCurrentUserId();
        if (buyerId == null) return R.ok(false);
        long count = orderService.lambdaQuery()
                .eq(Order::getProductId, productId)
                .eq(Order::getBuyerId, buyerId)
                .eq(Order::getStatus, OrderService.PENDING_PAY)
                .count();
        return R.ok(count > 0);
    }
}
