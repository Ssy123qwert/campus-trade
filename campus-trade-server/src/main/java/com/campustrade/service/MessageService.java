package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageService extends IService<Message> {
    Message send(Long fromUserId, Long toUserId, Long productId, String content);
    List<Message> getConversation(Long userId, Long otherUserId, Long productId);
    List<Map<String, Object>> getConversationList(Long userId);
    int getUnreadCount(Long userId);
    void markAsRead(Long fromUserId, Long toUserId, Long productId);
}
