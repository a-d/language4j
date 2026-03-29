package dev.languagelearning.llm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Manual configuration for OpenAI ChatModel.
 * <p>
 * This configuration is necessary because the OpenAI auto-configuration is excluded
 * in application.yml to prevent startup failures when API keys are not configured.
 * This class creates the OpenAI ChatModel bean conditionally when OPENAI_CHAT_ENABLED=true.
 * <p>
 * Supports OpenAI-compatible APIs (e.g., LM Studio, LocalAI, vLLM) via custom base URL.
 * Uses HTTP/1.1 for compatibility with local servers that may not support HTTP/2.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.chat.enabled", havingValue = "true")
public class OpenAiChatModelConfiguration {

    @Value("${spring.ai.openai.api-key:sk-no-key-required}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4-turbo-preview}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * Creates an OpenAI ChatModel bean configured with the application properties.
     * <p>
     * This bean is named "openAiChatModel" to match the expected name in
     * {@link ChatModelConfiguration}.
     * <p>
     * Uses a custom RestClient with HTTP/1.1 forced for compatibility with
     * local OpenAI-compatible servers like LM Studio.
     *
     * @return configured OpenAiChatModel instance
     */
    @Bean(name = "openAiChatModel")
    public OpenAiChatModel openAiChatModel() {
        log.info("========================================");
        log.info("Creating OpenAI ChatModel");
        log.info("  Base URL: {}", baseUrl);
        log.info("  Model: {}", model);
        log.info("  Temperature: {}", temperature);
        log.info("  API Key: {}...", apiKey.length() > 4 ? apiKey.substring(0, 4) : "****");
        log.info("  Full endpoint will be: {}/v1/chat/completions", baseUrl);
        log.info("  Using HTTP/1.1 for compatibility");
        log.info("========================================");

        // Create custom JDK HttpClient with HTTP/1.1 forced
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Create JDK-based request factory with custom HttpClient
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(5)); // LLM responses can be slow

        // Create RestClient with custom request factory
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

        // Create WebClient.Builder for streaming (uses defaults, but we mainly use RestClient for sync calls)
        WebClient.Builder webClientBuilder = WebClient.builder();

        // Create OpenAiApi with both custom RestClient and WebClient
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey, restClientBuilder, webClientBuilder);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, options);
    }
}
