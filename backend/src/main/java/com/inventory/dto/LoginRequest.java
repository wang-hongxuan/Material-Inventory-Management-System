package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求对象。
 */
public record LoginRequest(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码") String password
) {
}
