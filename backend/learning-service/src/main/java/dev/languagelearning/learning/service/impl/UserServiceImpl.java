package dev.languagelearning.learning.service.impl;

import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.core.context.UserContextHolder;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.core.repository.UserRepository;
import dev.languagelearning.learning.service.UserService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserService with multi-user support.
 * <p>
 * Uses {@link UserContextHolder} to determine the current user from the
 * X-User-Id request header. Falls back to guest user when no user ID
 * is provided.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String GUEST_USER_NAME = "Guest";

    private final UserRepository userRepository;
    private final LanguageConfig languageConfig;
    private final UserContextHolder userContextHolder;

    @Override
    @Nonnull
    @Transactional
    public User getCurrentUser() {
        // Check if user ID is provided in request context
        if (userContextHolder.hasUserId()) {
            UUID userId = userContextHolder.getUserId();
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                return user.get();
            }
            log.warn("User ID {} from context not found in database, falling back to guest", userId);
        }

        // No user ID in context - return guest user
        return getOrCreateGuestUser();
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Nonnull
    public Optional<User> findById(@Nonnull UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean hasAnyUsers() {
        return userRepository.count() > 0;
    }

    @Override
    @Nonnull
    @Transactional
    public User createUser(@Nonnull String displayName, @Nonnull String nativeLanguage, @Nonnull String targetLanguage) {
        log.info("Creating new user: {} ({} -> {})", displayName, nativeLanguage, targetLanguage);

        User user = User.of(displayName, nativeLanguage, targetLanguage);
        user.setSkillLevel(SkillLevel.A1);
        user.setAssessmentCompleted(false);
        user.setTimezone("UTC");

        return userRepository.save(user);
    }

    @Override
    @Nonnull
    @Transactional
    public User updateSkillLevel(@Nonnull UUID userId, @Nonnull SkillLevel skillLevel) {
        log.info("Updating skill level for user {}: {}", userId, skillLevel);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        user.setSkillLevel(skillLevel);
        return userRepository.save(user);
    }

    @Override
    @Nonnull
    @Transactional
    public User completeAssessment(@Nonnull UUID userId) {
        log.info("Marking assessment complete for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        user.setAssessmentCompleted(true);
        return userRepository.save(user);
    }

    @Override
    @Nonnull
    @Transactional
    public User updateDisplayName(@Nonnull UUID userId, @Nonnull String displayName) {
        log.info("Updating display name for user {}: {}", userId, displayName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        user.setDisplayName(displayName);
        return userRepository.save(user);
    }

    @Override
    @Nonnull
    @Transactional
    public User updateLanguages(@Nonnull UUID userId, @Nonnull String nativeLanguage, @Nonnull String targetLanguage) {
        log.info("Updating languages for user {}: {} -> {}", userId, nativeLanguage, targetLanguage);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        user.setNativeLanguage(nativeLanguage);
        user.setTargetLanguage(targetLanguage);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(@Nonnull UUID userId) {
        log.info("Deleting user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        userRepository.delete(user);
        log.info("User {} deleted successfully", userId);
    }

    @Override
    @Nonnull
    @Transactional
    public User getOrCreateGuestUser() {
        // Look for existing guest user with matching language settings
        String nativeCode = languageConfig.getNativeCode();
        String targetCode = languageConfig.getTargetCode();

        Optional<User> existingGuest = userRepository.findAll().stream()
                .filter(u -> GUEST_USER_NAME.equals(u.getDisplayName()))
                .filter(u -> nativeCode.equals(u.getNativeLanguage()))
                .filter(u -> targetCode.equals(u.getTargetLanguage()))
                .findFirst();

        if (existingGuest.isPresent()) {
            log.debug("Found existing guest user: {}", existingGuest.get().getId());
            return existingGuest.get();
        }

        // Create new guest user with configured languages
        log.info("Creating guest user with languages {} -> {}", nativeCode, targetCode);
        return createUser(GUEST_USER_NAME, nativeCode, targetCode);
    }
}