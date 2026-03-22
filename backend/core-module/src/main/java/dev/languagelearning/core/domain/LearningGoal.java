package dev.languagelearning.core.domain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a learning goal for a user.
 * <p>
 * Goals can be daily, weekly, monthly, or yearly and track progress
 * towards specific learning objectives.
 */
@Entity
@Table(name = "learning_goals")
@Getter
@Setter
@NoArgsConstructor
public class LearningGoal extends BaseEntity {

    /**
     * The user this goal belongs to.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Type of goal (daily, weekly, monthly, yearly).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    private GoalType goalType;

    /**
     * Title of the goal.
     */
    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Detailed description of the goal.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Target value to achieve (e.g., number of lessons, vocabulary words).
     */
    @Positive
    @Column(name = "target_value", nullable = false)
    private int targetValue;

    /**
     * Current progress towards the target.
     */
    @Column(name = "current_value", nullable = false)
    private int currentValue = 0;

    /**
     * Unit of measurement (e.g., "lessons", "words", "minutes").
     */
    @NotBlank
    @Column(name = "unit", nullable = false)
    private String unit;

    /**
     * Start date of the goal period.
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * End date of the goal period.
     */
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Whether the goal has been completed.
     */
    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    /**
     * When the goal was completed (null if not completed).
     */
    @Nullable
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Target skill level to achieve (optional).
     */
    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "target_skill_level")
    private SkillLevel targetSkillLevel;

    /**
     * Calculates the completion percentage.
     *
     * @return percentage of goal completion (0-100)
     */
    public int getCompletionPercentage() {
        if (targetValue <= 0) {
            return 0;
        }
        return Math.min(100, (currentValue * 100) / targetValue);
    }

    /**
     * Checks if the goal is currently active (within date range and not completed).
     *
     * @return true if the goal is active
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !completed && !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Checks if the goal period has expired without completion.
     *
     * @return true if the goal has expired
     */
    public boolean isExpired() {
        return !completed && LocalDate.now().isAfter(endDate);
    }

    /**
     * Increments progress towards the goal.
     *
     * @param amount the amount to add to current progress
     */
    public void incrementProgress(int amount) {
        this.currentValue += amount;
        if (this.currentValue >= this.targetValue && !this.completed) {
            this.completed = true;
            this.completedAt = Instant.now();
        }
    }

    /**
     * Gets the remaining value to achieve the goal.
     *
     * @return remaining value, or 0 if goal is completed
     */
    public int getRemainingValue() {
        return Math.max(0, targetValue - currentValue);
    }

    @Nonnull
    public Optional<SkillLevel> getTargetSkillLevel() {
        return Optional.ofNullable(targetSkillLevel);
    }

    @Nonnull
    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    /**
     * Factory method to create a new learning goal.
     *
     * @param user        the user
     * @param goalType    the type of goal
     * @param title       the goal title
     * @param targetValue the target value to achieve
     * @param unit        the unit of measurement
     * @param startDate   the start date
     * @param endDate     the end date
     * @return a new LearningGoal instance
     */
    public static LearningGoal of(
            User user,
            GoalType goalType,
            String title,
            int targetValue,
            String unit,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LearningGoal goal = new LearningGoal();
        goal.setUser(user);
        goal.setGoalType(goalType);
        goal.setTitle(title);
        goal.setTargetValue(targetValue);
        goal.setUnit(unit);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        return goal;
    }
}