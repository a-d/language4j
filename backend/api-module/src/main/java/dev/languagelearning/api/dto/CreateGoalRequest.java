package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Request DTO for creating a goal.
 */
@Schema(description = "Request to create a new goal")
public record CreateGoalRequest(
        @Schema(description = "Goal title", example = "Learn new words", requiredMode = Schema.RequiredMode.REQUIRED)
        @Nonnull String title,

        @Schema(description = "Detailed goal description", example = "Learn 10 new vocabulary words each day", nullable = true)
        @Nullable String description,

        @Schema(description = "Goal type (DAILY, WEEKLY, MONTHLY, YEARLY)", example = "DAILY", requiredMode = Schema.RequiredMode.REQUIRED)
        @Nonnull String type,

        @Schema(description = "Target value to achieve", example = "10", minimum = "1")
        int targetValue,

        @Schema(description = "Unit of measurement", example = "words", requiredMode = Schema.RequiredMode.REQUIRED)
        @Nonnull String unit
) {}