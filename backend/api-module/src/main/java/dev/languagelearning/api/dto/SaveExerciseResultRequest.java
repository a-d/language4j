package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for saving an exercise result.
 *
 * @param exerciseType     the type of exercise (TEXT_COMPLETION, DRAG_DROP, TRANSLATION, etc.)
 * @param exerciseReference optional reference to the exercise content
 * @param score            the score achieved (0-100)
 * @param correctAnswers   number of correct answers
 * @param totalQuestions   total number of questions
 * @param timeSpentSeconds time spent in seconds
 * @param userResponse     the user's response (JSON or text)
 * @param correctResponse  the correct response
 * @param feedback         optional AI feedback
 */
@Schema(description = "Request to save an exercise result")
public record SaveExerciseResultRequest(
        @NotNull
        @Schema(description = "Type of exercise", example = "TEXT_COMPLETION", 
                allowableValues = {"TEXT_COMPLETION", "DRAG_DROP", "TRANSLATION", "MULTIPLE_CHOICE", 
                        "LISTENING", "SPEAKING", "FLASHCARD", "ROLEPLAY", "DICTATION"})
        String exerciseType,

        @Schema(description = "Reference to the exercise content (e.g., topic or ID)", example = "basic-greetings")
        String exerciseReference,

        @NotNull
        @Min(0)
        @Max(100)
        @Schema(description = "Score achieved (0-100)", example = "80")
        Integer score,

        @NotNull
        @Min(0)
        @Schema(description = "Number of correct answers", example = "4")
        Integer correctAnswers,

        @NotNull
        @Min(0)
        @Schema(description = "Total number of questions", example = "5")
        Integer totalQuestions,

        @NotNull
        @Min(0)
        @Schema(description = "Time spent in seconds", example = "120")
        Long timeSpentSeconds,

        @Schema(description = "The user's responses (JSON array or text)")
        String userResponse,

        @Schema(description = "The correct responses (JSON array or text)")
        String correctResponse,

        @Schema(description = "Optional AI-generated feedback")
        String feedback
) {}