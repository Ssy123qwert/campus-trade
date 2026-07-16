package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offer")
@RequiredArgsConstructor
public class OfferController {

    private final SecurityUtil securityUtil;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/create")
    public R<Void> create(@RequestParam Long productId, @RequestParam BigDecimal price) {
        Long buyerId = securityUtil.getCurrentUserId();
        if (buyerId == null) throw BusinessException.unauthorized("请先登录");

        Map<String, Object> product = jdbcTemplate.queryForMap(
                "SELECT user_id, status FROM t_product WHERE id = ?", productId);
        Long sellerId = ((Number) product.get("user_id")).longValue();
        int status = ((Number) product.get("status")).intValue();
        if (sellerId.equals(buyerId)) throw BusinessException.badRequest("不能对自己的商品出价");
        if (status != 1) throw BusinessException.badRequest("该商品已下架或已售出");

        // 检查是否有待回复的出价
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id FROM t_offer WHERE product_id = ? AND buyer_id = ? AND status = 0 LIMIT 1",
                productId, buyerId);
        if (!existing.isEmpty()) throw BusinessException.badRequest("您已有待回复的出价");

        jdbcTemplate.update("INSERT INTO t_offer(product_id, buyer_id, seller_id, price, status) VALUES (?, ?, ?, ?, 0)",
                productId, buyerId, sellerId, price);
        return R.ok();
    }

    @GetMapping("/received")
    public R<List<Map<String, Object>>> received() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT o.*, p.title as product_title FROM t_offer o " +
                "JOIN t_product p ON o.product_id = p.id WHERE o.seller_id = ? ORDER BY o.create_time DESC", userId);
        return R.ok(list);
    }

    @GetMapping("/my")
    public R<List<Map<String, Object>>> my() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT o.*, p.title as product_title FROM t_offer o " +
                "JOIN t_product p ON o.product_id = p.id WHERE o.buyer_id = ? ORDER BY o.create_time DESC", userId);
        return R.ok(list);
    }

    @PostMapping("/accept/{id}")
    public R<Void> accept(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        // 验证是卖家
        Map<String, Object> offer = jdbcTemplate.queryForMap(
                "SELECT seller_id, status FROM t_offer WHERE id = ?", id);
        if (!Long.valueOf(((Number) offer.get("seller_id")).longValue()).equals(userId)) {
            throw BusinessException.forbidden("无权操作");
        }
        if (((Number) offer.get("status")).intValue() != 0) {
            throw BusinessException.badRequest("该出价已处理");
        }
        jdbcTemplate.update("UPDATE t_offer SET status = 1 WHERE id = ? AND status = 0", id);
        return R.ok();
    }

    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        Map<String, Object> offer = jdbcTemplate.queryForMap(
                "SELECT seller_id, status FROM t_offer WHERE id = ?", id);
        if (!Long.valueOf(((Number) offer.get("seller_id")).longValue()).equals(userId)) {
            throw BusinessException.forbidden("无权操作");
        }
        if (((Number) offer.get("status")).intValue() != 0) {
            throw BusinessException.badRequest("该出价已处理");
        }
        jdbcTemplate.update("UPDATE t_offer SET status = 2 WHERE id = ? AND status = 0", id);
        return R.ok();
    }
}
