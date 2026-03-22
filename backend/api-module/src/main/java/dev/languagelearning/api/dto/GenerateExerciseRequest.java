package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating exercises.
 */
public record GenerateExerciseRequest(
        @Nonnull String topic,
        int questionCount
) {
    public GenerateExerciseRequest {
        if (questionCount <= 0) {
            questionCount = 5;
        }
    }
}