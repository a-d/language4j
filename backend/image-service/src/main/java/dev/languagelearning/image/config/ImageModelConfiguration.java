package dev.languagelearning.image.config;

import dev.languagelearning.config.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the OpenAI Image Model (DALL-E).
 * <p>
 * Creates the OpenAI Image API client and model that can work with:
 * <ul>
 *   <li>OpenAI's official API (default)</li>
 *   <li>Custom OpenAI-compatible image generation endpoints</li>
 * </ul>
 * <p>
 * To enable image generation:
 * <pre>
 * OPENAI_IMAGE_ENABLED=true
 * IMAGE_API_KEY=your-api-key  # or uses LLM_API_KEY
 * IMAGE_BASE_URL=https://api.openai.com  # optional custom endpoint
 * </pre>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.image.enabled", havingValue = "true")
public class ImageModelConfiguration {

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";
    private static final String DUMMY_API_KEY = "local-service-no-key-required";

    /**
     * Creates the OpenAI Image API client.
     *
     * @param aiConfig the AI provider configuration
     * @return the OpenAI Image API client
     */
    @Bean
    public OpenAiImageApi openAiImageApi(AiProviderConfig aiConfig) {
        var imageConfig = aiConfig.getImage();

        String apiKey = resolveApiKey(imageConfig, aiConfig);
        String baseUrl = resolveBaseUrl(imageConfig);

        log.info("Configuring OpenAI Image API with base URL: {}", baseUrl);

        RestClient.Builder restClientBuilder = RestClient.builder();
        return new OpenAiImageApi(baseUrl, apiKey, restClientBuilder);
    }

    /**
     * Creates the OpenAI Image Model for image generation.
     *
     * @param openAiImageApi the OpenAI Image API client
     * @return the image model
     */
    @Bean
    public OpenAiImageModel openAiImageModel(OpenAiImageApi openAiImageApi) {
        log.info("Creating OpenAI Image Model for image generation (DALL-E)");
        return new OpenAiImageModel(openAiImageApi);
    }

    private String resolveApiKey(AiProviderConfig.ImageConfig imageConfig, AiProviderConfig aiConfig) {
        if (imageConfig != null && imageConfig.getApiKey().isPresent()) {
            return imageConfig.getApiKey().get();
        }
        // Fall back to LLM API key
        return aiConfig.getLlm().getApiKey().orElse(DUMMY_API_KEY);
    }

    private String resolveBaseUrl(AiProviderConfig.ImageConfig imageConfig) {
        if (imageConfig != null && imageConfig.getBaseUrl().isPresent()) {
            return imageConfig.getBaseUrl().get();
        }
        return DEFAULT_OPENAI_BASE_URL;
    }
}