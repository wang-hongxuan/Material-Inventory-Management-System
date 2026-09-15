package com.inventory.controller;

import com.inventory.dto.AuthResponse;
import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.UserView;
import com.inventory.entity.User;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录认证接口。
 *
 * <p>提供用户登录和当前登录用户查询能力，登录成功后返回 JWT，
 * 前端后续请求都通过该令牌访问受保护接口。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 用户登录，校验账号密码并返回 JWT。 */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /** 用户自助注册，创建普通操作员账号并直接返回 JWT。 */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /** 查询当前 token 对应的用户信息。 */
    @GetMapping("/me")
    public Map<String, UserView> me(@AuthenticationPrincipal User currentUser) {
        return Map.of("user", UserView.from(currentUser));
    }
}
