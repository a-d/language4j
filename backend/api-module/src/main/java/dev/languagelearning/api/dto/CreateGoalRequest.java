package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Request DTO for creating a goal.
 */
public record CreateGoalRequest(
        @Nonnull String title,
        @Nullable String description,
        @Nonnull String type,
        int targetValue,
        @Nonnull String unit
) {}