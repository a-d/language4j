package dev.languagelearning.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic controller for testing connectivity to external services.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostic")
public class DiagnosticController {

    @Value("${spring.ai.openai.base-url:not-configured}")
    private String openaiBaseUrl;

    @Value("${spring.ai.openai.api-key:not-configured}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.chat.options.model:not-configured}")
    private String model;

    /**
     * Test raw HTTP connection to the configured OpenAI base URL.
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseUrl", openaiBaseUrl);
        result.put("model", model);
        result.put("apiKeyPrefix", openaiApiKey.length() > 4 ? openaiApiKey.substring(0, 4) + "..." : "****");

        String modelsUrl = openaiBaseUrl + "/v1/models";
        result.put("testUrl", modelsUrl);

        log.info("Testing connection to: {}", modelsUrl);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .version(HttpClient.Version.HTTP_1_1) // Force HTTP/1.1
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            log.info("Sending request...");
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Response received in {} ms", duration);
            result.put("statusCode", response.statusCode());
            result.put("durationMs", duration);
            result.put("responseBody", response.body().length() > 500 
                    ? response.body().substring(0, 500) + "..." 
                    : response.body());
            result.put("success", true);

        } catch (Exception e) {
            log.error("Connection test failed", e);
            result.put("success", false);
            result.put("error", e.getClass().getName());
            result.put("errorMessage", e.getMessage());
            if (e.getCause() != null) {
                result.put("rootCause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Test a simple chat completion request.
     */
    @GetMapping("/test-chat")
    public ResponseEntity<Map<String, Object>> testChat() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseUrl", openaiBaseUrl);
        result.put("model", model);

        String chatUrl = openaiBaseUrl + "/v1/chat/completions";
        result.put("testUrl", chatUrl);

        String requestBody = """
                {
                    "model": "%s",
                    "messages": [{"role": "user", "content": "Say hello in exactly 3 words"}],
                    "max_tokens": 20
                }
                """.formatted(model);

        log.info("Testing chat completion at: {}", chatUrl);
        log.info("Request body: {}", requestBody);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .version(HttpClient.Version.HTTP_1_1) // Force HTTP/1.1
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatUrl))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Sending chat request...");
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Chat response received in {} ms", duration);
            result.put("statusCode", response.statusCode());
            result.put("durationMs", duration);
            result.put("responseBody", response.body());
            result.put("success", response.statusCode() == 200);

        } catch (Exception e) {
            log.error("Chat test failed", e);
            result.put("success", false);
            result.put("error", e.getClass().getName());
            result.put("errorMessage", e.getMessage());
            if (e.getCause() != null) {
                result.put("rootCause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
        }

        return ResponseEntity.ok(result);
    }
}