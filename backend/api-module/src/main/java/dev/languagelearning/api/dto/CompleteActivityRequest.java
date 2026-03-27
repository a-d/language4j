package dev.languagelearning.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request to complete an embedded activity.
 */
public record CompleteActivityRequest(
        @Min(value = 0, message = "Score must be at least 0")
        @Max(value = 100, message = "Score must not exceed 100")
        int score,

        @Size(max = 500, message = "Feedback must not exceed 500 characters")
        String feedback
) {}