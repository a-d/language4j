package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating a learning plan.
 */
public record GenerateLearningPlanRequest(
        @Nonnull String dailyGoal,
        @Nonnull String weeklyGoal,
        @Nonnull String monthlyGoal
) {}