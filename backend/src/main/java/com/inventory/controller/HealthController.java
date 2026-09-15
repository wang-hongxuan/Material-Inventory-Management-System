package com.inventory.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 *
 * <p>用于快速确认后端服务是否启动成功。</p>
 */
@RestController
public class HealthController {
    /** 返回服务状态和当前时间。 */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "time", LocalDateTime.now());
    }
}
