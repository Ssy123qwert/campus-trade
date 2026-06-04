package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Message;
import com.campustrade.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/send")
    public R<Message> send(@RequestParam Long fromUserId, @RequestParam Long toUserId,
                           @RequestParam Long productId, @RequestParam String content) {
        Message msg = messageService.send(fromUserId, toUserId, productId, content);
        return R.ok(msg);
    }

    @GetMapping("/conversation")
    public R<List<Message>> getConversation(@RequestParam Long userId, @RequestParam Long otherUserId,
                                            @RequestParam Long productId) {
        return R.ok(messageService.getConversation(userId, otherUserId, productId));
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> getConversationList(@RequestParam Long userId) {
        return R.ok(messageService.getConversationList(userId));
    }

    @GetMapping("/unread")
    public R<Integer> getUnreadCount(@RequestParam Long userId) {
        return R.ok(messageService.getUnreadCount(userId));
    }

    @PutMapping("/read")
    public R<?> markAsRead(@RequestParam Long fromUserId, @RequestParam Long toUserId,
                           @RequestParam Long productId) {
        messageService.markAsRead(fromUserId, toUserId, productId);
        return R.ok();
    }
}
