package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Favorite;
import com.campustrade.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/toggle")
    public R<Boolean> toggle(@RequestParam Long userId, @RequestParam Long productId) {
        return R.ok(favoriteService.toggle(userId, productId));
    }

    @GetMapping("/my")
    public R<List<Favorite>> myFavorites(@RequestParam Long userId) {
        return R.ok(favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime)
                .list());
    }

    @GetMapping("/check")
    public R<Boolean> check(@RequestParam Long userId, @RequestParam Long productId) {
        long count = favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)
                .count();
        return R.ok(count > 0);
    }
}
