package dev.languagelearning.learning.service;

import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management and profile operations.
 * <p>
 * Supports multi-user mode where the current user is determined by the
 * X-User-Id header sent with each request.
 */
public interface UserService {

    /**
     * Gets the current user based on the request context (X-User-Id header).
     * <p>
     * If no user ID is provided in the context, creates or returns a guest user
     * based on the configured default language settings.
     *
     * @return the current user
     */
    @Nonnull
    User getCurrentUser();

    /**
     * Lists all users in the system.
     *
     * @return list of all users
     */
    @Nonnull
    List<User> listAllUsers();

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user if found
     */
    @Nonnull
    Optional<User> findById(@Nonnull UUID id);

    /**
     * Checks if any users exist in the system.
     *
     * @return true if at least one user exists
     */
    boolean hasAnyUsers();

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

    /**
     * Updates user language settings.
     *
     * @param userId the user ID
     * @param nativeLanguage the native language code (ISO 639-1)
     * @param targetLanguage the target language code (ISO 639-1)
     * @return the updated user
     */
    @Nonnull
    User updateLanguages(@Nonnull UUID userId, @Nonnull String nativeLanguage, @Nonnull String targetLanguage);

    /**
     * Deletes a user and all associated data.
     * <p>
     * This cascades to delete all user's goals, chat sessions, exercise results, etc.
     *
     * @param userId the user ID to delete
     * @throws dev.languagelearning.core.exception.EntityNotFoundException if user not found
     */
    void deleteUser(@Nonnull UUID userId);

    /**
     * Creates or returns the default guest user based on configured language settings.
     * <p>
     * This is used when no users exist or when no user ID is provided in the request.
     *
     * @return the guest user
     */
    @Nonnull
    User getOrCreateGuestUser();
}
