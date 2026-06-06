package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Favorite;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.FavoriteMapper;
import com.campustrade.service.FavoriteService;
import org.springframework.stereotype.Service;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public boolean toggle(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw BusinessException.badRequest("用户ID和商品ID不能为空");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getProductId, productId);
        Favorite favorite = this.getOne(wrapper);

        if (favorite != null) {
            boolean removed = this.removeById(favorite.getId());
            if (!removed) {
                throw BusinessException.serverError("取消收藏失败");
            }
            return false; // 已取消收藏
        } else {
            favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            boolean saved = this.save(favorite);
            if (!saved) {
                throw BusinessException.serverError("添加收藏失败");
            }
            return true; // 已收藏
        }
    }
}
