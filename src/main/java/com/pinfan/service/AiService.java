package com.pinfan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    @Value("${pinfan.ai.deepseek.api-key}")
    private String apiKey;

    @Value("${pinfan.ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${pinfan.ai.deepseek.model}")
    private String model;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }


    public String chat(String prompt) {
        return callChat(prompt, false);
    }

    /**
     * 强制返回 JSON 格式，LLM 保证输出合法 JSON，无需正则提取。
     */
    public String chatJson(String prompt) {
        return callChat(prompt, true);
    }

    private String callChat(String prompt, boolean jsonMode) {
        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        // 构建 headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = baseUrl + "/v1/chat/completions";

        // 发起调用
        long start = System.currentTimeMillis();
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            throw new RuntimeException("AI 服务不可用: " + e.getMessage());
        }
        long cost = System.currentTimeMillis() - start;

        // 解析响应：{choices: [{message: {content: "..."}}], usage: {...}}
        Map respBody = response.getBody();
        if (respBody == null) {
            throw new RuntimeException("AI 返回空响应");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        // 记录 token 用量
        Map<String, Object> usage = (Map<String, Object>) respBody.get("usage");
        log.info("DeepSeek 调用完成 - 耗时 {}ms, token 用量: {}", cost, usage);

        return content;
    }
}