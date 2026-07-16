package com.campustrade.security;

import com.campustrade.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用户信息适配器
 * 将项目中的 User 实体包装为 UserDetails，供 Security 框架使用
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Integer role;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();

        // 根据角色设置权限：管理员有 ROLE_ADMIN，普通用户有 ROLE_USER
        String roleName = (user.getRole() != null && user.getRole() == 1) ? "ROLE_ADMIN" : "ROLE_USER";
        this.authorities = List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
