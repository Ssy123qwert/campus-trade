package com.campustrade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.dto.R;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.service.ProductService;
import com.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;

    // 检查是否为管理员
    private boolean isAdmin(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Long userId = Long.parseLong(token);
            User user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/check")
    public R<Boolean> check(@RequestHeader(value = "Authorization", required = false) String token) {
        return R.ok(isAdmin(token));
    }

    // ===== 用户管理 =====

    @GetMapping("/users")
    public R<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (!isAdmin(token)) return R.fail("无权限");
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @DeleteMapping("/user")
    public R<String> deleteUser(@RequestParam Long userId,
                                 @RequestHeader(value = "Authorization", required = false) String token) {
        if (!isAdmin(token)) return R.fail("无权限");
        userService.removeById(userId);
        return R.ok("删除成功");
    }

    // ===== 商品管理 =====

    @GetMapping("/products")
    public R<Map<String, Object>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (!isAdmin(token)) return R.fail("无权限");
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getTitle, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productService.page(new Page<>(page, size), wrapper);
        return R.ok(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @DeleteMapping("/product")
    public R<String> deleteProduct(@RequestParam Long productId,
                                    @RequestHeader(value = "Authorization", required = false) String token) {
        if (!isAdmin(token)) return R.fail("无权限");
        productService.removeById(productId);
        return R.ok("删除成功");
    }

    @PutMapping("/product/status")
    public R<String> updateProductStatus(@RequestParam Long productId, @RequestParam Integer status,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        if (!isAdmin(token)) return R.fail("无权限");
        Product product = productService.getById(productId);
        if (product == null) return R.fail("商品不存在");
        product.setStatus(status);
        productService.updateById(product);
        return R.ok("更新成功");
    }
}
