package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Order;
import com.campustrade.entity.Product;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.OrderMapper;
import com.campustrade.service.OrderService;
import com.campustrade.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ProductService productService;

    @Override
    @Transactional
    public Order create(Long productId, Long buyerId) {
        if (productId == null || buyerId == null) {
            throw BusinessException.badRequest("商品ID和买家ID不能为空");
        }

        Product product = productService.getById(productId);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        if (product.getStatus() != 1) {
            throw BusinessException.badRequest("该商品已售出或已下架");
        }
        if (product.getUserId().equals(buyerId)) {
            throw BusinessException.badRequest("不能购买自己的商品");
        }

        // 使用乐观锁标记商品为已售，防止并发重复下单
        boolean locked = productService.update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, 1)
                .set(Product::getStatus, 2));
        if (!locked) {
            throw BusinessException.conflict("该商品已被其他用户抢先购买");
        }

        // 创建订单，状态为待支付
        Order order = new Order();
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setAmount(product.getPrice());
        order.setStatus(0);  // 待支付
        boolean saved = this.save(order);
        if (!saved) {
            // 回滚商品状态
            product.setStatus(1);
            productService.updateById(product);
            throw BusinessException.serverError("创建订单失败");
        }
        return order;
    }

    @Override
    @Transactional
    public Order pay(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            throw BusinessException.badRequest("订单ID和用户ID不能为空");
        }

        Order order = this.getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw BusinessException.forbidden("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("订单状态不正确，当前状态: " + getStatusText(order.getStatus()));
        }

        Product product = productService.getById(order.getProductId());
        if (product == null) {
            throw BusinessException.notFound("关联商品不存在");
        }

        // 原子更新订单状态（乐观锁）
        boolean updated = this.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, 0)
                .set(Order::getStatus, 1));
        if (!updated) {
            throw BusinessException.conflict("订单状态已变更，请刷新后重试");
        }

        order.setStatus(1);
        return order;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "待发货";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
