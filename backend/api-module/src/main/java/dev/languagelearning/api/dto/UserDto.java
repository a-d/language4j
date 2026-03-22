package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.User;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user data.
 */
public record UserDto(
        UUID id,
        String displayName,
        String nativeLanguage,
        String targetLanguage,
        String skillLevel,
        boolean assessmentCompleted,
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