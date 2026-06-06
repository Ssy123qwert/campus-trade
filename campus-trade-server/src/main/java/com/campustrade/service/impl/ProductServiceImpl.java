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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Value("${campustrade.image.base-url:}")
    private String imageBaseUrl;

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

    @Override
    public void addViewCount(Long id) {
        if (id == null || id <= 0) {
            throw BusinessException.badRequest("商品ID不合法");
        }
        // 使用原子 SQL 更新，避免读-改-写竞态条件
        this.update(new LambdaUpdateWrapper<Product>()
                .setSql("view_count = COALESCE(view_count, 0) + 1")
                .eq(Product::getId, id));
    }

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
        return result;
    }
}
