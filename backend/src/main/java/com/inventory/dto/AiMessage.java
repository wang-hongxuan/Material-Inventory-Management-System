package com.inventory.dto;

/**
 * AI 助手上下文消息。
 *
 * <p>role 只接受 user 或 assistant，用于把前端最近几轮对话传给智谱模型，
 * 让助手能根据上下文连续回答。</p>
 */
public record AiMessage(
        String role,
        String content
) {
}
