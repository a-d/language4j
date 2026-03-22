package dev.languagelearning.learning.service;

import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing learning goals.
 */
public interface GoalService {

    /**
     * Gets all goals for the current user.
     *
     * @return list of goals
     */
    @Nonnull
    List<LearningGoal> getCurrentUserGoals();

    /**
     * Gets goals for the current user filtered by type.
     *
     * @param type the goal type (DAILY, WEEKLY, MONTHLY, YEARLY)
     * @return list of goals
     */
    @Nonnull
    List<LearningGoal> getCurrentUserGoalsByType(@Nonnull GoalType type);

    /**
     * Gets active (non-completed) goals for today.
     *
     * @return list of active daily goals
     */
    @Nonnull
    List<LearningGoal> getActiveDailyGoals();

    /**
     * Creates a new goal for the current user.
     *
     * @param title the goal title
     * @param description optional description
     * @param type the goal type
     * @param targetValue the target value to reach
     * @param unit the unit of measurement
     * @return the created goal
     */
    @Nonnull
    LearningGoal createGoal(
            @Nonnull String title,
            @Nullable String description,
            @Nonnull GoalType type,
            int targetValue,
            @Nonnull String unit
    );

    /**
     * Updates progress on a goal.
     *
     * @param goalId the goal ID
     * @param newValue the new current value
     * @return the updated goal
     */
    @Nonnull
    LearningGoal updateProgress(@Nonnull UUID goalId, int newValue);

    /**
     * Increments progress on a goal by a specified amount.
     *
     * @param goalId the goal ID
     * @param increment the amount to add
     * @return the updated goal
     */
    @Nonnull
    LearningGoal incrementProgress(@Nonnull UUID goalId, int increment);

    /**
     * Marks a goal as completed.
     *
     * @param goalId the goal ID
     * @return the completed goal
     */
    @Nonnull
    LearningGoal completeGoal(@Nonnull UUID goalId);

    /**
     * Deletes a goal.
     *
     * @param goalId the goal ID
     */
    void deleteGoal(@Nonnull UUID goalId);

    /**
     * Creates default goals for a new user.
     *
     * @param userId the user ID
     */
    void createDefaultGoals(@Nonnull UUID userId);
}