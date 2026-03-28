package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating user profile.
 */
@Schema(description = "Request to update user profile")
public record UpdateUserRequest(
        @Schema(description = "New display name", example = "John Doe", nullable = true)
        @Nullable String displayName,

        @Schema(description = "New CEFR skill level (A1, A2, B1, B2, C1, C2)", example = "B1", nullable = true)
        @Nullable String skillLevel,

        @Schema(description = "ISO 639-1 code for native language", example = "de", nullable = true)
        @Nullable
        @Pattern(regexp = "^[a-z]{2}$", message = "Native language must be a valid ISO 639-1 code")
        String nativeLanguage,

        @Schema(description = "ISO 639-1 code for target language (language being learned)", example = "fr", nullable = true)
        @Nullable
        @Pattern(regexp = "^[a-z]{2}$", message = "Target language must be a valid ISO 639-1 code")
        String targetLanguage
) {}
