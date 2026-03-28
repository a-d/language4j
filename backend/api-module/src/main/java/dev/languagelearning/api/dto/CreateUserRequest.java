package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user.
 *
 * @param displayName    the user's display name
 * @param nativeLanguage the user's native language code (ISO 639-1)
 * @param targetLanguage the language the user wants to learn (ISO 639-1)
 */
@Schema(description = "Request to create a new user profile")
public record CreateUserRequest(
        @Schema(description = "Display name for the user", example = "John")
        @NotBlank(message = "Display name is required")
        @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
        String displayName,

        @Schema(description = "Native language code (ISO 639-1)", example = "en")
        @NotBlank(message = "Native language is required")
        @Pattern(regexp = "^[a-z]{2}$", message = "Native language must be a valid ISO 639-1 code")
        String nativeLanguage,

        @Schema(description = "Target language code (ISO 639-1)", example = "de")
        @NotBlank(message = "Target language is required")
        @Pattern(regexp = "^[a-z]{2}$", message = "Target language must be a valid ISO 639-1 code")
        String targetLanguage
) {
}