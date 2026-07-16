package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 *
 * Redis 缓存策略：
 * - 商品详情缓存：key=product:detail:{id}，TTL=30分钟
 * - 热门商品列表：key=product:hot:top10，TTL=5分钟（预留）
 * - 浏览量计数器：key=product:view:{id}，每5分钟批量刷库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final StringRedisTemplate redisTemplate;

    @Value("${campustrade.image.base-url:}")
    private String imageBaseUrl;

    /** Redis 商品详情缓存前缀 */
    private static final String CACHE_DETAIL = "product:detail:";
    /** Redis 浏览量计数器前缀 */
    private static final String VIEW_COUNT = "product:view:";
    /** 商品详情缓存 TTL（秒） */
    private static final long CACHE_TTL = 1800;

    // ==================== 查询 ====================

    // getById 使用 ServiceImpl 默认实现（可直接查 DB）
    // 后续可扩展 Redis 缓存 + JSON 序列化

    @Override
    public Page<Product> queryPage(ProductQuery query) {
        if (query == null) {
            throw BusinessException.badRequest("查询参数不能为空");
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1); // 只查在售

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Product::getTitle, query.getKeyword());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Product::getCategory, query.getCategory());
        }
        if (query.getCondition() != null) {
            wrapper.eq(Product::getCondition, query.getCondition());
        }

        // 排序
        if ("price_asc".equals(query.getSort())) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(query.getSort())) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> page = new Page<>(query.getPage(), query.getSize());
        Page<Product> result = this.page(page, wrapper);

        // 补全图片URL
        if (StringUtils.hasText(imageBaseUrl)) {
            for (Product p : result.getRecords()) {
                if (StringUtils.hasText(p.getImages()) && p.getImages().startsWith("[\"/api")) {
                    p.setImages(p.getImages().replace("\"/api/file/image/", "\"" + imageBaseUrl + "/api/file/image/"));
                }
            }
        }
        return result;
    }

    // ==================== 浏览量 ====================

    @Override
    public void addViewCount(Long id) {
        if (id == null || id <= 0) {
            throw BusinessException.badRequest("商品ID不合法");
        }
        // 即时更新 MySQL（用户立即看到变化）
        this.update(new LambdaUpdateWrapper<Product>()
                .setSql("view_count = COALESCE(view_count, 0) + 1")
                .eq(Product::getId, id));
        // 同时写入 Redis 计数器（用于后续统计/热门排行）
        String key = VIEW_COUNT + id;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }

    // ==================== 写入 ====================

    @Override
    public boolean save(Product entity) {
        if (entity.getUserId() == null) {
            throw BusinessException.badRequest("卖家ID不能为空");
        }
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            throw BusinessException.badRequest("商品标题不能为空");
        }
        if (entity.getPrice() == null) {
            throw BusinessException.badRequest("价格不能为空");
        }
        boolean result = super.save(entity);
        if (!result) {
            throw BusinessException.serverError("发布失败");
        }
        // 清除列表缓存
        redisTemplate.delete("product:list:hot");
        return result;
    }

    @Override
    public boolean updateById(Product entity) {
        boolean result = super.updateById(entity);
        // 清除对应的详情缓存
        if (entity.getId() != null) {
            redisTemplate.delete(CACHE_DETAIL + entity.getId());
        }
        return result;
    }

    /**
     * 批量回写 Redis 浏览量到 MySQL（定时任务调用）
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 300_000)
    public void flushViewCounts() {
        var keys = redisTemplate.keys(VIEW_COUNT + "*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr == null) continue;
            try {
                Long productId = Long.parseLong(key.substring(VIEW_COUNT.length()));
                int count = Integer.parseInt(countStr);
                if (count > 0) {
                    // 批量写入 MySQL
                    this.update(new LambdaUpdateWrapper<Product>()
                            .setSql("view_count = COALESCE(view_count, 0) + " + count)
                            .eq(Product::getId, productId));
                    // 重置计数器
                    redisTemplate.delete(key);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
