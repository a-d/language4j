package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating content by topic.
 */
public record GenerateContentRequest(
        @Nonnull String topic
) {}