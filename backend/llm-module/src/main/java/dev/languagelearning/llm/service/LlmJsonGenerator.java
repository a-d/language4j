package dev.languagelearning.llm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.exception.LlmException;
import dev.languagelearning.llm.util.JsonSanitizer;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Service for generating validated JSON content from LLM.
 * <p>
 * Provides automatic retry capability when JSON parsing fails, ensuring
 * that returned content is always valid, parseable JSON.
 * <p>
 * Features:
 * <ul>
 *   <li>Automatic sanitization of LLM output</li>
 *   <li>Configurable retry count</li>
 *   <li>Validation before returning</li>
 *   <li>Logging of retry attempts</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * String json = llmJsonGenerator.generateJson(
 *     MyPrompts.VOCABULARY_PROMPT,
 *     Map.of("topic", "food", "count", 10)
 * );
 * }</pre>
 */
@Slf4j
@Service
public class LlmJsonGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmService llmService;
    private final int maxRetries;
    private final int retryDelayMs;

    /**
     * Creates a new LlmJsonGenerator.
     *
     * @param llmService the underlying LLM service
     * @param maxRetries maximum number of retry attempts (default: 3)
     * @param retryDelayMs delay between retries in milliseconds (default: 500)
     */
    public LlmJsonGenerator(
            LlmService llmService,
            @Value("${llm.json.max-retries:3}") int maxRetries,
            @Value("${llm.json.retry-delay-ms:500}") int retryDelayMs) {
        this.llmService = llmService;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    /**
     * Generates validated JSON from the LLM using a prompt template.
     * <p>
     * Automatically sanitizes the response and retries if JSON is invalid.
     *
     * @param template the prompt template
     * @param variables the template variables
     * @return validated JSON string
     * @throws LlmException if valid JSON cannot be generated after max retries
     */
    @Nonnull
    public String generateJson(@Nonnull PromptTemplate template, @Nonnull Map<String, Object> variables) {
        return generateWithRetry(() -> llmService.generate(template, variables));
    }

    /**
     * Generates validated JSON from the LLM using system and user prompts.
     * <p>
     * Automatically sanitizes the response and retries if JSON is invalid.
     *
     * @param systemPrompt the system prompt
     * @param userPrompt the user prompt
     * @return validated JSON string
     * @throws LlmException if valid JSON cannot be generated after max retries
     */
    @Nonnull
    public String generateJson(@Nonnull String systemPrompt, @Nonnull String userPrompt) {
        return generateWithRetry(() -> llmService.generate(systemPrompt, userPrompt));
    }

    /**
     * Generates validated JSON from the LLM using a simple prompt.
     * <p>
     * Automatically sanitizes the response and retries if JSON is invalid.
     *
     * @param prompt the prompt
     * @return validated JSON string
     * @throws LlmException if valid JSON cannot be generated after max retries
     */
    @Nonnull
    public String generateJson(@Nonnull String prompt) {
        return generateWithRetry(() -> llmService.generate(prompt));
    }

    /**
     * Generates and parses JSON to a specific type.
     * <p>
     * Automatically sanitizes and retries, then deserializes to the target type.
     *
     * @param template the prompt template
     * @param variables the template variables
     * @param targetType the target class to deserialize to
     * @param <T> the target type
     * @return the deserialized object
     * @throws LlmException if valid JSON cannot be generated or deserialization fails
     */
    @Nonnull
    public <T> T generateTyped(
            @Nonnull PromptTemplate template,
            @Nonnull Map<String, Object> variables,
            @Nonnull Class<T> targetType) {
        String json = generateJson(template, variables);
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new LlmException("Failed to deserialize JSON to " + targetType.getSimpleName(), e);
        }
    }

    /**
     * Generates JSON and returns it as a JsonNode.
     * <p>
     * Useful when you need to work with the JSON structure dynamically.
     *
     * @param template the prompt template
     * @param variables the template variables
     * @return the parsed JsonNode
     * @throws LlmException if valid JSON cannot be generated
     */
    @Nonnull
    public JsonNode generateJsonNode(@Nonnull PromptTemplate template, @Nonnull Map<String, Object> variables) {
        String json = generateJson(template, variables);
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new LlmException("Failed to parse JSON to JsonNode", e);
        }
    }

    /**
     * Core retry logic for JSON generation.
     */
    private String generateWithRetry(Supplier<String> generator) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("JSON generation attempt {}/{}", attempt, maxRetries);
                
                // Generate raw response from LLM
                String rawResponse = generator.get();
                
                // Sanitize and validate
                String sanitizedJson = JsonSanitizer.sanitizeAndValidate(rawResponse);
                
                // Double-check it's valid JSON
                OBJECT_MAPPER.readTree(sanitizedJson);
                
                if (attempt > 1) {
                    log.info("JSON generation succeeded on attempt {}", attempt);
                }
                
                return sanitizedJson;
                
            } catch (JsonSanitizer.JsonSanitizationException e) {
                lastException = e;
                log.warn("JSON sanitization failed on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                
            } catch (JsonProcessingException e) {
                lastException = e;
                log.warn("JSON parsing failed on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
                
            } catch (LlmException e) {
                // LLM service error - don't retry, propagate immediately
                throw e;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Unexpected error on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());
            }
            
            // Wait before retry (except on last attempt)
            if (attempt < maxRetries && retryDelayMs > 0) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new LlmException("JSON generation interrupted", ie);
                }
            }
        }
        
        // All retries exhausted
        throw new LlmException(
                "Failed to generate valid JSON after " + maxRetries + " attempts",
                lastException
        );
    }
}