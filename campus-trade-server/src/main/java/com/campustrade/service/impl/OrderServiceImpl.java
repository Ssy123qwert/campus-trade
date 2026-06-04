package com.campustrade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Order;
import com.campustrade.entity.Product;
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
        Product product = productService.getById(productId);
        if (product == null || product.getStatus() != 1) {
            return null;
        }

        // 不能买自己的商品
        if (product.getUserId().equals(buyerId)) {
            return null;
        }

        // 创建订单，状态为待支付
        Order order = new Order();
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setAmount(product.getPrice());
        order.setStatus(0);  // 待支付
        this.save(order);

        return order;
    }

    @Override
    @Transactional
    public Order pay(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) {
            return null;
        }
        if (!order.getBuyerId().equals(userId)) {
            return null;
        }
        if (order.getStatus() != 0) {
            return null;
        }

        Product product = productService.getById(order.getProductId());
        if (product == null || product.getStatus() != 1) {
            return null;
        }

        // 更新订单状态为已支付(待发货)
        order.setStatus(1);
        this.updateById(order);

        // 更新商品状态为已售出
        product.setStatus(2);
        productService.updateById(product);

        return order;
    }
}
