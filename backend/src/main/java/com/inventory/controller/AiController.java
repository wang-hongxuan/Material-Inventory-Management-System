package com.inventory.controller;

import com.inventory.dto.AiChatRequest;
import com.inventory.dto.AiChatResponse;
import com.inventory.entity.User;
import com.inventory.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收前端请求，提供 RESTful API，AI 助手接口。
 *
 * <p>接口需要登录后访问，由后端转发到智谱 API，避免前端泄露第三方 API Key。</p>
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    /** 与库存系统 AI 助手对话。 */
    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request,
                               @AuthenticationPrincipal User currentUser) {
        return aiAssistantService.chat(request, currentUser);
    }
}
