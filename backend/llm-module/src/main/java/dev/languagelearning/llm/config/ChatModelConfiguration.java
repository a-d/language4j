package dev.languagelearning.llm.config;

import dev.languagelearning.config.AiProviderConfig;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * Configuration class that selects the appropriate ChatModel based on the configured provider.
 * <p>
 * Spring AI auto-configurations create ChatModel beans only when their corresponding
 * {@code spring.ai.{provider}.chat.enabled} property is true. This configuration creates
 * a primary bean that delegates to the available one based on configuration.
 * <p>
 * Provider enablement is controlled via environment variables:
 * <ul>
 *   <li>OPENAI_CHAT_ENABLED - Enable OpenAI ChatModel</li>
 *   <li>ANTHROPIC_CHAT_ENABLED - Enable Anthropic ChatModel</li>
 *   <li>OLLAMA_CHAT_ENABLED - Enable Ollama ChatModel</li>
 * </ul>
 * <p>
 * Supported providers:
 * <ul>
 *   <li>openai - Uses OpenAI's GPT models</li>
 *   <li>anthropic - Uses Anthropic's Claude models</li>
 *   <li>ollama - Uses locally running Ollama models</li>
 * </ul>
 */
@Slf4j
@Configuration
public class ChatModelConfiguration {

    private static final String OPENAI_BEAN = "openAiChatModel";
    private static final String ANTHROPIC_BEAN = "anthropicChatModel";
    private static final String OLLAMA_BEAN = "ollamaChatModel";

    /**
     * Creates a primary ChatModel bean that selects the appropriate provider-specific
     * ChatModel based on configuration.
     * <p>
     * With provider-specific enabled flags, typically only one ChatModel bean will be
     * available in the context. This method validates that the available bean matches
     * the configured provider.
     *
     * @param aiConfig        the AI provider configuration
     * @param availableModels map of all available ChatModel beans by name
     * @return the selected ChatModel for the configured provider
     * @throws IllegalStateException if no ChatModel is available or provider mismatch
     */
    @Bean
    @Primary
    @Nonnull
    public ChatModel primaryChatModel(
            @Nonnull AiProviderConfig aiConfig,
            @Nonnull Map<String, ChatModel> availableModels
    ) {
        String provider = aiConfig.getLlm().getProvider().toLowerCase();
        log.info("Configuring LLM provider: {}", provider);
        log.debug("Available ChatModel beans: {}", availableModels.keySet());

        if (availableModels.isEmpty()) {
            throw new IllegalStateException(
                    "No ChatModel beans found. Ensure one of the provider enabled flags is set to true: " +
                    "OPENAI_CHAT_ENABLED, ANTHROPIC_CHAT_ENABLED, or OLLAMA_CHAT_ENABLED"
            );
        }

        String expectedBeanName = switch (provider) {
            case "openai" -> OPENAI_BEAN;
            case "anthropic" -> ANTHROPIC_BEAN;
            case "ollama" -> OLLAMA_BEAN;
            default -> throw new IllegalStateException(
                    "Unsupported LLM provider: " + provider + 
                    ". Supported providers: openai, anthropic, ollama"
            );
        };

        // First try to get the expected bean for the configured provider
        ChatModel chatModel = availableModels.get(expectedBeanName);
        
        if (chatModel != null) {
            log.info("Using ChatModel: {} ({})", expectedBeanName, chatModel.getClass().getSimpleName());
            return chatModel;
        }

        // If expected bean not found, check if any other bean is available
        // This can happen if provider config doesn't match enabled flags
        if (availableModels.size() == 1) {
            Map.Entry<String, ChatModel> entry = availableModels.entrySet().iterator().next();
            log.warn("Configured provider '{}' does not match available ChatModel '{}'. " +
                     "Check that LLM_PROVIDER matches the enabled provider flag.",
                     provider, entry.getKey());
            log.info("Using available ChatModel: {} ({})", entry.getKey(), entry.getValue().getClass().getSimpleName());
            return entry.getValue();
        }

        // Multiple models available but not the expected one - user configuration issue
        throw new IllegalStateException(
                "ChatModel bean '" + expectedBeanName + "' not found. Available models: " + 
                availableModels.keySet() + ". " +
                "Ensure LLM_PROVIDER=" + provider + " matches the enabled provider flag " +
                "(e.g., " + provider.toUpperCase() + "_CHAT_ENABLED=true)"
        );
    }
}
