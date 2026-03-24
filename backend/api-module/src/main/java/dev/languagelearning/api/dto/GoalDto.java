package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.LearningGoal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for learning goal data.
 */
@Schema(description = "Learning goal information")
public record GoalDto(
        @Schema(description = "Unique goal identifier", example = "550e8400-e29b-41d4-a716-446655440001")
        UUID id,

        @Schema(description = "Goal type (DAILY, WEEKLY, MONTHLY, YEARLY)", example = "DAILY")
        String type,

        @Schema(description = "Goal title", example = "Learn new words")
        String title,

        @Schema(description = "Detailed goal description", example = "Learn 10 new vocabulary words each day", nullable = true)
        String description,

        @Schema(description = "Target value to achieve", example = "10")
        int targetValue,

        @Schema(description = "Current progress value", example = "5")
        int currentValue,

        @Schema(description = "Unit of measurement", example = "words")
        String unit,

        @Schema(description = "Goal start date", example = "2024-01-15")
        LocalDate startDate,

        @Schema(description = "Goal end date (null for ongoing goals)", example = "2024-01-15", nullable = true)
        LocalDate endDate,

        @Schema(description = "Whether the goal is completed", example = "false")
        boolean completed,

        @Schema(description = "Completion timestamp (null if not completed)", example = "2024-01-15T18:30:00Z", nullable = true)
        Instant completedAt,

        @Schema(description = "Progress percentage (0-100)", example = "50", minimum = "0", maximum = "100")
        int progressPercent
) {
    public static GoalDto from(LearningGoal goal) {
        int progressPercent = goal.getTargetValue() > 0
                ? Math.min(100, (goal.getCurrentValue() * 100) / goal.getTargetValue())
                : 0;

        return new GoalDto(
                goal.getId(),
                goal.getGoalType().name(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetValue(),
                goal.getCurrentValue(),
                goal.getUnit(),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.isCompleted(),
                goal.getCompletedAt().orElse(null),
                progressPercent
        );
    }
}