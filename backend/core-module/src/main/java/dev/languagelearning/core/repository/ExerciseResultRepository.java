package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.ExerciseResult;
import dev.languagelearning.core.domain.ExerciseType;
import dev.languagelearning.core.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ExerciseResult entity operations.
 */
@Repository
public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, UUID> {

    /**
     * Finds all exercise results for a user.
     *
     * @param user     the user
     * @param pageable pagination info
     * @return page of exercise results
     */
    Page<ExerciseResult> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Finds exercise results for a user by exercise type.
     *
     * @param user         the user
     * @param exerciseType the type of exercise
     * @param pageable     pagination info
     * @return page of matching results
     */
    Page<ExerciseResult> findByUserAndExerciseTypeOrderByCreatedAtDesc(
            User user,
            ExerciseType exerciseType,
            Pageable pageable
    );

    /**
     * Finds recent exercise results for a user.
     *
     * @param user  the user
     * @param since results since this time
     * @return list of recent results
     */
    List<ExerciseResult> findByUserAndCreatedAtAfterOrderByCreatedAtDesc(User user, Instant since);

    /**
     * Calculates average score for a user on a specific exercise type.
     *
     * @param user         the user
     * @param exerciseType the exercise type
     * @return average score or null if no results
     */
    @Query("SELECT AVG(r.score) FROM ExerciseResult r " +
           "WHERE r.user = :user AND r.exerciseType = :exerciseType")
    Double calculateAverageScore(
            @Param("user") User user,
            @Param("exerciseType") ExerciseType exerciseType
    );

    /**
     * Calculates overall average score for a user.
     *
     * @param user the user
     * @return average score or null if no results
     */
    @Query("SELECT AVG(r.score) FROM ExerciseResult r WHERE r.user = :user")
    Double calculateOverallAverageScore(@Param("user") User user);

    /**
     * Counts exercises completed by a user within a time range.
     *
     * @param user  the user
     * @param start range start
     * @param end   range end
     * @return count of exercises
     */
    @Query("SELECT COUNT(r) FROM ExerciseResult r " +
           "WHERE r.user = :user " +
           "AND r.createdAt BETWEEN :start AND :end")
    long countExercisesInRange(
            @Param("user") User user,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * Counts passed exercises for a user within a time range.
     *
     * @param user  the user
     * @param start range start
     * @param end   range end
     * @return count of passed exercises
     */
    @Query("SELECT COUNT(r) FROM ExerciseResult r " +
           "WHERE r.user = :user " +
           "AND r.passed = true " +
           "AND r.createdAt BETWEEN :start AND :end")
    long countPassedExercisesInRange(
            @Param("user") User user,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * Gets total time spent on exercises by a user.
     *
     * @param user the user
     * @return total time in seconds
     */
    @Query("SELECT COALESCE(SUM(r.timeSpentSeconds), 0) FROM ExerciseResult r WHERE r.user = :user")
    long getTotalTimeSpentSeconds(@Param("user") User user);

    /**
     * Gets exercise counts grouped by type for a user.
     *
     * @param user the user
     * @return list of [ExerciseType, count] arrays
     */
    @Query("SELECT r.exerciseType, COUNT(r) FROM ExerciseResult r " +
           "WHERE r.user = :user " +
           "GROUP BY r.exerciseType")
    List<Object[]> countByExerciseType(@Param("user") User user);

    /**
     * Finds the most recent result for a specific exercise reference.
     *
     * @param user              the user
     * @param exerciseReference the exercise reference
     * @return the most recent result if exists
     */
    @Query("SELECT r FROM ExerciseResult r " +
           "WHERE r.user = :user " +
           "AND r.exerciseReference = :reference " +
           "ORDER BY r.createdAt DESC " +
           "LIMIT 1")
    ExerciseResult findMostRecentByReference(
            @Param("user") User user,
            @Param("reference") String exerciseReference
    );

    /**
     * Finds recent exercise results for a user by user ID within N days.
     *
     * @param userId the user ID
     * @param days   number of days to look back
     * @return list of recent results, most recent first
     */
    @Query(value = "SELECT * FROM exercise_results r " +
           "WHERE r.user_id = :userId " +
           "AND r.created_at >= CURRENT_TIMESTAMP - CAST(:days || ' days' AS INTERVAL) " +
           "ORDER BY r.created_at DESC", nativeQuery = true)
    List<ExerciseResult> findRecentByUserId(@Param("userId") UUID userId, @Param("days") int days);

    /**
     * Finds today's exercise results for a user by user ID.
     *
     * @param userId the user ID
     * @return list of today's results
     */
    @Query(value = "SELECT * FROM exercise_results r " +
           "WHERE r.user_id = :userId " +
           "AND DATE(r.created_at) = CURRENT_DATE " +
           "ORDER BY r.created_at DESC", nativeQuery = true)
    List<ExerciseResult> findTodayByUserId(@Param("userId") UUID userId);
}
