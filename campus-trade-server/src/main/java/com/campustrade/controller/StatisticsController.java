package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Order;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.service.OrderService;
import com.campustrade.service.ProductService;
import com.campustrade.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 数据统计 Controller
 *
 * 提供 ECharts 可视化大屏所需数据
 * 所有接口需要 ADMIN 权限
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    /** 概览卡片数据 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userService.count());
        data.put("productCount", productService.count());
        data.put("orderCount", orderService.count());

        // 今日交易额（已完成订单）
        BigDecimal todayRevenue = Optional.ofNullable(
                jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(amount), 0) FROM t_order WHERE status = 3 AND DATE(create_time) = CURDATE()",
                        BigDecimal.class)
        ).orElse(BigDecimal.ZERO);
        data.put("todayRevenue", todayRevenue);

        data.put("todayOrders", Optional.ofNullable(
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM t_order WHERE DATE(create_time) = CURDATE()",
                        Long.class)
        ).orElse(0L));

        return R.ok(data);
    }

    /** 近 N 天用户增长趋势 */
    @GetMapping("/user-trend")
    public R<List<Map<String, Object>>> userTrend(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "SELECT DATE(create_time) AS date, COUNT(*) AS count " +
                        "FROM t_user WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                        "GROUP BY DATE(create_time) ORDER BY date",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", rs.getString("date"));
                    m.put("count", rs.getLong("count"));
                    return m;
                },
                days
        );
        return R.ok(list);
    }

    /** 近 N 天订单趋势 */
    @GetMapping("/order-trend")
    public R<List<Map<String, Object>>> orderTrend(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "SELECT DATE(create_time) AS date, COUNT(*) AS count, " +
                        "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS completed " +
                        "FROM t_order WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                        "GROUP BY DATE(create_time) ORDER BY date",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", rs.getString("date"));
                    m.put("count", rs.getLong("count"));
                    m.put("completed", rs.getLong("completed"));
                    return m;
                },
                days
        );
        return R.ok(list);
    }

    /** 商品分类占比 */
    @GetMapping("/category-distribution")
    public R<List<Map<String, Object>>> categoryDistribution() {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "SELECT category, COUNT(*) AS count FROM t_product WHERE status = 1 GROUP BY category ORDER BY count DESC",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString("category") != null ? rs.getString("category") : "未分类");
                    m.put("value", rs.getLong("count"));
                    return m;
                }
        );
        return R.ok(list);
    }

    /** 热门商品 TOP N */
    @GetMapping("/hot-products")
    public R<List<Map<String, Object>>> hotProducts(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> list = jdbcTemplate.query(
                "SELECT id, title, view_count FROM t_product WHERE status = 1 ORDER BY view_count DESC LIMIT ?",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("title", rs.getString("title"));
                    m.put("viewCount", rs.getInt("view_count"));
                    return m;
                },
                limit
        );
        return R.ok(list);
    }

    /** 操作日志列表 */
    @GetMapping("/operation-logs")
    public R<List<Map<String, Object>>> operationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> list = jdbcTemplate.query(
                "SELECT * FROM t_operation_log ORDER BY create_time DESC LIMIT ? OFFSET ?",
                (rs, row) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("userId", rs.getLong("user_id"));
                    m.put("username", rs.getString("username"));
                    m.put("operation", rs.getString("operation"));
                    m.put("method", rs.getString("method"));
                    m.put("result", rs.getString("result"));
                    m.put("ip", rs.getString("ip"));
                    m.put("createTime", rs.getString("create_time"));
                    return m;
                },
                size, offset
        );
        return R.ok(list);
    }
}
