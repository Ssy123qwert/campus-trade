package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Page<Product> queryPage(ProductQuery query) {
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
        return this.page(page, wrapper);
    }

    @Override
    public void addViewCount(Long id) {
        Product product = this.getById(id);
        if (product != null) {
            product.setViewCount(product.getViewCount() == null ? 1 : product.getViewCount() + 1);
            this.updateById(product);
        }
    }
}
