package dev.languagelearning.learning.service;

import dev.languagelearning.core.domain.ExerciseResult;
import dev.languagelearning.core.domain.ExerciseType;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing exercise results and statistics.
 * <p>
 * Handles saving exercise outcomes, retrieving history, and calculating
 * performance statistics for users.
 */
public interface ExerciseResultService {

    /**
     * Saves an exercise result for the current user.
     *
     * @param exerciseType    the type of exercise
     * @param score           the score achieved (0-100)
     * @param correctAnswers  number of correct answers
     * @param totalQuestions  total number of questions
     * @param timeSpentSeconds time spent in seconds
     * @param userResponse    the user's response (JSON or text)
     * @param correctResponse the correct response
     * @param feedback        optional AI feedback
     * @return the saved exercise result
     */
    @Nonnull
    ExerciseResult saveResult(
            @Nonnull ExerciseType exerciseType,
            int score,
            int correctAnswers,
            int totalQuestions,
            long timeSpentSeconds,
            String userResponse,
            String correctResponse,
            String feedback
    );

    /**
     * Saves an exercise result with an exercise reference.
     *
     * @param exerciseType      the type of exercise
     * @param exerciseReference reference to the exercise content
     * @param score             the score achieved (0-100)
     * @param correctAnswers    number of correct answers
     * @param totalQuestions    total number of questions
     * @param timeSpentSeconds  time spent in seconds
     * @param userResponse      the user's response
     * @param correctResponse   the correct response
     * @param feedback          optional AI feedback
     * @return the saved exercise result
     */
    @Nonnull
    ExerciseResult saveResult(
            @Nonnull ExerciseType exerciseType,
            String exerciseReference,
            int score,
            int correctAnswers,
            int totalQuestions,
            long timeSpentSeconds,
            String userResponse,
            String correctResponse,
            String feedback
    );

    /**
     * Finds an exercise result by ID.
     *
     * @param id the result ID
     * @return the exercise result if found
     */
    @Nonnull
    Optional<ExerciseResult> findById(@Nonnull UUID id);

    /**
     * Gets paginated exercise history for the current user.
     *
     * @param pageable pagination info
     * @return page of exercise results
     */
    @Nonnull
    Page<ExerciseResult> getHistory(@Nonnull Pageable pageable);

    /**
     * Gets paginated exercise history filtered by type for the current user.
     *
     * @param exerciseType the exercise type filter
     * @param pageable     pagination info
     * @return page of exercise results
     */
    @Nonnull
    Page<ExerciseResult> getHistoryByType(@Nonnull ExerciseType exerciseType, @Nonnull Pageable pageable);

    /**
     * Gets recent exercise results for the current user.
     *
     * @param since results since this time
     * @return list of recent results
     */
    @Nonnull
    List<ExerciseResult> getRecentResults(@Nonnull Instant since);

    /**
     * Gets recent exercise results from the last N days.
     *
     * @param days number of days to look back
     * @return list of recent results
     */
    @Nonnull
    List<ExerciseResult> getRecentResults(int days);

    /**
     * Calculates the average score for an exercise type.
     *
     * @param exerciseType the exercise type
     * @return average score or empty if no results
     */
    @Nonnull
    Optional<Double> getAverageScore(@Nonnull ExerciseType exerciseType);

    /**
     * Calculates the overall average score across all exercise types.
     *
     * @return overall average score or empty if no results
     */
    @Nonnull
    Optional<Double> getOverallAverageScore();

    /**
     * Gets the total time spent on exercises.
     *
     * @return total time in seconds
     */
    long getTotalTimeSpentSeconds();

    /**
     * Counts exercises completed within a time range.
     *
     * @param start range start
     * @param end   range end
     * @return count of exercises
     */
    long countExercisesInRange(@Nonnull Instant start, @Nonnull Instant end);

    /**
     * Counts exercises completed today.
     *
     * @return count of exercises completed today
     */
    long countExercisesToday();

    /**
     * Gets exercise counts grouped by type.
     *
     * @return map of exercise type to count
     */
    @Nonnull
    Map<ExerciseType, Long> getCountsByType();

    /**
     * Gets statistics summary for the current user.
     *
     * @return exercise statistics
     */
    @Nonnull
    ExerciseStatistics getStatistics();

    /**
     * Statistics summary record.
     *
     * @param totalExercises     total number of exercises completed
     * @param exercisesToday     exercises completed today
     * @param averageScore       overall average score
     * @param totalTimeSeconds   total time spent in seconds
     * @param passRate           percentage of exercises passed
     * @param countsByType       counts per exercise type
     */
    record ExerciseStatistics(
            long totalExercises,
            long exercisesToday,
            Double averageScore,
            long totalTimeSeconds,
            Double passRate,
            Map<ExerciseType, Long> countsByType
    ) {}
}