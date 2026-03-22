package dev.languagelearning.learning.service.impl;

import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageConfig languageConfig;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private static final String NATIVE_CODE = "de";
    private static final String TARGET_CODE = "fr";

    @BeforeEach
    void setUp() {
        lenient().when(languageConfig.getNativeCode()).thenReturn(NATIVE_CODE);
        lenient().when(languageConfig.getTargetCode()).thenReturn(TARGET_CODE);
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("should create new user when no users exist")
        void shouldCreateNewUserWhenNoUsersExist() {
            // Given
            when(userRepository.findAll()).thenReturn(Collections.emptyList());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // When
            User result = userService.getCurrentUser();

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getDisplayName()).isEqualTo("Learner");
            assertThat(savedUser.getNativeLanguage()).isEqualTo(NATIVE_CODE);
            assertThat(savedUser.getTargetLanguage()).isEqualTo(TARGET_CODE);
            assertThat(savedUser.getSkillLevel()).isEqualTo(SkillLevel.A1);
            assertThat(savedUser.isAssessmentCompleted()).isFalse();
        }

        @Test
        @DisplayName("should return existing user with matching language settings")
        void shouldReturnExistingUserWithMatchingLanguageSettings() {
            // Given
            User existingUser = createUser(UUID.randomUUID(), "TestUser", NATIVE_CODE, TARGET_CODE);
            when(userRepository.findAll()).thenReturn(List.of(existingUser));

            // When
            User result = userService.getCurrentUser();

            // Then
            assertThat(result).isEqualTo(existingUser);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new user when existing users have different language settings")
        void shouldCreateNewUserWhenExistingUsersHaveDifferentLanguageSettings() {
            // Given
            User existingUser = createUser(UUID.randomUUID(), "TestUser", "en", "es");
            when(userRepository.findAll()).thenReturn(List.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // When
            User result = userService.getCurrentUser();

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getNativeLanguage()).isEqualTo(NATIVE_CODE);
            assertThat(savedUser.getTargetLanguage()).isEqualTo(TARGET_CODE);
        }

        @Test
        @DisplayName("should return cached user on subsequent calls")
        void shouldReturnCachedUserOnSubsequentCalls() {
            // Given
            UUID userId = UUID.randomUUID();
            User existingUser = createUser(userId, "TestUser", NATIVE_CODE, TARGET_CODE);
            when(userRepository.findAll()).thenReturn(List.of(existingUser));
            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

            // When - first call
            User firstResult = userService.getCurrentUser();
            // When - second call
            User secondResult = userService.getCurrentUser();

            // Then
            assertThat(firstResult).isEqualTo(secondResult);
            verify(userRepository, times(1)).findAll();
            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, "TestUser", NATIVE_CODE, TARGET_CODE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            Optional<User> result = userService.findById(userId);

            // Then
            assertThat(result).contains(user);
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findById(userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user with provided parameters")
        void shouldCreateUserWithProvidedParameters() {
            // Given
            String displayName = "NewUser";
            String nativeLanguage = "en";
            String targetLanguage = "de";
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // When
            User result = userService.createUser(displayName, nativeLanguage, targetLanguage);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getDisplayName()).isEqualTo(displayName);
            assertThat(savedUser.getNativeLanguage()).isEqualTo(nativeLanguage);
            assertThat(savedUser.getTargetLanguage()).isEqualTo(targetLanguage);
            assertThat(savedUser.getSkillLevel()).isEqualTo(SkillLevel.A1);
            assertThat(savedUser.isAssessmentCompleted()).isFalse();
            assertThat(savedUser.getTimezone()).isEqualTo("UTC");
            assertThat(savedUser.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateSkillLevel")
    class UpdateSkillLevel {

        @Test
        @DisplayName("should update skill level for existing user")
        void shouldUpdateSkillLevelForExistingUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, "TestUser", NATIVE_CODE, TARGET_CODE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.updateSkillLevel(userId, SkillLevel.B2);

            // Then
            assertThat(result.getSkillLevel()).isEqualTo(SkillLevel.B2);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.updateSkillLevel(userId, SkillLevel.B2))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("completeAssessment")
    class CompleteAssessment {

        @Test
        @DisplayName("should mark assessment as completed")
        void shouldMarkAssessmentAsCompleted() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, "TestUser", NATIVE_CODE, TARGET_CODE);
            assertThat(user.isAssessmentCompleted()).isFalse();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.completeAssessment(userId);

            // Then
            assertThat(result.isAssessmentCompleted()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.completeAssessment(userId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateDisplayName")
    class UpdateDisplayName {

        @Test
        @DisplayName("should update display name for existing user")
        void shouldUpdateDisplayNameForExistingUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, "OldName", NATIVE_CODE, TARGET_CODE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.updateDisplayName(userId, "NewName");

            // Then
            assertThat(result.getDisplayName()).isEqualTo("NewName");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.updateDisplayName(userId, "NewName"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    private User createUser(UUID id, String displayName, String nativeLanguage, String targetLanguage) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setNativeLanguage(nativeLanguage);
        user.setTargetLanguage(targetLanguage);
        user.setSkillLevel(SkillLevel.A1);
        user.setAssessmentCompleted(false);
        user.setTimezone("UTC");
        return user;
    }
}