package com.campustrade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;

public interface ProductService extends IService<Product> {
    Page<Product> queryPage(ProductQuery query);
    void addViewCount(Long id);
}
