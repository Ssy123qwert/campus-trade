package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Message;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
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
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setProductId(productId);
        message.setContent(content);
        message.setIsRead(0);
        this.save(message);
        return message;
    }

    @Override
    public List<Message> getConversation(Long userId, Long otherUserId, Long productId) {
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
        // 获取所有与userId相关的消息，按 from+to+productId 分组取最新一条
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Message::getFromUserId, userId).or().eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime);
        List<Message> messages = this.list(wrapper);

        // 按会话分组
        Map<String, Message> convMap = new LinkedHashMap<>();
        for (Message msg : messages) {
            Long other = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();
            String key = other + "_" + msg.getProductId();
            if (!convMap.containsKey(key)) {
                convMap.put(key, msg);
            }
        }

        // 构建返回数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message msg : convMap.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            Long otherId = msg.getFromUserId().equals(userId) ? msg.getToUserId() : msg.getFromUserId();

            User otherUser = userService.getById(otherId);
            Product product = productService.getById(msg.getProductId());

            item.put("otherUserId", otherId);
            item.put("otherUserName", otherUser != null ? otherUser.getNickname() : "未知用户");
            item.put("otherAvatar", otherUser != null ? otherUser.getAvatar() : "");
            item.put("productId", msg.getProductId());
            item.put("productTitle", product != null ? product.getTitle() : "未知商品");
            item.put("lastMessage", msg.getContent());
            item.put("lastTime", msg.getCreateTime());

            // 该会话未读数量
            int unread = (int) this.count(new LambdaQueryWrapper<Message>()
                    .eq(Message::getFromUserId, otherId)
                    .eq(Message::getToUserId, userId)
                    .eq(Message::getProductId, msg.getProductId())
                    .eq(Message::getIsRead, 0));
            item.put("unreadCount", unread);

            result.add(item);
        }
        return result;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return (int) this.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public void markAsRead(Long fromUserId, Long toUserId, Long productId) {
        List<Message> unreadMessages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getFromUserId, fromUserId)
                .eq(Message::getToUserId, toUserId)
                .eq(Message::getProductId, productId)
                .eq(Message::getIsRead, 0));
        for (Message msg : unreadMessages) {
            msg.setIsRead(1);
            this.updateById(msg);
        }
    }
}
