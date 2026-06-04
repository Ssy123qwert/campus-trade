package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campustrade.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(User user);
}
