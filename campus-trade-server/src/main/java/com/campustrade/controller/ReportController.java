package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 举报 Controller
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final SecurityUtil securityUtil;
    private final JdbcTemplate jdbcTemplate;

    /** 提交举报 */
    @PostMapping("/create")
    public R<Void> create(@RequestParam String targetType, @RequestParam Long targetId,
                          @RequestParam String reason, @RequestParam(required = false) String description) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        jdbcTemplate.update("INSERT INTO t_report(reporter_id, target_type, target_id, reason, description, status) VALUES (?, ?, ?, ?, ?, 0)",
                userId, targetType, targetId, reason, description);
        return R.ok();
    }

    /** 管理员查看举报列表 */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        List<Map<String, Object>> list;
        if (status == null) {
            // 不传 status 返回全部
            list = jdbcTemplate.queryForList("SELECT * FROM t_report ORDER BY create_time DESC");
        } else {
            // 0=待处理 1=已处理 2=已驳回
            list = jdbcTemplate.queryForList("SELECT * FROM t_report WHERE status = ? ORDER BY create_time DESC", status);
        }
        return R.ok(list);
    }

    /** 管理员处理举报 */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/handle/{id}")
    public R<Void> handle(@PathVariable Long id, @RequestParam String result,
                          @RequestParam(required = false) String handleMsg) {
        int status = "approve".equals(result) ? 1 : 2;
        jdbcTemplate.update("UPDATE t_report SET status = ?, handle_msg = ?, handle_time = NOW() WHERE id = ?",
                status, handleMsg, id);
        return R.ok();
    }
}
