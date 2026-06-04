package com.campustrade.controller;

import com.campustrade.dto.R;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final String API_KEY = "sk-27739e888630412bb44d30bd6f3a1094";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @PostMapping("/chat")
    public R<String> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return R.fail("\u95ee\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }

        try {
            String systemPrompt = "\u4f60\u662f\u6821\u56ed\u4e8c\u624b\u4ea4\u6613\u5e73\u53f0\u7684AI\u52a9\u624b\u3002\u4f60\u7684\u804c\u8d23\u662f\uff1a\\n" +
                    "1. \u4e3a\u7528\u6237\u63d0\u4f9b\u4e8c\u624b\u5546\u54c1\u9009\u8d2d\u5efa\u8bae\\n" +
                    "2. \u5e2e\u52a9\u7528\u6237\u8bc4\u4f30\u5546\u54c1\u5408\u7406\u4ef7\u683c\\n" +
                    "3. \u63d0\u4f9b\u4e8c\u624b\u4ea4\u6613\u6ce8\u610f\u4e8b\u9879\u548c\u5b89\u5168\u5efa\u8bae\\n" +
                    "4. \u56de\u7b54\u5173\u4e8e\u6821\u56ed\u4e8c\u624b\u4ea4\u6613\u7684\u5404\u79cd\u95ee\u9898\\n" +
                    "\u8bf7\u7528\u53cb\u597d\u3001\u4e13\u4e1a\u7684\u8bed\u6c14\u56de\u7b54\uff0c\u6bcf\u6b21\u56de\u7b54\u63a7\u5236\u5728200\u5b57\u4ee5\u5185\u3002";

            String requestBody = String.format(
                    "{\"model\":\"deepseek-chat\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":500,\"temperature\":0.7}",
                    systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"),
                    question.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String bodyStr = response.body();
                int contentStart = bodyStr.indexOf("\"content\":\"");
                if (contentStart > 0) {
                    contentStart += 11;
                    int contentEnd = bodyStr.indexOf("\"", contentStart);
                    if (contentEnd > contentStart) {
                        String content = bodyStr.substring(contentStart, contentEnd)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"");
                        return R.ok(content);
                    }
                }
                return R.ok("AI\u5df2\u6536\u5230\u4f60\u7684\u95ee\u9898\uff0c\u4f46\u6682\u65f6\u65e0\u6cd5\u89e3\u6790\u56de\u590d\u3002");
            } else {
                return R.fail("AI\u670d\u52a1\u8fd4\u56de\u9519\u8bef\uff1a" + response.statusCode());
            }
        } catch (Exception e) {
            return R.fail("AI\u670d\u52a1\u8c03\u7528\u5931\u8d25\uff1a" + e.getMessage());
        }
    }
}
