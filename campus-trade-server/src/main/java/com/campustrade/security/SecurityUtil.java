package com.campustrade.security;

import com.campustrade.entity.User;
import com.campustrade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类
 * 方便 Controller/Service 获取当前登录用户信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserMapper userMapper;

    /**
     * 获取当前登录用户 ID
     * 如果没有认证用户则返回 null
     */
    public Long getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getId() : null;
    }

    /**
     * 获取当前登录用户的完整 User 实体
     * 如果用户未登录或不存在则返回 null
     */
    public User getCurrentUserEntity() {
        Long userId = getCurrentUserId();
        if (userId == null) return null;
        return userMapper.selectById(userId);
    }

    /**
     * 判断当前用户是否为管理员
     */
    public boolean isAdmin() {
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null && userDetails.getRole() != null && userDetails.getRole() == 1;
    }

    /**
     * 从 SecurityContextHolder 获取当前用户
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
