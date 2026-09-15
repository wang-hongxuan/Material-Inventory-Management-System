package com.inventory.dto;

import com.inventory.entity.User;

import java.time.LocalDateTime;

/**
 * 返回给前端的用户视图。
 *
 * <p>不包含密码哈希，避免敏感信息泄露。</p>
 */
public record UserView(
        Long id,
        String username,
        String name,
        String role,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 将用户实体转换为安全的前端展示对象。 */
    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
