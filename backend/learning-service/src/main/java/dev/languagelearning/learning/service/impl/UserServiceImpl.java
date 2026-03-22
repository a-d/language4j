package dev.languagelearning.learning.service.impl;

import dev.languagelearning.config.LanguageConfig;
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

import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of UserService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LanguageConfig languageConfig;

    // Single-user mode: stores the current user ID
    private UUID currentUserId;

    @Override
    @Nonnull
    @Transactional
    public User getCurrentUser() {
        if (currentUserId != null) {
            Optional<User> existing = userRepository.findById(currentUserId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // Check if a user already exists with these language settings
        Optional<User> existingUser = userRepository.findAll().stream()
                .filter(u -> u.getNativeLanguage().equals(languageConfig.getNativeCode()) &&
                             u.getTargetLanguage().equals(languageConfig.getTargetCode()))
                .findFirst();

        if (existingUser.isPresent()) {
            currentUserId = existingUser.get().getId();
            return existingUser.get();
        }

        // Create new user
        User newUser = createUser("Learner", languageConfig.getNativeCode(), languageConfig.getTargetCode());
        currentUserId = newUser.getId();
        return newUser;
    }

    @Override
    @Nonnull
    public Optional<User> findById(@Nonnull UUID id) {
        return userRepository.findById(id);
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
}