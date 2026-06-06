package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Message;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.MessageMapper;
import com.campustrade.service.MessageService;
import com.campustrade.service.ProductService;
import com.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Override
    public Message send(Long fromUserId, Long toUserId, Long productId, String content) {
        if (fromUserId == null || toUserId == null || productId == null) {
            throw BusinessException.badRequest("发送方、接收方和商品ID不能为空");
        }
        if (content == null || content.isBlank()) {
            throw BusinessException.badRequest("消息内容不能为空");
        }
        if (content.length() > 1000) {
            throw BusinessException.badRequest("消息内容不能超过1000个字符");
        }

        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setProductId(productId);
        message.setContent(content);
        message.setIsRead(0);
        boolean saved = this.save(message);
        if (!saved) {
            throw BusinessException.serverError("发送消息失败");
        }
        return message;
    }

    @Override
    public List<Message> getConversation(Long userId, Long otherUserId, Long productId) {
        if (userId == null || otherUserId == null || productId == null) {
            throw BusinessException.badRequest("参数不能为空");
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Message::getFromUserId, userId).eq(Message::getToUserId, otherUserId))
                .or(w2 -> w2.eq(Message::getFromUserId, otherUserId).eq(Message::getToUserId, userId)))
                .eq(Message::getProductId, productId)
                .orderByAsc(Message::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        if (userId == null) {
            throw BusinessException.badRequest("用户ID不能为空");
        }

        // 获取所有与 userId 相关的消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getFromUserId, userId).or().eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime);
        List<Message> messages = this.list(wrapper);

        // 按会话分组
        Map<String, Message> convMap = new LinkedHashMap<>();
        for (Message msg : messages) {
            Long other = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            String key = other + "_" + msg.getProductId();
            convMap.putIfAbsent(key, msg);
        }

        // 批量查询相关用户和商品，解决 N+1 问题
        Set<Long> userIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        for (Message msg : convMap.values()) {
            Long otherId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            userIds.add(otherId);
            productIds.add(msg.getProductId());
        }

        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Product> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 批量查询未读数
        Map<String, Long> unreadMap = new HashMap<>();
        for (Message msg : convMap.values()) {
            Long otherId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            String key = otherId + "_" + msg.getProductId();
            long unread = this.count(new LambdaQueryWrapper<Message>()
                    .eq(Message::getFromUserId, otherId)
                    .eq(Message::getToUserId, userId)
                    .eq(Message::getProductId, msg.getProductId())
                    .eq(Message::getIsRead, 0));
            unreadMap.put(key, unread);
        }

        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message msg : convMap.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            Long otherId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            String key = otherId + "_" + msg.getProductId();

            User otherUser = userMap.get(otherId);
            Product product = productMap.get(msg.getProductId());

            item.put("otherUserId", otherId);
            item.put("otherUserName", otherUser != null ? otherUser.getNickname() : "未知用户");
            item.put("otherAvatar", otherUser != null ? otherUser.getAvatar() : "");
            item.put("productId", msg.getProductId());
            item.put("productTitle", product != null ? product.getTitle() : "未知商品");
            item.put("lastMessage", msg.getContent());
            item.put("lastTime", msg.getCreateTime());
            item.put("unreadCount", unreadMap.getOrDefault(key, 0L).intValue());

            result.add(item);
        }
        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        if (userId == null) {
            throw BusinessException.badRequest("用户ID不能为空");
        }
        return (int) this.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public void markAsRead(Long fromUserId, Long toUserId, Long productId) {
        if (fromUserId == null || toUserId == null || productId == null) {
            throw BusinessException.badRequest("参数不能为空");
        }
        // 批量更新替代逐条更新，一次 SQL 完成
        this.update(new LambdaUpdateWrapper<Message>()
                .eq(Message::getFromUserId, fromUserId)
                .eq(Message::getToUserId, toUserId)
                .eq(Message::getProductId, productId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }
}
