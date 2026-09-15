package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户自助注册请求对象。
 *
 * <p>注册账号默认使用 email 作为登录账号，角色固定为普通操作员，
 * 管理员账号仍由管理员在用户管理中创建。</p>
 */
public record RegisterRequest(
        @NotBlank(message = "Name 不能为空") String name,
        @NotBlank(message = "Email 不能为空") String email,
        @NotBlank(message = "Password 不能为空") String password
) {
}
