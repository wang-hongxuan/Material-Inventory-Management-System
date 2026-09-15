package com.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 静态资源配置。
 *
 * <p>将物资照片上传目录映射为 /uploads/**，同时为自定义 Swagger 页面
 * 提供 swagger-ui 静态资源访问路径。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir}")
    private String uploadDir;

    /** 注册上传图片和 Swagger UI 的静态资源映射。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/api/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.17.14/");
    }
}
