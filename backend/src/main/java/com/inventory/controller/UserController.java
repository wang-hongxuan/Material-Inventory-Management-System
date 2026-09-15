package com.inventory.controller;

import com.inventory.common.PageResult;
import com.inventory.dto.UserRequest;
import com.inventory.dto.UserView;
import com.inventory.entity.User;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统用户管理接口。
 *
 * <p>仅管理员可访问，用于维护登录用户、角色和启停状态。
 * 密码在 Service 层加密存储，不会返回给前端。</p>
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 分页查询用户，支持账号和姓名关键字筛选。 */
    @GetMapping
    public PageResult<UserView> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return userService.list(keyword, page(page), pageSize(pageSize));
    }

    /** 新增系统用户。 */
    @PostMapping
    public UserView create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    /** 编辑用户资料、角色、状态或密码。 */
    @PutMapping("/{id}")
    public UserView update(@PathVariable Long id, @Valid @RequestBody UserRequest request,
                           @AuthenticationPrincipal User currentUser) {
        return userService.update(id, request, currentUser);
    }

    /** 删除用户；不能删除当前登录账号。 */
    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        userService.delete(id, currentUser);
        return Map.of("success", true);
    }

    /** 获取当前登录用户的个人信息。 */
    @GetMapping("/profile/current")
    public UserView current(@AuthenticationPrincipal User currentUser) {
        return UserView.from(currentUser);
    }

    private int page(int page) {
        return Math.max(page, 1);
    }

    private int pageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }
}
