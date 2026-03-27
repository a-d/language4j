package dev.languagelearning.chat.service;

import dev.languagelearning.chat.model.ChatContext;
import dev.languagelearning.core.domain.ChatMessage;
import dev.languagelearning.core.domain.ChatSession;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing chat conversations and generating AI responses.
 * <p>
 * Provides both streaming and non-streaming response generation,
 * session management, and activity completion handling.
 */
public interface ChatService {

    /**
     * Gets or creates an active chat session for the current user.
     * <p>
     * If no active session exists, creates a new one with a greeting message.
     *
     * @return the active chat session
     */
    @Nonnull
    ChatSession getOrCreateSession();

    /**
     * Gets a chat session by ID.
     *
     * @param sessionId the session ID
     * @return the session
     * @throws dev.languagelearning.core.exception.EntityNotFoundException if not found
     */
    @Nonnull
    ChatSession getSession(@Nonnull UUID sessionId);

    /**
     * Gets all messages for a session.
     *
     * @param sessionId the session ID
     * @return list of messages
     */
    @Nonnull
    List<ChatMessage> getMessages(@Nonnull UUID sessionId);

    /**
     * Sends a user message and generates an AI response (streaming).
     *
     * @param sessionId the session ID
     * @param content   the user message content
     * @return a flux of response chunks (text + potential activity)
     */
    @Nonnull
    Flux<ChatResponseChunk> sendMessageStream(@Nonnull UUID sessionId, @Nonnull String content);

    /**
     * Sends a user message and generates an AI response (non-streaming).
     *
     * @param sessionId the session ID
     * @param content   the user message content
     * @return the complete response
     */
    @Nonnull
    ChatResponse sendMessage(@Nonnull UUID sessionId, @Nonnull String content);

    /**
     * Generates a greeting message for a new session.
     *
     * @param session the chat session
     * @param context the user context
     * @return the greeting response
     */
    @Nonnull
    ChatResponse generateGreeting(@Nonnull ChatSession session, @Nonnull ChatContext context);

    /**
     * Completes an embedded activity and replaces it with a summary.
     *
     * @param messageId the message ID containing the activity
     * @param score     the achieved score (0-100)
     * @param feedback  optional feedback text
     */
    void completeActivity(@Nonnull UUID messageId, int score, String feedback);

    /**
     * Clears the chat history for a session (starts fresh).
     *
     * @param sessionId the session ID
     */
    void clearSession(@Nonnull UUID sessionId);

    /**
     * Represents a streaming response chunk.
     */
    record ChatResponseChunk(
            ChunkType type,
            String content
    ) {
        public enum ChunkType {
            TEXT,           // Regular text chunk
            ACTIVITY_START, // Signals start of embedded activity
            ACTIVITY_DATA,  // Activity JSON data
            ACTIVITY_END,   // Signals end of embedded activity
            DONE            // Response complete
        }
    }

    /**
     * Represents a complete chat response.
     */
    record ChatResponse(
            ChatMessage message,
            List<String> suggestions
    ) {}
}