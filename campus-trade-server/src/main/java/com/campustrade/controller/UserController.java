package com.campustrade.controller;

import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.R;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.service.UserService;
import jakarta.validation.Valid;
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
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        // Service 层已抛异常，到这里一定是登录成功
        Map<String, Object> map = new HashMap<>();
        map.put("token", user.getId().toString());
        map.put("user", user);
        return R.ok(map);
    }

    @PostMapping("/register")
    public R<User> register(@Valid @RequestBody RegisterDTO dto) {
        User user = userService.register(dto.getUsername(), dto.getPassword());
        return R.ok(user);
    }

    @GetMapping("/info")
    public R<User> info(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        user.setPassword(null);
        return R.ok(user);
    }

    @PutMapping("/update")
    public R<User> update(@Valid @RequestBody UserUpdateDTO dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setPhone(dto.getPhone());
        user.setSchool(dto.getSchool());
        boolean updated = userService.updateById(user);
        if (!updated) {
            return R.fail("更新失败");
        }
        User updatedUser = userService.getById(dto.getId());
        if (updatedUser == null) {
            throw BusinessException.notFound("用户不存在");
        }
        updatedUser.setPassword(null);
        return R.ok(updatedUser);
    }
}
