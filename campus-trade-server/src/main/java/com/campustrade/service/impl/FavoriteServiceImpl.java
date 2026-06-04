package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Favorite;
import com.campustrade.mapper.FavoriteMapper;
import com.campustrade.service.FavoriteService;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public boolean toggle(Long userId, Long productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getProductId, productId);
        Favorite favorite = this.getOne(wrapper);
        if (favorite != null) {
            this.removeById(favorite.getId());
            return false; // 取消收藏
        } else {
            favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            this.save(favorite);
            return true; // 已收藏
        }
    }
}
