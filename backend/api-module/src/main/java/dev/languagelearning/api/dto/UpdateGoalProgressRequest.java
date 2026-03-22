package dev.languagelearning.api.dto;

/**
 * Request DTO for updating goal progress.
 */
public record UpdateGoalProgressRequest(
        int currentValue
) {}