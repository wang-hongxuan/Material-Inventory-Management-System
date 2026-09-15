package com.inventory.service;

import com.inventory.common.ApiException;
import com.inventory.common.PageResult;
import com.inventory.dto.AuthResponse;
import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.dto.UserRequest;
import com.inventory.dto.UserView;
import com.inventory.entity.User;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统用户业务服务。
 *
 * <p>负责登录校验、JWT 签发、用户分页查询和管理员用户维护。
 * 所有密码均使用 BCrypt 加密后保存。</p>
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** 校验账号、密码和启用状态，成功后返回 JWT 与用户视图。 */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (user.getStatus() != 1 || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new AuthResponse(jwtService.createToken(user), UserView.from(user));
    }

    /** 自助注册普通操作员账号，注册成功后直接签发 JWT。 */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = trim(request.email());
        String name = trim(request.name());
        String password = trim(request.password());
        User user = createUser(username, name, password, "operator", 1);
        return new AuthResponse(jwtService.createToken(user), UserView.from(user));
    }

    /** 分页查询用户，支持用户名和姓名模糊搜索。 */
    public PageResult<UserView> list(String keyword, int page, int pageSize) {
        Page<User> result = userRepository.findAll((root, query, cb) -> {
            String key = trim(keyword);
            if (!StringUtils.hasText(key)) return cb.conjunction();
            String like = "%" + key + "%";
            return cb.or(cb.like(root.get("username"), like), cb.like(root.get("name"), like));
        }, PageRequest.of(page - 1, pageSize, Sort.by("id").ascending()));
        List<UserView> items = result.getContent().stream().map(UserView::from).toList();
        return new PageResult<>(items, result.getTotalElements(), page, pageSize);
    }

    /** 新增用户，校验用户名唯一并加密保存密码。 */
    @Transactional
    public UserView create(UserRequest request) {
        String username = trim(request.username());
        String password = trim(request.password());
        String name = trim(request.name());
        User user = createUser(username, name, password, normalRole(request.role()), normalStatus(request.status()));
        return UserView.from(user);
    }

    /** 统一创建用户，保证自助注册和管理员新增账号使用同一套校验规则。 */
    private User createUser(String username, String name, String password, String role, int status) {
        if (!StringUtils.hasText(username)) {
            throw ApiException.badRequest("账号不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw ApiException.badRequest("姓名不能为空");
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw ApiException.badRequest("密码至少需要 6 位");
        }
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("账号已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(status);
        return userRepository.save(user);
    }

    /** 编辑用户信息；当前登录账号不能被停用。 */
    @Transactional
    public UserView update(Long id, UserRequest request, User currentUser) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("用户不存在"));
        String name = trim(request.name());
        if (!StringUtils.hasText(name)) throw ApiException.badRequest("姓名不能为空");
        int status = normalStatus(request.status());
        if (currentUser.getId().equals(id) && status == 0) throw ApiException.badRequest("不能停用当前登录账号");

        user.setName(name);
        user.setRole(normalRole(request.role()));
        user.setStatus(status);
        if (StringUtils.hasText(request.password())) {
            String password = request.password().trim();
            if (password.length() < 6) throw ApiException.badRequest("密码至少需要 6 位");
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        return UserView.from(userRepository.save(user));
    }

    /** 删除用户；为避免误操作，不允许删除当前登录账号。 */
    @Transactional
    public void delete(Long id, User currentUser) {
        if (currentUser.getId().equals(id)) throw ApiException.badRequest("不能删除当前登录账号");
        if (!userRepository.existsById(id)) throw ApiException.notFound("用户不存在");
        userRepository.deleteById(id);
    }

    private String normalRole(String role) {
        return "admin".equals(role) ? "admin" : "operator";
    }

    private int normalStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
