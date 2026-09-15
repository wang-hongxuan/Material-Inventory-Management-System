package com.inventory.dto;

/**
 * 登录成功返回对象，包含 JWT 和当前用户信息。
 */
public record AuthResponse(String token, UserView user) {
}
