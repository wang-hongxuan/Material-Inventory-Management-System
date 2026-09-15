package com.inventory.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义 Swagger UI 页面。
 *
 * <p>项目关闭了 springdoc 默认页面，通过该控制器固定暴露 /api/docs，
 * 便于课程验收时直接访问接口文档。</p>
 */
@RestController
public class SwaggerPageController {
    /** 返回嵌入 Swagger UI 的 HTML 页面。 */
    @GetMapping(value = {"/api/docs", "/api/docs/"}, produces = MediaType.TEXT_HTML_VALUE)
    public String docs() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>物资库存管理系统 API</title>
                  <link rel="stylesheet" href="/api/swagger-ui/swagger-ui.css">
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="/api/swagger-ui/swagger-ui-bundle.js"></script>
                  <script src="/api/swagger-ui/swagger-ui-standalone-preset.js"></script>
                  <script>
                    window.onload = function () {
                      SwaggerUIBundle({
                        url: '/v3/api-docs',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        layout: 'StandaloneLayout'
                      });
                    };
                  </script>
                </body>
                </html>
                """;
    }
}
