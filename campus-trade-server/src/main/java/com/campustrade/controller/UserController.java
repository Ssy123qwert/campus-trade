package com.campustrade.controller;

import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.R;
import com.campustrade.entity.User;
import com.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        if (user == null) {
            return R.fail("用户名或密码错误");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("token", user.getId().toString());
        map.put("user", user);
        return R.ok(map);
    }

    @PostMapping("/register")
    public R<User> register(@RequestBody User user) {
        User result = userService.register(user);
        if (result == null) {
            return R.fail("用户名已存在");
        }
        return R.ok(result);
    }

    @GetMapping("/info")
    public R<User> info(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return R.ok(user);
    }

    @PutMapping("/update")
    public R<User> update(@RequestBody User user) {
        userService.updateById(user);
        User updated = userService.getById(user.getId());
        updated.setPassword(null);
        return R.ok(updated);
    }
}
