package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户新增和编辑请求对象。
 *
 * <p>新增时密码必填，编辑时密码可留空表示不修改。</p>
 */
public record UserRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "姓名不能为空") String name,
        String password,
        String role,
        Integer status
) {
}
