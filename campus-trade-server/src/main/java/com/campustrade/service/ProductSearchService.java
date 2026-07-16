package com.campustrade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;

/**
 * 商品搜索服务
 *
 * 搜索策略：
 * 1. Elasticsearch 全文检索（IK 分词、高亮、聚合）
 * 2. ES 不可用时自动降级为 MySQL LIKE 查询
 * 3. 搜索结果统一返回 MyBatis-Plus Page<Product>，前端无需感知后端搜索方式
 */
public interface ProductSearchService {

    /**
     * 搜索商品
     * @param query 搜索参数（关键词、分类、价格区间、排序、分页）
     * @return 分页结果
     */
    Page<Product> search(ProductQuery query);

    /**
     * 同步商品到 ES
     * @param product 商品实体
     */
    void syncProduct(Product product);

    /**
     * 从 ES 删除商品
     * @param productId 商品 ID
     */
    void deleteProduct(Long productId);

    /**
     * 全量同步（将 MySQL 所有商品批量写入 ES）
     */
    void syncAll();
}
