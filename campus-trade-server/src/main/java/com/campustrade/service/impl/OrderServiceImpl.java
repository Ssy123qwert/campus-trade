package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Order;
import com.campustrade.entity.Product;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.OrderService;
import com.campustrade.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 *
 * 状态机规则：
 * - 0(待支付) → 1(已支付)：买家支付
 * - 1(已支付) → 2(已发货)：卖家发货
 * - 2(已发货) → 3(已完成)：买家确认收货
 * - 0(待支付) → 4(已取消)：买家取消 / 超时自动取消
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductService productService;
    private final TaskScheduler taskScheduler;

    @Override
    @Transactional
    public Order create(Long productId, Long buyerId) {
        if (productId == null || buyerId == null) {
            throw BusinessException.badRequest("商品ID和买家ID不能为空");
        }

        Product product = productService.getById(productId);
        if (product == null) throw BusinessException.notFound("商品不存在");
        if (product.getStatus() != 1) throw BusinessException.badRequest("该商品已售出或已下架");
        if (product.getUserId().equals(buyerId)) throw BusinessException.badRequest("不能购买自己的商品");

        // 乐观锁标记商品为已售，防并发
        boolean locked = productService.update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, 1)
                .set(Product::getStatus, 2));
        if (!locked) throw BusinessException.conflict("该商品已被其他用户抢先购买");

        // 生成订单号：ORD + yyyyMMddHHmmss + 6位随机
        String orderNo = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", (int) (Math.random() * 1000000));

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setAmount(product.getPrice());
        order.setStatus(PENDING_PAY);
        boolean saved = this.save(order);
        if (!saved) {
            // 回滚商品状态
            product.setStatus(1);
            productService.updateById(product);
            throw BusinessException.serverError("创建订单失败");
        }

        // 发送延迟取消消息（MQ 不可用时无影响）
        sendDelayCancel(order.getId());

        return order;
    }

    @Override
    @Transactional
    public Order pay(Long orderId, Long userId) {
        Order order = validateOwner(orderId, userId);
        if (order.getStatus() != PENDING_PAY) {
            throw BusinessException.badRequest("订单状态不正确，当前: " + getStatusText(order.getStatus()));
        }

        // 原子更新
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, PENDING_PAY)
                .set(Order::getStatus, PAID)
                .set(Order::getPayTime, LocalDateTime.now()));
        if (!updated) throw BusinessException.conflict("订单状态已变更");

        order.setStatus(PAID);
        order.setPayTime(LocalDateTime.now());
        return order;
    }

    @Override
    @Transactional
    public Order cancel(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (!order.getBuyerId().equals(userId)) throw BusinessException.forbidden("无权操作");
        if (order.getStatus() != PENDING_PAY) {
            throw BusinessException.badRequest("只有待支付订单可以取消");
        }

        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, PENDING_PAY)
                .set(Order::getStatus, CANCELLED)
                .set(Order::getCancelTime, LocalDateTime.now()));
        if (!updated) throw BusinessException.conflict("订单状态已变更");

        // 恢复商品状态为在售
        productService.update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, order.getProductId())
                .set(Product::getStatus, 1));

        order.setStatus(CANCELLED);
        return order;
    }

    @Override
    @Transactional
    public Order ship(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (!order.getSellerId().equals(userId)) throw BusinessException.forbidden("只有卖家可以发货");
        if (order.getStatus() != PAID) {
            throw BusinessException.badRequest("只有已支付订单可以发货");
        }

        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, PAID)
                .set(Order::getStatus, SHIPPED));
        if (!updated) throw BusinessException.conflict("订单状态已变更");

        order.setStatus(SHIPPED);
        return order;
    }

    @Override
    @Transactional
    public Order confirmReceipt(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (!order.getBuyerId().equals(userId)) throw BusinessException.forbidden("只有买家可以确认收货");
        if (order.getStatus() != SHIPPED) {
            throw BusinessException.badRequest("只有已发货订单可以确认收货");
        }

        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, SHIPPED)
                .set(Order::getStatus, COMPLETED));
        if (!updated) throw BusinessException.conflict("订单状态已变更");

        order.setStatus(COMPLETED);
        return order;
    }

    @Override
    public void sendDelayCancel(Long orderId) {
        // 30 分钟后自动检查并取消未支付订单
        taskScheduler.schedule(() -> {
            try {
                Order order = getById(orderId);
                if (order != null && order.getStatus() == PENDING_PAY) {
                    // 自动取消订单
                    this.update(new LambdaUpdateWrapper<Order>()
                            .eq(Order::getId, orderId)
                            .eq(Order::getStatus, PENDING_PAY)
                            .set(Order::getStatus, CANCELLED)
                            .set(Order::getCancelTime, LocalDateTime.now()));
                    // 恢复商品状态
                    productService.update(new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, order.getProductId())
                            .set(Product::getStatus, 1));
                    log.info("订单 {} 超时未支付，已自动取消", orderId);
                }
            } catch (Exception e) {
                log.warn("自动取消订单 {} 失败: {}", orderId, e.getMessage());
            }
        }, Instant.now().plusSeconds(1800)); // 30 分钟后执行
    }

    // ==================== 工具 ====================

    /**
     * 验证订单存在且属于当前用户
     */
    private Order validateOwner(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");
        if (!order.getBuyerId().equals(userId)) throw BusinessException.forbidden("无权操作此订单");
        return order;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case PENDING_PAY -> "待支付";
            case PAID -> "已支付";
            case SHIPPED -> "已发货";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}
