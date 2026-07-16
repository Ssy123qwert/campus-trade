package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Order;

/**
 * 订单服务
 *
 * 状态机：0=待支付 → 1=已支付 → 2=已发货 → 3=已完成
 *         0=待支付 → 4=已取消（超时/手动）
 */
public interface OrderService extends IService<Order> {

    /** 订单状态 */
    int PENDING_PAY = 0;    // 待支付
    int PAID = 1;           // 已支付/待发货
    int SHIPPED = 2;        // 已发货
    int COMPLETED = 3;      // 已完成
    int CANCELLED = 4;      // 已取消

    /**
     * 创建订单（含乐观锁防超卖、生成订单号、创建快照）
     */
    Order create(Long productId, Long buyerId);

    /**
     * 支付订单（模拟支付，0→1）
     */
    Order pay(Long orderId, Long userId);

    /**
     * 取消订单（0→4，只有待支付可取消）
     */
    Order cancel(Long orderId, Long userId);

    /**
     * 确认收货（2→3）
     */
    Order confirmReceipt(Long orderId, Long userId);

    /**
     * 带状态校验的更新（发货 1→2）
     */
    Order ship(Long orderId, Long userId);

    /**
     * 发送延迟取消消息到 RabbitMQ（下单时调用）
     * MQ 不可用时不做处理，订单需手动取消
     */
    void sendDelayCancel(Long orderId);
}
