package dev.languagelearning.llm.impl;

import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.exception.LlmException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of LlmService using Spring AI ChatModel.
 * <p>
 * Supports both synchronous and streaming generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLlmService implements LlmService {

    private final ChatModel chatModel;

    @Override
    @Nonnull
    public String generate(@Nonnull String userPrompt) {
        log.debug("Generating response for prompt: {}", truncateForLog(userPrompt));
        try {
            Prompt prompt = new Prompt(userPrompt);
            ChatResponse response = chatModel.call(prompt);
            String result = extractContent(response);
            log.debug("Generated response: {}", truncateForLog(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to generate LLM response", e);
            throw new LlmException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public String generate(@Nonnull String systemPrompt, @Nonnull String userPrompt) {
        log.debug("Generating response with system prompt");
        try {
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            );
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            return extractContent(response);
        } catch (Exception e) {
            log.error("Failed to generate LLM response", e);
            throw new LlmException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public String generate(@Nonnull PromptTemplate template, @Nonnull Map<String, Object> variables) {
        String renderedPrompt = template.render(variables);
        return generate(renderedPrompt);
    }

    @Override
    @Nonnull
    public Flux<String> generateStream(@Nonnull String userPrompt) {
        log.debug("Generating streaming response for prompt: {}", truncateForLog(userPrompt));
        
        if (!(chatModel instanceof StreamingChatModel streamingModel)) {
            // Fallback: return single-chunk flux for non-streaming models
            return Flux.just(generate(userPrompt));
        }

        try {
            Prompt prompt = new Prompt(userPrompt);
            return streamingModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && 
                            response.getResult().getOutput() != null &&
                            response.getResult().getOutput().getContent() != null) {
                            return response.getResult().getOutput().getContent();
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty())
                    .doOnError(e -> log.error("Streaming error", e));
        } catch (Exception e) {
            log.error("Failed to start streaming generation", e);
            return Flux.error(new LlmException("Failed to stream response: " + e.getMessage(), e));
        }
    }

    @Override
    @Nonnull
    public Flux<String> generateStream(@Nonnull String systemPrompt, @Nonnull String userPrompt) {
        log.debug("Generating streaming response with system prompt");
        
        if (!(chatModel instanceof StreamingChatModel streamingModel)) {
            return Flux.just(generate(systemPrompt, userPrompt));
        }

        try {
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            );
            Prompt prompt = new Prompt(messages);
            return streamingModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && 
                            response.getResult().getOutput() != null &&
                            response.getResult().getOutput().getContent() != null) {
                            return response.getResult().getOutput().getContent();
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty())
                    .doOnError(e -> log.error("Streaming error", e));
        } catch (Exception e) {
            log.error("Failed to start streaming generation", e);
            return Flux.error(new LlmException("Failed to stream response: " + e.getMessage(), e));
        }
    }

    @Override
    @Nonnull
    public ChatModel getChatModel() {
        return chatModel;
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || 
            response.getResult().getOutput() == null) {
            throw new LlmException("Empty response from LLM");
        }
        String content = response.getResult().getOutput().getContent();
        if (content == null) {
            throw new LlmException("Null content in LLM response");
        }
        return content;
    }

    private String truncateForLog(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}