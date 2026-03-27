package dev.languagelearning.chat.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Context information used to ground the chat AI's responses.
 * <p>
 * Contains user profile, learning progress, goals, and other
 * relevant information for personalized responses.
 */
@Getter
@Builder
public class ChatContext {

    /**
     * User profile information.
     */
    private final UserContext user;

    /**
     * Current learning goals status.
     */
    private final GoalsContext goals;

    /**
     * Recent activity summary.
     */
    private final ActivityContext recentActivity;

    /**
     * Time of day context.
     */
    private final String timeOfDay;

    /**
     * Suggested focus area based on analysis.
     */
    private final String suggestedFocus;

    /**
     * User profile context.
     */
    @Getter
    @Builder
    public static class UserContext {
        private final String displayName;
        private final String skillLevel;
        private final String nativeLanguage;
        private final String targetLanguage;
        private final boolean assessmentCompleted;
    }

    /**
     * Goals context with progress.
     */
    @Getter
    @Builder
    public static class GoalsContext {
        private final List<GoalSummary> dailyGoals;
        private final List<GoalSummary> weeklyGoals;
        private final int totalDailyProgress;
        private final int totalDailyTarget;
        private final int completedToday;
    }

    /**
     * Summary of a single goal.
     */
    @Getter
    @Builder
    public static class GoalSummary {
        private final String title;
        private final int currentValue;
        private final int targetValue;
        private final String unit;
        private final boolean completed;
    }

    /**
     * Recent activity context.
     */
    @Getter
    @Builder
    public static class ActivityContext {
        private final String lastExerciseType;
        private final Integer lastScore;
        private final int exercisesToday;
        private final int dayStreak;
        private final double averageScore;
    }

    /**
     * Converts this context to a formatted string for the LLM system prompt.
     *
     * @return formatted context string
     */
    public String toSystemPromptContext() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== USER CONTEXT ===\n");
        sb.append("Name: ").append(user.getDisplayName()).append("\n");
        sb.append("Skill Level: ").append(user.getSkillLevel()).append(" (CEFR)\n");
        sb.append("Learning: ").append(user.getTargetLanguage())
                .append(" (native: ").append(user.getNativeLanguage()).append(")\n");
        sb.append("Assessment Completed: ").append(user.isAssessmentCompleted() ? "Yes" : "No").append("\n\n");
        
        sb.append("=== TODAY'S GOALS ===\n");
        if (goals.getDailyGoals() != null && !goals.getDailyGoals().isEmpty()) {
            for (GoalSummary goal : goals.getDailyGoals()) {
                String status = goal.isCompleted() ? "✓" : String.format("%d/%d", goal.getCurrentValue(), goal.getTargetValue());
                sb.append("- ").append(goal.getTitle()).append(": ").append(status)
                        .append(" ").append(goal.getUnit()).append("\n");
            }
            sb.append("Overall: ").append(goals.getCompletedToday()).append(" completed, ")
                    .append(goals.getTotalDailyProgress()).append("/").append(goals.getTotalDailyTarget()).append(" total\n\n");
        } else {
            sb.append("No daily goals set.\n\n");
        }
        
        sb.append("=== RECENT ACTIVITY ===\n");
        if (recentActivity != null) {
            if (recentActivity.getLastExerciseType() != null) {
                sb.append("Last exercise: ").append(recentActivity.getLastExerciseType())
                        .append(" (score: ").append(recentActivity.getLastScore()).append("%)\n");
            }
            sb.append("Exercises today: ").append(recentActivity.getExercisesToday()).append("\n");
            sb.append("Day streak: ").append(recentActivity.getDayStreak()).append(" days\n");
            sb.append("Average score: ").append(String.format("%.0f", recentActivity.getAverageScore())).append("%\n\n");
        }
        
        sb.append("=== SESSION INFO ===\n");
        sb.append("Time of day: ").append(timeOfDay).append("\n");
        if (suggestedFocus != null) {
            sb.append("Suggested focus: ").append(suggestedFocus).append("\n");
        }
        
        return sb.toString();
    }
}