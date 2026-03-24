package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user data.
 */
@Schema(description = "User profile information")
public record UserDto(
        @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "User's display name", example = "John Doe")
        String displayName,

        @Schema(description = "ISO 639-1 language code for native language", example = "en")
        String nativeLanguage,

        @Schema(description = "ISO 639-1 language code for target language", example = "de")
        String targetLanguage,

        @Schema(description = "CEFR skill level (A1, A2, B1, B2, C1, C2)", example = "A1")
        String skillLevel,

        @Schema(description = "Whether the user has completed the initial skill assessment", example = "false")
        boolean assessmentCompleted,

        @Schema(description = "Account creation timestamp", example = "2024-01-15T10:30:00Z")
        Instant createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getDisplayName(),
                user.getNativeLanguage(),
                user.getTargetLanguage(),
                user.getSkillLevel().name(),
                user.isAssessmentCompleted(),
                user.getCreatedAt()
        );
    }
}