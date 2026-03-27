package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.EmbeddedActivityType;
import dev.languagelearning.core.domain.MessageRole;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a chat message.
 */
public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content,
        EmbeddedActivityType embeddedActivityType,
        String embeddedActivityContent,
        boolean activityCompleted,
        String activitySummary,
        Instant createdAt
) {}