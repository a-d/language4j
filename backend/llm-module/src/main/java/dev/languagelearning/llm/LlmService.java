package dev.languagelearning.llm;

import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Service interface for LLM interactions.
 * <p>
 * Provides a unified interface for text generation across different AI providers.
 */
public interface LlmService {

    /**
     * Generates a response from the LLM for the given prompt.
     *
     * @param userPrompt the user's prompt
     * @return the generated response text
     */
    @Nonnull
    String generate(@Nonnull String userPrompt);

    /**
     * Generates a response with a system message for context.
     *
     * @param systemPrompt the system message providing context
     * @param userPrompt   the user's prompt
     * @return the generated response text
     */
    @Nonnull
    String generate(@Nonnull String systemPrompt, @Nonnull String userPrompt);

    /**
     * Generates a response using a template with variables.
     *
     * @param template  the prompt template
     * @param variables the variables to substitute
     * @return the generated response text
     */
    @Nonnull
    String generate(@Nonnull PromptTemplate template, @Nonnull Map<String, Object> variables);

    /**
     * Generates a streaming response for the given prompt.
     *
     * @param userPrompt the user's prompt
     * @return a flux of response chunks
     */
    @Nonnull
    Flux<String> generateStream(@Nonnull String userPrompt);

    /**
     * Generates a streaming response with a system message.
     *
     * @param systemPrompt the system message providing context
     * @param userPrompt   the user's prompt
     * @return a flux of response chunks
     */
    @Nonnull
    Flux<String> generateStream(@Nonnull String systemPrompt, @Nonnull String userPrompt);

    /**
     * Gets the underlying chat model.
     *
     * @return the chat model
     */
    @Nonnull
    ChatModel getChatModel();
}