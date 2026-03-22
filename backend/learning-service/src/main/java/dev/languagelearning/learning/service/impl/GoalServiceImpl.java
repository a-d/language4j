package dev.languagelearning.learning.service.impl;

import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.core.repository.LearningGoalRepository;
import dev.languagelearning.learning.service.GoalService;
import dev.languagelearning.learning.service.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of GoalService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final LearningGoalRepository goalRepository;
    private final UserService userService;

    @Override
    @Nonnull
    public List<LearningGoal> getCurrentUserGoals() {
        User user = userService.getCurrentUser();
        return goalRepository.findByUser(user);
    }

    @Override
    @Nonnull
    public List<LearningGoal> getCurrentUserGoalsByType(@Nonnull GoalType type) {
        User user = userService.getCurrentUser();
        return goalRepository.findByUserAndGoalType(user, type);
    }

    @Override
    @Nonnull
    public List<LearningGoal> getActiveDailyGoals() {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now();
        return goalRepository.findActiveGoals(user, today);
    }

    @Override
    @Nonnull
    @Transactional
    public LearningGoal createGoal(
            @Nonnull String title,
            @Nullable String description,
            @Nonnull GoalType type,
            int targetValue,
            @Nonnull String unit
    ) {
        User user = userService.getCurrentUser();
        LocalDate[] dateRange = calculateDateRange(type);

        log.info("Creating {} goal for user {}: {}", type, user.getId(), title);

        LearningGoal goal = LearningGoal.of(
                user,
                type,
                title,
                targetValue,
                unit,
                dateRange[0],
                dateRange[1]
        );
        goal.setDescription(description);

        return goalRepository.save(goal);
    }

    @Override
    @Nonnull
    @Transactional
    public LearningGoal updateProgress(@Nonnull UUID goalId, int newValue) {
        LearningGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> EntityNotFoundException.goalNotFound(goalId));

        goal.setCurrentValue(newValue);

        // Auto-complete if target reached
        if (newValue >= goal.getTargetValue() && !goal.isCompleted()) {
            goal.setCompleted(true);
            goal.setCompletedAt(Instant.now());
            log.info("Goal {} completed!", goalId);
        }

        return goalRepository.save(goal);
    }

    @Override
    @Nonnull
    @Transactional
    public LearningGoal incrementProgress(@Nonnull UUID goalId, int increment) {
        LearningGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> EntityNotFoundException.goalNotFound(goalId));

        return updateProgress(goalId, goal.getCurrentValue() + increment);
    }

    @Override
    @Nonnull
    @Transactional
    public LearningGoal completeGoal(@Nonnull UUID goalId) {
        LearningGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> EntityNotFoundException.goalNotFound(goalId));

        goal.setCompleted(true);
        goal.setCompletedAt(Instant.now());
        goal.setCurrentValue(goal.getTargetValue());

        log.info("Goal {} manually completed", goalId);
        return goalRepository.save(goal);
    }

    @Override
    @Transactional
    public void deleteGoal(@Nonnull UUID goalId) {
        log.info("Deleting goal {}", goalId);
        goalRepository.deleteById(goalId);
    }

    @Override
    @Transactional
    public void createDefaultGoals(@Nonnull UUID userId) {
        log.info("Creating default goals for user {}", userId);
        
        User user = userService.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        LocalDate today = LocalDate.now();

        // Daily goals
        createGoalForUser(user, "Complete 3 lessons", null, GoalType.DAILY, 3, "lessons", today, today);
        createGoalForUser(user, "Learn 10 new words", null, GoalType.DAILY, 10, "words", today, today);
        createGoalForUser(user, "Practice speaking", null, GoalType.DAILY, 15, "minutes", today, today);

        // Weekly goals
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        createGoalForUser(user, "Complete 15 exercises", null, GoalType.WEEKLY, 15, "exercises", today, weekEnd);

        // Monthly goals
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        createGoalForUser(user, "Learn 100 vocabulary words", null, GoalType.MONTHLY, 100, "words", today, monthEnd);
    }

    private void createGoalForUser(User user, String title, String description, GoalType type,
                                   int targetValue, String unit, LocalDate startDate, LocalDate endDate) {
        LearningGoal goal = LearningGoal.of(user, type, title, targetValue, unit, startDate, endDate);
        goal.setDescription(description);
        goalRepository.save(goal);
    }

    private LocalDate[] calculateDateRange(GoalType type) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today;
        LocalDate endDate;

        switch (type) {
            case DAILY -> endDate = today;
            case WEEKLY -> endDate = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            case MONTHLY -> endDate = today.with(TemporalAdjusters.lastDayOfMonth());
            case YEARLY -> endDate = today.with(TemporalAdjusters.lastDayOfYear());
            default -> endDate = today;
        }

        return new LocalDate[]{startDate, endDate};
    }
}