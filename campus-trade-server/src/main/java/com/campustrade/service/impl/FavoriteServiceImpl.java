package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Favorite;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.FavoriteMapper;
import com.campustrade.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 收藏服务实现
 *
 * Redis Set 加速：
 * - 收藏/取消收藏：双写 Redis + MySQL
 * - 检查是否收藏：优先走 Redis（SISMEMBER，O(1)）
 * - 我的收藏列表：Redis 存 ID 列表，MySQL 补全详情
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final StringRedisTemplate redisTemplate;

    /** Redis Set 前缀 */
    private static final String FAV_SET = "user:fav:";

    @Override
    public boolean toggle(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw BusinessException.badRequest("用户ID和商品ID不能为空");
        }

        String key = FAV_SET + userId;
        String member = String.valueOf(productId);

        // 检查 Redis Set 或 MySQL
        Boolean isFaved = redisTemplate.opsForSet().isMember(key, member);
        if (isFaved == null) isFaved = false;
        if (!isFaved) {
            isFaved = this.count(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getProductId, productId)) > 0;
        }

        if (isFaved) {
            // 取消收藏
            redisTemplate.opsForSet().remove(key, member);
            this.remove(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getProductId, productId));
            return false;
        } else {
            // 添加收藏
            redisTemplate.opsForSet().add(key, member);
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            this.save(fav);
            return true;
        }
    }
}
