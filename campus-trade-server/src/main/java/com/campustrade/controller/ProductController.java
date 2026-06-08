package com.campustrade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.ProductQuery;
import com.campustrade.dto.ProductSaveDTO;
import com.campustrade.dto.R;
import com.campustrade.entity.Product;
import com.campustrade.exception.BusinessException;
import com.campustrade.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/list")
    public R<Page<Product>> list(@Valid @RequestBody ProductQuery query) {
        return R.ok(productService.queryPage(query));
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
        Product product = new Product();
        product.setUserId(dto.getUserId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCategory(dto.getCategory());
        product.setImages(dto.getImages());
        product.setCondition(dto.getCondition());
        product.setStatus(1);
        product.setViewCount(0);
        productService.save(product);
        return R.ok(product);
    }

    @GetMapping("/my")
    public R<List<Product>> myProducts(@RequestParam Long userId) {
        return R.ok(productService.lambdaQuery()
                .eq(Product::getUserId, userId)
                .orderByDesc(Product::getCreateTime)
                .list());
    }

    @PutMapping("/update")
    public R<Product> update(@Valid @RequestBody ProductSaveDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.badRequest("商品ID不能为空");
        }
        // 验证商品属于该用户
        Product existing = productService.getById(dto.getId());
        if (existing == null) {
            throw BusinessException.notFound("商品不存在");
        }
        if (!existing.getUserId().equals(dto.getUserId())) {
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
        Product product = productService.getById(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }
        product.setStatus(3);
        productService.updateById(product);
        return R.ok();
    }

    @GetMapping("/categories")
    public R<List<String>> categories() {
        return R.ok(List.of("数码电子", "书籍教材", "生活用品", "服饰鞋包", "运动户外", "其他"));
    }
}
