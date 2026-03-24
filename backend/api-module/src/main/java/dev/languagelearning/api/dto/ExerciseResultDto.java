package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.ExerciseResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for exercise result data.
 *
 * @param id               unique identifier
 * @param exerciseType     type of exercise
 * @param exerciseReference reference to the exercise content
 * @param score            score achieved (0-100)
 * @param maxScore         maximum possible score
 * @param correctAnswers   number of correct answers
 * @param totalQuestions   total number of questions
 * @param timeSpentSeconds time spent in seconds
 * @param passed           whether the exercise was passed
 * @param skillLevel       skill level at the time of exercise
 * @param feedback         optional feedback
 * @param createdAt        when the result was recorded
 */
@Schema(description = "Exercise result data")
public record ExerciseResultDto(
        @Schema(description = "Unique identifier")
        UUID id,

        @Schema(description = "Type of exercise", example = "TEXT_COMPLETION")
        String exerciseType,

        @Schema(description = "Reference to the exercise content", example = "basic-greetings")
        String exerciseReference,

        @Schema(description = "Score achieved (0-100)", example = "80")
        int score,

        @Schema(description = "Maximum possible score", example = "100")
        int maxScore,

        @Schema(description = "Number of correct answers", example = "4")
        int correctAnswers,

        @Schema(description = "Total number of questions", example = "5")
        int totalQuestions,

        @Schema(description = "Time spent in seconds", example = "120")
        long timeSpentSeconds,

        @Schema(description = "Whether the exercise was passed (score >= 70)")
        boolean passed,

        @Schema(description = "Skill level at the time of exercise", example = "A1")
        String skillLevel,

        @Schema(description = "Feedback on the exercise")
        String feedback,

        @Schema(description = "When the result was recorded")
        Instant createdAt
) {
    /**
     * Creates a DTO from an ExerciseResult entity.
     *
     * @param entity the entity to convert
     * @return the DTO
     */
    public static ExerciseResultDto from(ExerciseResult entity) {
        return new ExerciseResultDto(
                entity.getId(),
                entity.getExerciseType().name(),
                entity.getExerciseReference(),
                entity.getScore(),
                entity.getMaxScore(),
                entity.getCorrectAnswers(),
                entity.getTotalQuestions(),
                entity.getTimeSpentSeconds(),
                entity.isPassed(),
                entity.getSkillLevelAtTime().name(),
                entity.getFeedback().orElse(null),
                entity.getCreatedAt()
        );
    }
}