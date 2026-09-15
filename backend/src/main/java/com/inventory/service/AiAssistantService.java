package com.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.common.ApiException;
import com.inventory.dto.AiChatRequest;
import com.inventory.dto.AiChatResponse;
import com.inventory.dto.AiMessage;
import com.inventory.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI 助手服务。
 *
 * <p>后端统一调用智谱 API，前端只调用本系统接口，避免把 API Key 暴露到浏览器。
 * API Key 优先读取环境变量，也支持读取本地 .env.local 文件，避免硬编码到源码。</p>
 */
@Service
public class AiAssistantService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiInventoryContextService inventoryContextService;
    private final String apiKey;
    private final String endpoint;
    private final String model;

    public AiAssistantService(
            ObjectMapper objectMapper,
            AiInventoryContextService inventoryContextService,
            @Value("${app.ai.zhipu-api-key:}") String apiKey,
            @Value("${app.ai.zhipu-endpoint:https://open.bigmodel.cn/api/paas/v4/chat/completions}") String endpoint,
            @Value("${app.ai.zhipu-model:glm-4-flash}") String model
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.inventoryContextService = inventoryContextService;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
    }

    /** 调用智谱模型生成回答。 */
    public AiChatResponse chat(AiChatRequest request, User currentUser) {
        String resolvedApiKey = resolveApiKey();
        if (!StringUtils.hasText(resolvedApiKey)) {
            throw ApiException.badRequest("AI 助手未配置密钥，请在 backend/.env.local 中填写 ZHIPU_API_KEY");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt(currentUser)));
        messages.add(message("system", inventoryContextService.buildContext(request.message(), currentUser)));
        messages.addAll(historyMessages(request.history()));
        messages.add(message("user", request.message().trim()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.6);
        body.put("top_p", 0.8);

        try {
            String response = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resolvedApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return new AiChatResponse(parseReply(response));
        } catch (RestClientException ex) {
            throw ApiException.badRequest("AI 助手调用失败，请检查智谱 API Key、网络或模型配置");
        }
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(apiKey)) return apiKey.trim();
        return readLocalApiKey(Path.of(".env.local"))
                .or(() -> readLocalApiKey(Path.of("backend", ".env.local")))
                .orElse("");
    }

    private Optional<String> readLocalApiKey(Path path) {
        if (!Files.exists(path)) return Optional.empty();
        try {
            return Files.readAllLines(path).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("ZHIPU_API_KEY="))
                    .map(line -> line.substring("ZHIPU_API_KEY=".length()).trim())
                    .map(this::unquote)
                    .filter(StringUtils::hasText)
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String parseReply(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (StringUtils.hasText(content)) return content.trim();
        } catch (Exception ignored) {
            // 统一走下面的业务提示，避免把第三方响应细节暴露给前端。
        }
        throw ApiException.badRequest("AI 助手返回内容为空，请稍后重试");
    }

    private List<Map<String, String>> historyMessages(List<AiMessage> history) {
        if (history == null || history.isEmpty()) return List.of();
        return history.stream()
                .filter(item -> item != null && StringUtils.hasText(item.content()))
                .filter(item -> "user".equals(item.role()) || "assistant".equals(item.role()))
                .skip(Math.max(0, history.size() - 8L))
                .map(item -> message(item.role(), item.content().trim()))
                .toList();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("role", role);
        item.put("content", content);
        return item;
    }

    private String systemPrompt(User currentUser) {
        String role = currentUser == null ? "用户" : ("admin".equals(currentUser.getRole()) ? "管理员" : "操作员");
        return """
                你是系统内置的 AI 助手，当前使用者角色是：%s。
                你可以回答用户提出的大多数问题，包括系统使用、课程答辩、学习问题、技术问题、写作表达、日常知识和一般咨询。
                如果问题和物资库存管理系统有关，请优先结合本系统功能回答：
                1. 管理员可以管理用户、管理物资、登记入库、直接出库、审批操作员的出库申请、查看报表。
                2. 操作员可以自助注册、管理基础业务数据、提交出库申请；操作员提交出库后库存不变，管理员审批通过后才扣减库存。
                3. 管理员自己登记出库时不需要审批，系统会直接扣减库存。
                4. 入库会增加库存；出库会减少库存；库存不足时系统会拦截，避免负库存。
                5. 低库存预警表示当前库存小于或等于安全库存，需要及时补货。
                6. 库存报表通过出入库记录和物资库存统计生成，包含库存总览、分类库存、出入库趋势、低库存预警等。
                7. 用户询问当前库存、某个物资库存、低库存、待审批出库、最近出入库等实时业务数据时，必须使用后续“实时库存数据上下文”中的数据库查询结果回答；如果上下文没有对应物资或记录，不要编造数据，应提示用户到对应页面进一步筛选查询。
                如果问题涉及医学、法律、金融、安全、隐私、账号密钥等高风险内容，请提醒用户谨慎，并给出安全、合规的建议。
                回答使用中文，表达清楚，尽量简洁实用；不要编造你不知道的事实，不确定时说明不确定。
                """.formatted(role);
    }
}
