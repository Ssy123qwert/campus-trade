package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Favorite;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏管理 Controller
 *
 * 安全说明：所有操作从 JWT 获取当前用户 ID
 */
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityUtil securityUtil;

    @PostMapping("/toggle")
    public R<Boolean> toggle(@RequestParam Long productId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(favoriteService.toggle(userId, productId));
    }

    @GetMapping("/my")
    public R<List<Favorite>> myFavorites() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");
        return R.ok(favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime)
                .list());
    }

    @GetMapping("/check")
    public R<Boolean> check(@RequestParam Long productId) {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) return R.ok(false);
        long count = favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)
                .count();
        return R.ok(count > 0);
    }
}
