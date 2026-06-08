package com.campustrade.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.campustrade.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI 智能助手控制器
 * 将 DeepSeek API Key 移至配置文件，使用 fastjson2 替代字符串拼接。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Value("${campustrade.ai.api-key:}")
    private String apiKey;

    @Value("${campustrade.ai.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private static final String SYSTEM_PROMPT = "你是校园二手交易平台的AI助手。你的职责是：\n" +
            "1. 为用户提供二手商品选购建议\n" +
            "2. 帮助用户评估商品合理价格\n" +
            "3. 提供二手交易注意事项和安全建议\n" +
            "4. 回答关于校园二手交易的各种问题\n" +
            "请用友好、专业的语气回答，每次回答控制在200字以内。";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @PostMapping("/chat")
    public R<String> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return R.fail("问题不能为空");
        }

        // 检查 API Key 是否配置
        if (apiKey == null || apiKey.isBlank()) {
            return R.fail("AI服务暂未配置，请联系管理员");
        }

        try {
            // 使用 fastjson2 构建请求体
            JSONObject requestBody = JSONObject.of(
                "model", "deepseek-chat",
                "messages", JSONArray.of(
                    JSONObject.of("role", "system", "content", SYSTEM_PROMPT),
                    JSONObject.of("role", "user", "content", question)
                ),
                "max_tokens", 500,
                "temperature", 0.7
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 使用 fastjson2 解析响应
                JSONObject respJson = JSON.parseObject(response.body());
                JSONArray choices = respJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    if (messageObj != null) {
                        String content = messageObj.getString("content");
                        if (content != null && !content.isBlank()) {
                            return R.ok(content.trim());
                        }
                    }
                }
                return R.ok("AI已收到你的问题，但暂时无法解析回复。");
            } else {
                log.warn("AI API 返回错误状态码: {}, body: {}", response.statusCode(), response.body());
                return R.fail("AI服务暂时不可用(" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            log.error("AI服务调用失败", e);
            return R.fail("AI服务调用失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
