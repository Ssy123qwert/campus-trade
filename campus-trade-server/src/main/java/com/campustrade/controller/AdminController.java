package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.annotation.OperationLog;
import com.campustrade.dto.R;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.service.ProductService;
import com.campustrade.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台 Controller
 *
 * 所有接口要求 ROLE_ADMIN 权限（已在 SecurityConfig 中统一配置 /api/admin/**）
 * 使用 @PreAuthorize 做二次保障
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // 类级别：所有方法都需要管理员权限
public class AdminController {

    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/check")
    public R<Boolean> check() {
        return R.ok(true);
    }

    // ===== 用户管理 =====

    @GetMapping("/users")
    public R<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                   .or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @OperationLog("删除用户")
    @DeleteMapping("/user")
    public R<String> deleteUser(@RequestParam Long userId) {
        userService.removeById(userId);
        return R.ok("删除成功");
    }

    // ===== 商品管理 =====

    @GetMapping("/products")
    public R<Map<String, Object>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getTitle, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productService.page(new Page<>(page, size), wrapper);
        return R.ok(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @OperationLog("删除商品")
    @DeleteMapping("/product")
    public R<String> deleteProduct(@RequestParam Long productId) {
        productService.removeById(productId);
        return R.ok("删除成功");
    }

    @PutMapping("/product/status")
    public R<String> updateProductStatus(@RequestParam Long productId,
                                          @RequestParam Integer status) {
        Product product = productService.getById(productId);
        if (product == null) return R.fail("商品不存在");
        product.setStatus(status);
        productService.updateById(product);
        return R.ok("更新成功");
    }
}
