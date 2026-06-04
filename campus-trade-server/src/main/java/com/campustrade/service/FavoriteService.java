package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.Favorite;

public interface FavoriteService extends IService<Favorite> {
    boolean toggle(Long userId, Long productId);
}
