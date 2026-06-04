package com.campustrade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.ProductQuery;
import com.campustrade.dto.R;
import com.campustrade.entity.Product;
import com.campustrade.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/list")
    public R<Page<Product>> list(@RequestBody ProductQuery query) {
        return R.ok(productService.queryPage(query));
    }

    @GetMapping("/detail")
    public R<Product> detail(@RequestParam Long id) {
        productService.addViewCount(id);
        Product product = productService.getById(id);
        return R.ok(product);
    }

    @PostMapping("/publish")
    public R<Product> publish(@RequestBody Product product) {
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
    public R<Product> update(@RequestBody Product product) {
        productService.updateById(product);
        return R.ok(productService.getById(product.getId()));
    }

    @PutMapping("/offline")
    public R<?> offline(@RequestParam Long id) {
        Product product = productService.getById(id);
        if (product != null) {
            product.setStatus(3);
            productService.updateById(product);
        }
        return R.ok();
    }

    @GetMapping("/categories")
    public R<List<String>> categories() {
        return R.ok(List.of("数码电子", "书籍教材", "生活用品", "服饰鞋包", "运动户外", "其他"));
    }
}
