package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * AI 助手聊天请求对象。
 *
 * <p>message 是本次用户输入，history 是可选的最近聊天上下文。</p>
 */
public record AiChatRequest(
        @NotBlank(message = "请输入要咨询的问题")
        String message,
        List<AiMessage> history
) {
}
