package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.LearningGoal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for learning goal data.
 */
public record GoalDto(
        UUID id,
        String type,
        String title,
        String description,
        int targetValue,
        int currentValue,
        String unit,
        LocalDate startDate,
        LocalDate endDate,
        boolean completed,
        Instant completedAt,
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