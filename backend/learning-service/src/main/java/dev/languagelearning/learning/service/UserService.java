package dev.languagelearning.learning.service;

import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import jakarta.annotation.Nonnull;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management and profile operations.
 */
public interface UserService {

    /**
     * Gets or creates the current user based on configuration.
     *
     * @return the current user
     */
    @Nonnull
    User getCurrentUser();

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user if found
     */
    @Nonnull
    Optional<User> findById(@Nonnull UUID id);

    /**
     * Creates a new user with the given display name.
     *
     * @param displayName the display name
     * @param nativeLanguage the native language code
     * @param targetLanguage the target language code
     * @return the created user
     */
    @Nonnull
    User createUser(@Nonnull String displayName, @Nonnull String nativeLanguage, @Nonnull String targetLanguage);

    /**
     * Updates the user's skill level.
     *
     * @param userId the user ID
     * @param skillLevel the new skill level
     * @return the updated user
     */
    @Nonnull
    User updateSkillLevel(@Nonnull UUID userId, @Nonnull SkillLevel skillLevel);

    /**
     * Marks the user's assessment as completed.
     *
     * @param userId the user ID
     * @return the updated user
     */
    @Nonnull
    User completeAssessment(@Nonnull UUID userId);

    /**
     * Updates user display name.
     *
     * @param userId the user ID
     * @param displayName the new display name
     * @return the updated user
     */
    @Nonnull
    User updateDisplayName(@Nonnull UUID userId, @Nonnull String displayName);
}