package dev.languagelearning.api.dto;

import jakarta.annotation.Nullable;

/**
 * Request DTO for updating user profile.
 */
public record UpdateUserRequest(
        @Nullable String displayName,
        @Nullable String skillLevel
) {}