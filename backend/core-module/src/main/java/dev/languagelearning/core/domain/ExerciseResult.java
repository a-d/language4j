package dev.languagelearning.core.domain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.Optional;

/**
 * Records the result of a completed exercise.
 * <p>
 * Tracks user performance including score, time spent, and any feedback.
 */
@Entity
@Table(name = "exercise_results")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseResult extends BaseEntity {

    /**
     * The user who completed the exercise.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Type of exercise completed.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false)
    private ExerciseType exerciseType;

    /**
     * Reference to the exercise content (e.g., lesson ID, card ID).
     */
    @Column(name = "exercise_reference")
    private String exerciseReference;

    /**
     * Score achieved (0-100).
     */
    @Min(0)
    @Max(100)
    @Column(name = "score", nullable = false)
    private int score;

    /**
     * Maximum possible score.
     */
    @Min(0)
    @Column(name = "max_score", nullable = false)
    private int maxScore = 100;

    /**
     * Time spent on the exercise in seconds.
     */
    @Min(0)
    @Column(name = "time_spent_seconds", nullable = false)
    private long timeSpentSeconds;

    /**
     * Number of correct answers.
     */
    @Min(0)
    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers = 0;

    /**
     * Total number of questions/items.
     */
    @Min(0)
    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    /**
     * The user's submitted answer or response.
     */
    @Column(name = "user_response", columnDefinition = "TEXT")
    private String userResponse;

    /**
     * The correct answer for comparison.
     */
    @Column(name = "correct_response", columnDefinition = "TEXT")
    private String correctResponse;

    /**
     * AI-generated feedback on the response.
     */
    @Nullable
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /**
     * Skill level at the time of the exercise.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level_at_time", nullable = false)
    private SkillLevel skillLevelAtTime;

    /**
     * Whether the exercise was passed (score >= passing threshold).
     */
    @Column(name = "passed", nullable = false)
    private boolean passed;

    /**
     * Gets the time spent as a Duration.
     *
     * @return time spent duration
     */
    @Nonnull
    public Duration getTimeSpent() {
        return Duration.ofSeconds(timeSpentSeconds);
    }

    /**
     * Sets the time spent from a Duration.
     *
     * @param duration the time spent
     */
    public void setTimeSpent(@Nonnull Duration duration) {
        this.timeSpentSeconds = duration.toSeconds();
    }

    /**
     * Calculates the accuracy percentage.
     *
     * @return accuracy as percentage (0-100)
     */
    public int getAccuracyPercentage() {
        if (totalQuestions <= 0) {
            return 0;
        }
        return (correctAnswers * 100) / totalQuestions;
    }

    /**
     * Calculates the normalized score percentage.
     *
     * @return normalized score (0-100)
     */
    public int getNormalizedScore() {
        if (maxScore <= 0) {
            return 0;
        }
        return (score * 100) / maxScore;
    }

    @Nonnull
    public Optional<String> getFeedback() {
        return Optional.ofNullable(feedback);
    }

    /**
     * Factory method to create a new exercise result.
     *
     * @param user             the user
     * @param exerciseType     the type of exercise
     * @param score            the score achieved
     * @param timeSpent        the time spent
     * @param skillLevelAtTime the skill level at the time
     * @return a new ExerciseResult instance
     */
    public static ExerciseResult of(
            User user,
            ExerciseType exerciseType,
            int score,
            Duration timeSpent,
            SkillLevel skillLevelAtTime
    ) {
        ExerciseResult result = new ExerciseResult();
        result.setUser(user);
        result.setExerciseType(exerciseType);
        result.setScore(score);
        result.setTimeSpent(timeSpent);
        result.setSkillLevelAtTime(skillLevelAtTime);
        result.setPassed(score >= 70); // Default passing threshold
        return result;
    }
}