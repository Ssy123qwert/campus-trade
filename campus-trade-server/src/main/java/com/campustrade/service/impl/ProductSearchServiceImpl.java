package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.service.ProductSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品搜索服务实现
 *
 * 当前使用 MySQL LIKE 查询。
 * 当 Elasticsearch 就绪后，只需增加 ES 分支逻辑，
 * 搜索策略改为 ES 优先 → MySQL 降级即可。
 */
@Slf4j
@Service
public class ProductSearchServiceImpl
        extends ServiceImpl<ProductMapper, Product>
        implements ProductSearchService {

    @Override
    public Page<Product> search(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1); // 默认只在售

        // 关键词 LIKE 匹配（标题 + 描述）
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w
                    .like(Product::getTitle, query.getKeyword())
                    .or()
                    .like(Product::getDescription, query.getKeyword())
            );
        }

        // 分类筛选
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Product::getCategory, query.getCategory());
        }

        // 成色筛选
        if (query.getCondition() != null && query.getCondition() > 0) {
            wrapper.eq(Product::getCondition, query.getCondition());
        }

        // 排序（前端传 time_desc / price_asc / price_desc）
        if ("time_desc".equals(query.getSort()) || "time".equals(query.getSort())) {
            wrapper.orderByDesc(Product::getCreateTime);
        } else if ("price_asc".equals(query.getSort())) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(query.getSort())) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getViewCount);
        }

        int page = query.getPage() != null ? query.getPage() : 1;
        int size = query.getSize() != null ? query.getSize() : 20;
        return this.page(new Page<>(page, size), wrapper);
    }

    // ===== ES 同步接口（预留，ES 就绪后启用） =====

    @Override
    public void syncProduct(Product product) {
        log.debug("ES 未启用，跳过商品同步: {}", product.getId());
    }

    @Override
    public void deleteProduct(Long productId) {
        log.debug("ES 未启用，跳过商品删除: {}", productId);
    }

    @Override
    public void syncAll() {
        log.info("ES 未启用，跳过全量同步");
    }
}
