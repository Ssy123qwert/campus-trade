package com.campustrade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.ProductQuery;
import com.campustrade.dto.ProductSaveDTO;
import com.campustrade.dto.R;
import com.campustrade.entity.Product;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.ProductSearchService;
import com.campustrade.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理 Controller
 *
 * 安全说明：
 * - 发布/修改商品：从 JWT 获取当前用户 ID，不从请求参数接收
 * - 查看商品列表/详情：公开接口（仅需登录）
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;
    private final SecurityUtil securityUtil;

    @PostMapping("/list")
    public R<Page<Product>> list(@Valid @RequestBody ProductQuery query) {
        // 使用 ProductSearchService（ES 优先，自动降级 MySQL LIKE）
        return R.ok(productSearchService.search(query));
    }

    @GetMapping("/detail")
    public R<Product> detail(@RequestParam Long id) {
        productService.addViewCount(id);
        Product product = productService.getById(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        return R.ok(product);
    }

    @PostMapping("/publish")
    public R<Product> publish(@Valid @RequestBody ProductSaveDTO dto) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw BusinessException.unauthorized("请先登录");
        }

        Product product = new Product();
        product.setUserId(currentUserId);           // 从 JWT 获取，拒绝伪造
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCategory(dto.getCategory());
        product.setImages(dto.getImages());
        product.setVideoUrl(dto.getVideoUrl());
        product.setCondition(dto.getCondition());
        product.setStatus(1);
        product.setViewCount(0);
        productService.save(product);
        return R.ok(product);
    }

    @GetMapping("/my")
    public R<List<Product>> myProducts() {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return R.ok(productService.lambdaQuery()
                .eq(Product::getUserId, currentUserId)
                .orderByDesc(Product::getCreateTime)
                .list());
    }

    @PutMapping("/update")
    public R<Product> update(@Valid @RequestBody ProductSaveDTO dto) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        if (dto.getId() == null) {
            throw BusinessException.badRequest("商品ID不能为空");
        }

        // 验证商品属于当前用户
        Product existing = productService.getById(dto.getId());
        if (existing == null) {
            throw BusinessException.notFound("商品不存在");
        }
        if (!existing.getUserId().equals(currentUserId)) {
            throw BusinessException.forbidden("无权修改他人商品");
        }

        Product product = new Product();
        product.setId(dto.getId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCategory(dto.getCategory());
        product.setImages(dto.getImages());
        product.setVideoUrl(dto.getVideoUrl());
        product.setCondition(dto.getCondition());
        productService.updateById(product);
        return R.ok(productService.getById(product.getId()));
    }

    @PutMapping("/offline")
    public R<?> offline(@RequestParam Long id) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) throw BusinessException.unauthorized("请先登录");
        Product product = productService.getById(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        // 只有商品所有者或管理员可以下架
        if (!product.getUserId().equals(currentUserId) && !securityUtil.isAdmin()) {
            throw BusinessException.forbidden("无权操作");
        }
        product.setStatus(3); // 已下架
        productService.updateById(product);
        return R.ok();
    }

    /** 重新上架 */
    @PutMapping("/relist")
    public R<?> relist(@RequestParam Long id) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) throw BusinessException.unauthorized("请先登录");
        Product product = productService.getById(id);
        if (product == null) throw BusinessException.notFound("商品不存在");
        if (!product.getUserId().equals(currentUserId) && !securityUtil.isAdmin()) {
            throw BusinessException.forbidden("无权操作");
        }
        product.setStatus(1); // 重新上架
        productService.updateById(product);
        return R.ok();
    }

    @GetMapping("/categories")
    public R<List<String>> categories() {
        return R.ok(List.of("数码电子", "书籍教材", "生活用品", "服饰鞋包", "运动户外", "其他"));
    }

    /** 相似商品推荐（同分类 + 相近价格） */
    @GetMapping("/similar")
    public R<List<Product>> similar(@RequestParam Long id, @RequestParam(defaultValue = "5") int limit) {
        Product current = productService.getById(id);
        if (current == null) return R.ok(List.of());
        List<Product> list = productService.lambdaQuery()
                .eq(Product::getCategory, current.getCategory())
                .eq(Product::getStatus, 1)
                .ne(Product::getId, id)
                .apply("ABS(price - {0}) < {1} * 0.3", current.getPrice(), current.getPrice())
                .orderByDesc(Product::getViewCount)
                .last("LIMIT " + Math.min(limit, 20))
                .list();
        return R.ok(list);
    }
}
