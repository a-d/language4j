package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating roleplay scenarios.
 */
public record GenerateScenarioRequest(
        @Nonnull String scenario
) {}