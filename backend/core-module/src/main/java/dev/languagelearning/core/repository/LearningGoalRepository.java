package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LearningGoal entity operations.
 */
@Repository
public interface LearningGoalRepository extends JpaRepository<LearningGoal, UUID> {

    /**
     * Finds all goals for a specific user.
     *
     * @param user the user
     * @return list of user's goals
     */
    List<LearningGoal> findByUser(User user);

    /**
     * Finds all goals for a user of a specific type.
     *
     * @param user     the user
     * @param goalType the type of goal
     * @return list of matching goals
     */
    List<LearningGoal> findByUserAndGoalType(User user, GoalType goalType);

    /**
     * Finds all active goals for a user (not completed and within date range).
     *
     * @param user the user
     * @param date the current date
     * @return list of active goals
     */
    @Query("SELECT g FROM LearningGoal g WHERE g.user = :user " +
           "AND g.completed = false " +
           "AND g.startDate <= :date " +
           "AND g.endDate >= :date")
    List<LearningGoal> findActiveGoals(@Param("user") User user, @Param("date") LocalDate date);

    /**
     * Finds all completed goals for a user.
     *
     * @param user the user
     * @return list of completed goals
     */
    List<LearningGoal> findByUserAndCompletedTrue(User user);

    /**
     * Finds all incomplete goals for a user that have expired.
     *
     * @param user the user
     * @param date the current date
     * @return list of expired incomplete goals
     */
    @Query("SELECT g FROM LearningGoal g WHERE g.user = :user " +
           "AND g.completed = false " +
           "AND g.endDate < :date")
    List<LearningGoal> findExpiredGoals(@Param("user") User user, @Param("date") LocalDate date);

    /**
     * Finds the current goal of a specific type for a user.
     *
     * @param user     the user
     * @param goalType the type of goal
     * @param date     the current date
     * @return the current goal if exists
     */
    @Query("SELECT g FROM LearningGoal g WHERE g.user = :user " +
           "AND g.goalType = :goalType " +
           "AND g.startDate <= :date " +
           "AND g.endDate >= :date")
    Optional<LearningGoal> findCurrentGoal(
            @Param("user") User user,
            @Param("goalType") GoalType goalType,
            @Param("date") LocalDate date
    );

    /**
     * Counts completed goals for a user within a date range.
     *
     * @param user      the user
     * @param startDate range start
     * @param endDate   range end
     * @return count of completed goals
     */
    @Query("SELECT COUNT(g) FROM LearningGoal g WHERE g.user = :user " +
           "AND g.completed = true " +
           "AND g.completedAt BETWEEN :startDate AND :endDate")
    long countCompletedGoalsInRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}