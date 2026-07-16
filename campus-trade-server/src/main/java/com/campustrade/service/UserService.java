package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.User;

public interface UserService extends IService<User> {
    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码（明文）
     * @param isBcrypt 密码是否已用 BCrypt 加密（true=BCrypt校验 false=MD5兼容）
     */
    User login(String username, String password, boolean isBcrypt);

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码（明文，自动 BCrypt 加密）
     */
    User register(String username, String password);
}
