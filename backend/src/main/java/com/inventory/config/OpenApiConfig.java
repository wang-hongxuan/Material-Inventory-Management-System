package com.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置。
 *
 * <p>用于生成 RESTful 接口文档，并声明 JWT Bearer 鉴权方式，
 * 方便测试登录后的业务接口。</p>
 */
@Configuration
public class OpenApiConfig {
    /** 配置接口文档标题、版本和全局鉴权方案。 */
    @Bean
    public OpenAPI inventoryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("物资库存管理系统 API")
                        .version("1.0.0")
                        .description("SpringBoot + MySQL 版 RESTful 接口文档"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
