package dev.languagelearning.chat.service;

import dev.languagelearning.chat.model.ChatContext;
import dev.languagelearning.chat.model.ChatContext.*;
import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.repository.ExerciseResultRepository;
import dev.languagelearning.learning.service.GoalService;
import dev.languagelearning.learning.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for building chat context from user data.
 * <p>
 * Gathers user profile, goals, and activity information to provide
 * context for the chat AI's responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextService {

    private final UserService userService;
    private final GoalService goalService;
    private final ExerciseResultRepository exerciseResultRepository;

    /**
     * Builds the complete context for the current user.
     *
     * @return the chat context
     */
    public ChatContext buildContext() {
        User user = userService.getCurrentUser();
        return buildContext(user);
    }

    /**
     * Builds the complete context for a specific user.
     *
     * @param user the user
     * @return the chat context
     */
    public ChatContext buildContext(User user) {
        log.debug("Building chat context for user: {}", user.getDisplayName());

        UserContext userContext = buildUserContext(user);
        GoalsContext goalsContext = buildGoalsContext(user);
        ActivityContext activityContext = buildActivityContext(user);
        String timeOfDay = determineTimeOfDay(user);
        String suggestedFocus = determineSuggestedFocus(goalsContext, activityContext);

        return ChatContext.builder()
                .user(userContext)
                .goals(goalsContext)
                .recentActivity(activityContext)
                .timeOfDay(timeOfDay)
                .suggestedFocus(suggestedFocus)
                .build();
    }

    private UserContext buildUserContext(User user) {
        return UserContext.builder()
                .displayName(user.getDisplayName())
                .skillLevel(user.getSkillLevel().name())
                .nativeLanguage(user.getNativeLanguage())
                .targetLanguage(user.getTargetLanguage())
                .assessmentCompleted(user.isAssessmentCompleted())
                .build();
    }

    private GoalsContext buildGoalsContext(User user) {
        List<LearningGoal> dailyGoals = goalService.getCurrentUserGoalsByType(GoalType.DAILY);
        List<LearningGoal> weeklyGoals = goalService.getCurrentUserGoalsByType(GoalType.WEEKLY);

        List<GoalSummary> dailySummaries = dailyGoals.stream()
                .map(this::toGoalSummary)
                .toList();

        List<GoalSummary> weeklySummaries = weeklyGoals.stream()
                .map(this::toGoalSummary)
                .toList();

        int totalDailyProgress = dailyGoals.stream()
                .mapToInt(LearningGoal::getCurrentValue)
                .sum();

        int totalDailyTarget = dailyGoals.stream()
                .mapToInt(LearningGoal::getTargetValue)
                .sum();

        int completedToday = (int) dailyGoals.stream()
                .filter(LearningGoal::isCompleted)
                .count();

        return GoalsContext.builder()
                .dailyGoals(dailySummaries)
                .weeklyGoals(weeklySummaries)
                .totalDailyProgress(totalDailyProgress)
                .totalDailyTarget(totalDailyTarget)
                .completedToday(completedToday)
                .build();
    }

    private GoalSummary toGoalSummary(LearningGoal goal) {
        return GoalSummary.builder()
                .title(goal.getTitle())
                .currentValue(goal.getCurrentValue())
                .targetValue(goal.getTargetValue())
                .unit(goal.getUnit())
                .completed(goal.isCompleted())
                .build();
    }

    private ActivityContext buildActivityContext(User user) {
        var recentResults = exerciseResultRepository.findRecentByUserId(user.getId(), 7);
        var todayResults = exerciseResultRepository.findTodayByUserId(user.getId());

        String lastExerciseType = null;
        Integer lastScore = null;

        if (!recentResults.isEmpty()) {
            var latest = recentResults.get(0);
            lastExerciseType = latest.getExerciseType().name();
            lastScore = latest.getScore();
        }

        double averageScore = recentResults.stream()
                .mapToInt(r -> r.getScore())
                .average()
                .orElse(0.0);

        // Simple streak calculation (would need proper implementation)
        int dayStreak = calculateDayStreak(user);

        return ActivityContext.builder()
                .lastExerciseType(lastExerciseType)
                .lastScore(lastScore)
                .exercisesToday(todayResults.size())
                .dayStreak(dayStreak)
                .averageScore(averageScore)
                .build();
    }

    private int calculateDayStreak(User user) {
        // Simplified streak calculation
        // In a real implementation, this would check consecutive days with activity
        var todayResults = exerciseResultRepository.findTodayByUserId(user.getId());
        return todayResults.isEmpty() ? 0 : 1;
    }

    private String determineTimeOfDay(User user) {
        ZoneId zone = ZoneId.of(user.getTimezone() != null ? user.getTimezone() : "UTC");
        LocalTime now = LocalTime.now(zone);

        if (now.isBefore(LocalTime.of(12, 0))) {
            return "morning";
        } else if (now.isBefore(LocalTime.of(17, 0))) {
            return "afternoon";
        } else if (now.isBefore(LocalTime.of(21, 0))) {
            return "evening";
        } else {
            return "night";
        }
    }

    private String determineSuggestedFocus(GoalsContext goals, ActivityContext activity) {
        // Logic to suggest focus area based on:
        // 1. Incomplete daily goals
        // 2. Weak areas from recent activity
        // 3. Time since last practice of each skill

        if (goals.getDailyGoals() != null) {
            for (GoalSummary goal : goals.getDailyGoals()) {
                if (!goal.isCompleted()) {
                    // Suggest the first incomplete goal's focus area
                    String title = goal.getTitle().toLowerCase();
                    if (title.contains("vocabulary") || title.contains("word")) {
                        return "vocabulary practice";
                    } else if (title.contains("exercise") || title.contains("practice")) {
                        return "exercises";
                    } else if (title.contains("lesson")) {
                        return "lessons";
                    }
                }
            }
        }

        // Default suggestion based on activity
        if (activity != null && activity.getAverageScore() < 70) {
            return "review and practice";
        }

        return "continue learning";
    }
}