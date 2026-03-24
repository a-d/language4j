package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

/**
 * Request DTO for updating user profile.
 */
@Schema(description = "Request to update user profile")
public record UpdateUserRequest(
        @Schema(description = "New display name", example = "John Doe", nullable = true)
        @Nullable String displayName,

        @Schema(description = "New CEFR skill level (A1, A2, B1, B2, C1, C2)", example = "B1", nullable = true)
        @Nullable String skillLevel
) {}