package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Notification;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final SecurityUtil securityUtil;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public R<List<Notification>> list() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        List<Notification> list = jdbcTemplate.query(
                "SELECT * FROM t_notification WHERE user_id = ? ORDER BY create_time DESC LIMIT 50",
                (rs, row) -> {
                    Notification n = new Notification();
                    n.setId(rs.getLong("id"));
                    n.setUserId(rs.getLong("user_id"));
                    n.setType(rs.getString("type"));
                    n.setTitle(rs.getString("title"));
                    n.setContent(rs.getString("content"));
                    n.setRelatedId(rs.getLong("related_id"));
                    n.setIsRead(rs.getInt("is_read"));
                    n.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                    return n;
                }, userId);
        return R.ok(list);
    }

    @GetMapping("/unread")
    public R<Integer> unread() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) return R.ok(0);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = ? AND is_read = 0",
                Integer.class, userId);
        return R.ok(count != null ? count : 0);
    }

    @PutMapping("/read/{id}")
    public R<Void> markRead(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE t_notification SET is_read = 1 WHERE id = ?", id);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        jdbcTemplate.update("UPDATE t_notification SET is_read = 1 WHERE user_id = ?", userId);
        return R.ok();
    }
}
