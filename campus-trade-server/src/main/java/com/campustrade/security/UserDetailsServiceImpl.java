package com.campustrade.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.entity.User;
import com.campustrade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现
 *
 * 两个加载方式：
 * - loadUserByUsername：登录时根据用户名加载（用于用户名密码认证）
 * - loadUserById：JWT 过滤时根据用户 ID 加载（用于 Token 认证）
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户（Spring Security 标准接口）
     * 用于用户名+密码登录认证
     */
    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return new CustomUserDetails(user);
    }

    /**
     * 根据用户 ID 加载用户（JWT 过滤时使用）
     * 绕过用户名查询，直接查主键，性能更好
     */
    public CustomUserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在, id: " + userId);
        }
        return new CustomUserDetails(user);
    }
}
