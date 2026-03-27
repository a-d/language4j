package dev.languagelearning.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a chat session.
 */
public record ChatSessionDto(
        UUID id,
        String title,
        boolean active,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {}