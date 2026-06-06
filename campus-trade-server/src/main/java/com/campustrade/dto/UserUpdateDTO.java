package com.campustrade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户信息更新 DTO — 只能修改非敏感字段，防止攻击者修改密码等
 */
@Data
public class UserUpdateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;
    private String phone;
    private String school;
}
