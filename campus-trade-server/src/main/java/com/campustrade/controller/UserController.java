package com.campustrade.controller;

import com.campustrade.annotation.RateLimit;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.R;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.security.JwtUtil;
import com.campustrade.security.SecurityUtil;
import com.campustrade.service.OrderService;
import com.campustrade.service.ProductService;
import com.campustrade.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户认证与信息管理 Controller
 *
 * 认证方式：JWT（Access Token + Refresh Token）
 * - Access Token：有效期 2h，放在 Authorization: Bearer xxx 请求头
 * - Refresh Token：有效期 7d，用于换取新 Access Token
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final JwtUtil jwtUtil;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;

    @RateLimit(key = "login", max = 10, window = 60)  // 每分钟最多 10 次
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        // 1. 先查用户（只查一次）
        User user = userService.getByUsername(dto.getUsername());
        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 2. 根据密码格式选择验证方式
        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword.startsWith("$2")) {
            // BCrypt 格式（新用户）
            matched = passwordEncoder.matches(dto.getPassword(), storedPassword);
        } else {
            // MD5 格式（老用户兼容）
            matched = cn.hutool.crypto.digest.DigestUtil.md5Hex(dto.getPassword()).equals(storedPassword);
            if (matched) {
                // 自动升级为 BCrypt，对用户透明
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                userService.updateById(user);
            }
        }
        if (!matched) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 3. 生成 JWT Token 对
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole());

        Map<String, Object> map = new HashMap<>();
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        user.setPassword(null);
        map.put("user", user);
        return R.ok(map);
    }

    @RateLimit(key = "register", max = 5, window = 60)
    @PostMapping("/register")
    public R<User> register(@Valid @RequestBody RegisterDTO dto) {
        User user = userService.register(dto.getUsername(), dto.getPassword());
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 用 Refresh Token 换取新的 Access Token
     */
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail("refreshToken 不能为空");
        }

        JwtUtil.TokenPair tokenPair = jwtUtil.refreshAccessToken(refreshToken);
        if (tokenPair == null) {
            return R.fail("Refresh Token 无效或已过期，请重新登录");
        }

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", tokenPair.getAccessToken());
        map.put("refreshToken", tokenPair.getRefreshToken());
        return R.ok(map);
    }

    /**
     * 登出：将当前 Token 加入黑名单，清除 Refresh Token
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // 从请求头提取 Token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtUtil.blacklistToken(token);
        }

        // 清除 Refresh Token
        Long userId = securityUtil.getCurrentUserId();
        if (userId != null) {
            jwtUtil.removeRefreshToken(userId);
        }

        return R.ok();
    }

    /**
     * 获取当前登录用户信息
     * 不再需要手动传 userId，从 SecurityContext 获取
     */
    @GetMapping("/info")
    public R<User> info() {
        User user = securityUtil.getCurrentUserEntity();
        if (user == null) {
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 更新当前登录用户信息
     * userId 从 SecurityContext 获取，不允许修改他人信息
     */
    @PutMapping("/update")
    public R<User> update(@Valid @RequestBody UserUpdateDTO dto) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw BusinessException.unauthorized("未登录或登录已过期");
        }

        User user = new User();
        user.setId(currentUserId);           // 强制使用当前登录用户 ID，拒绝越权
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setPhone(dto.getPhone());
        user.setSchool(dto.getSchool());
        boolean updated = userService.updateById(user);
        if (!updated) {
            return R.fail("更新失败");
        }
        User updatedUser = userService.getById(currentUserId);
        updatedUser.setPassword(null);
        return R.ok(updatedUser);
    }

    /** 公开用户信息（查看他人资料） */
    @GetMapping("/public/{userId}")
    public R<User> publicInfo(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) throw BusinessException.notFound("用户不存在");
        user.setPassword(null);
        return R.ok(user);
    }

    /** 个人主页统计数据 */
    @GetMapping("/profile-stats")
    public R<Map<String, Object>> profileStats() {
        Long userId = securityUtil.getCurrentUserId();
        if (userId == null) throw BusinessException.unauthorized("请先登录");

        Map<String, Object> stats = new HashMap<>();
        // 发布的商品数
        stats.put("productCount", productService.lambdaQuery()
                .eq(com.campustrade.entity.Product::getUserId, userId).count());
        // 卖出数（作为卖家的已完成订单）
        stats.put("soldCount", orderService.lambdaQuery()
                .eq(com.campustrade.entity.Order::getSellerId, userId)
                .eq(com.campustrade.entity.Order::getStatus, 3).count());
        // 买入数（作为买家的已完成订单）
        stats.put("boughtCount", orderService.lambdaQuery()
                .eq(com.campustrade.entity.Order::getBuyerId, userId)
                .eq(com.campustrade.entity.Order::getStatus, 3).count());
        return R.ok(stats);
    }
}
