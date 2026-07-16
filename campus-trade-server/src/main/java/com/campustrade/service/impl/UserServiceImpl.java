package com.campustrade.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.mapper.UserMapper;
import com.campustrade.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 *
 * 密码加密升级说明：
 * - 新用户：BCrypt 加密
 * - 老用户（MD5）：首次登录时自动升级为 BCrypt，对用户透明
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public User getByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public User login(String username, String password, boolean isBcrypt) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);

        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 密码校验：支持 BCrypt（新用户）和 MD5（老用户兼容）
        boolean passwordMatch;
        if (isBcrypt) {
            // 新用户：BCrypt 校验
            passwordMatch = passwordEncoder.matches(password, user.getPassword());
        } else {
            // 老用户：MD5 校验（兼容历史数据）
            passwordMatch = DigestUtil.md5Hex(password).equals(user.getPassword());
        }

        if (!passwordMatch) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 如果老用户用 MD5 登录成功，自动升级为 BCrypt
        if (!isBcrypt && passwordMatch) {
            user.setPassword(passwordEncoder.encode(password));
            this.updateById(user);
        }

        return user;
    }

    @Override
    @Transactional
    public User register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }
        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count > 0) {
            throw BusinessException.conflict("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt 加密
        user.setNickname(username);
        user.setRole(0); // 新注册用户默认为普通用户
        boolean saved = this.save(user);
        if (!saved) {
            throw BusinessException.serverError("注册失败，请稍后重试");
        }
        return user;
    }
}
