package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.chat.service.ChatService;
import dev.languagelearning.chat.service.ChatService.ChatResponse;
import dev.languagelearning.chat.service.ChatService.ChatResponseChunk;
import dev.languagelearning.core.domain.ChatMessage;
import dev.languagelearning.core.domain.ChatSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for chat operations.
 * <p>
 * Provides endpoints for managing chat sessions and sending/receiving messages.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat moderation and learning activities")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/session")
    @Operation(summary = "Get or create active session",
            description = "Gets the current active chat session or creates a new one with a greeting")
    public ResponseEntity<ChatSessionDto> getOrCreateSession() {
        ChatSession session = chatService.getOrCreateSession();
        return ResponseEntity.ok(toSessionDto(session));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get session by ID")
    public ResponseEntity<ChatSessionDto> getSession(@PathVariable UUID sessionId) {
        ChatSession session = chatService.getSession(sessionId);
        return ResponseEntity.ok(toSessionDto(session));
    }

    @GetMapping("/session/{sessionId}/messages")
    @Operation(summary = "Get all messages in a session")
    public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable UUID sessionId) {
        List<ChatMessage> messages = chatService.getMessages(sessionId);
        List<ChatMessageDto> dtos = messages.stream()
                .map(this::toMessageDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/session/{sessionId}/messages")
    @Operation(summary = "Send a message",
            description = "Sends a user message and gets an AI response with optional embedded activity")
    public ResponseEntity<ChatResponseDto> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        
        log.info("Sending message to session {}: {}", sessionId, 
                request.content().length() > 50 ? request.content().substring(0, 50) + "..." : request.content());
        
        ChatResponse response = chatService.sendMessage(sessionId, request.content());
        
        return ResponseEntity.ok(new ChatResponseDto(
                toMessageDto(response.message()),
                response.suggestions()
        ));
    }

    @PostMapping(value = "/session/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Send a message with streaming response",
            description = "Sends a user message and streams the AI response in real-time")
    public Flux<ServerSentEvent> sendMessageStream(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        
        log.info("Streaming message to session {}", sessionId);
        
        return chatService.sendMessageStream(sessionId, request.content())
                .map(chunk -> new ServerSentEvent(
                        chunk.type().name().toLowerCase(),
                        chunk.content()
                ));
    }

    @PostMapping("/messages/{messageId}/complete")
    @Operation(summary = "Complete an activity",
            description = "Marks an embedded activity as completed with a score and optional feedback")
    public ResponseEntity<Void> completeActivity(
            @PathVariable UUID messageId,
            @Valid @RequestBody CompleteActivityRequest request) {
        
        log.info("Completing activity for message {}: score={}", messageId, request.score());
        
        chatService.completeActivity(messageId, request.score(), request.feedback());
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Clear session",
            description = "Clears all messages from a session (starts fresh)")
    public ResponseEntity<Void> clearSession(@PathVariable UUID sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.ok().build();
    }

    // DTO conversion methods
    private ChatSessionDto toSessionDto(ChatSession session) {
        return new ChatSessionDto(
                session.getId(),
                session.getTitle(),
                session.isActive(),
                session.getMessageCount(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ChatMessageDto toMessageDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getEmbeddedActivityType(),
                message.getEmbeddedActivityContent(),
                message.isActivityCompleted(),
                message.getActivitySummary(),
                message.getCreatedAt()
        );
    }

    /**
     * Server-Sent Event wrapper for streaming responses.
     */
    public record ServerSentEvent(
            String event,
            String data
    ) {}
}