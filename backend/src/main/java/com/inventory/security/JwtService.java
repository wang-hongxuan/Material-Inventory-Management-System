package com.inventory.security;

import com.inventory.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具服务。
 *
 * <p>负责生成登录令牌和解析令牌中的用户 ID。令牌有效期为 8 小时，
 * 前端每次请求通过 Authorization: Bearer token 传递。</p>
 */
@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${app.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(sha256(secret));
    }

    /** 根据登录用户生成包含账号、姓名和角色信息的 JWT。 */
    public String createToken(User user) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + Duration.ofHours(8).toMillis());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expires)
                .signWith(key)
                .compact();
    }

    /** 校验并解析 JWT，返回当前登录用户 ID。 */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /** 将配置中的密钥扩展为 HMAC-SHA 所需长度，避免弱密钥报错。 */
    private byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", error);
        }
    }
}
