package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Message;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息管理 Controller
 *
 * 安全说明：
 * - 发送者（fromUserId）从 JWT 获取
 * - 只能查看自己的消息列表和会话
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtil securityUtil;

    @PostMapping("/send")
    public R<Message> send(@RequestParam Long toUserId,
                           @RequestParam Long productId,
                           @RequestParam String content) {
        Long fromUserId = securityUtil.getCurrentUserId();
        if (fromUserId == null) throw BusinessException.unauthorized("请先登录");
        Message msg = messageService.send(fromUserId, toUserId, productId, content);
        return R.ok(msg);
    }

    @GetMapping("/conversation")
    public R<List<Message>> getConversation(@RequestParam Long otherUserId,
                                            @RequestParam Long productId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(messageService.getConversation(userId, otherUserId, productId));
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> getConversationList() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(messageService.getConversationList(userId));
    }

    @GetMapping("/unread")
    public R<Integer> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) return R.ok(0);
        return R.ok(messageService.getUnreadCount(userId));
    }

    @PutMapping("/read")
    public R<?> markAsRead(@RequestParam Long fromUserId,
                           @RequestParam Long productId) {
        Long toUserId = securityUtil.getCurrentUserId();
        if (toUserId == null) throw BusinessException.unauthorized("请先登录");
        messageService.markAsRead(fromUserId, toUserId, productId);
        return R.ok();
    }
}
