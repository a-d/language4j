package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for evaluating a user's response.
 */
public record EvaluateResponseRequest(
        @Nonnull String exercise,
        @Nonnull String userResponse,
        @Nonnull String expectedAnswer
) {}