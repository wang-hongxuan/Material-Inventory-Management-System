package com.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 物资库存管理系统后端启动入口。
 *
 * <p>项目采用 SpringBoot 提供 RESTful API，前端通过 JWT 调用接口，
 * 数据持久化到 MySQL。</p>
 */
@SpringBootApplication
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
