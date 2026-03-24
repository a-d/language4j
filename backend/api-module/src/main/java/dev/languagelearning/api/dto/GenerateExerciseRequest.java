package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating exercises.
 */
@Schema(description = "Request to generate exercises")
public record GenerateExerciseRequest(
        @Schema(description = "Topic for exercise generation", example = "past tense verbs", requiredMode = Schema.RequiredMode.REQUIRED)
        @Nonnull String topic,

        @Schema(description = "Number of questions to generate (default 5)", example = "5", minimum = "1", maximum = "20", defaultValue = "5")
        int questionCount
) {
    public GenerateExerciseRequest {
        if (questionCount <= 0) {
            questionCount = 5;
        }
    }
}