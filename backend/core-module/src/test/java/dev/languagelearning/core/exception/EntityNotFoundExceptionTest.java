package dev.languagelearning.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EntityNotFoundException}.
 */
class EntityNotFoundExceptionTest {

    @Nested
    @DisplayName("Constructor with entity type and UUID")
    class ConstructorWithUUID {

        @Test
        @DisplayName("should format message with entity type and UUID")
        void shouldFormatMessageWithEntityTypeAndUUID() {
            // Given
            UUID id = UUID.randomUUID();

            // When
            EntityNotFoundException exception = new EntityNotFoundException("User", id);

            // Then
            assertThat(exception.getMessage()).isEqualTo("User not found with id: " + id);
        }
    }

    @Nested
    @DisplayName("Constructor with entity type and string identifier")
    class ConstructorWithStringIdentifier {

        @Test
        @DisplayName("should format message with entity type and identifier")
        void shouldFormatMessageWithEntityTypeAndIdentifier() {
            // When
            EntityNotFoundException exception = new EntityNotFoundException("Lesson", "lesson-abc-123");

            // Then
            assertThat(exception.getMessage()).isEqualTo("Lesson not found: lesson-abc-123");
        }
    }

    @Nested
    @DisplayName("Constructor with simple message")
    class ConstructorWithSimpleMessage {

        @Test
        @DisplayName("should use message as-is")
        void shouldUseMessageAsIs() {
            // When
            EntityNotFoundException exception = new EntityNotFoundException("Custom not found message");

            // Then
            assertThat(exception.getMessage()).isEqualTo("Custom not found message");
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("userNotFound should create exception with User type")
        void userNotFoundShouldCreateExceptionWithUserType() {
            // Given
            UUID id = UUID.randomUUID();

            // When
            EntityNotFoundException exception = EntityNotFoundException.userNotFound(id);

            // Then
            assertThat(exception.getMessage()).contains("User");
            assertThat(exception.getMessage()).contains(id.toString());
        }

        @Test
        @DisplayName("goalNotFound should create exception with Learning goal type")
        void goalNotFoundShouldCreateExceptionWithGoalType() {
            // Given
            UUID id = UUID.randomUUID();

            // When
            EntityNotFoundException exception = EntityNotFoundException.goalNotFound(id);

            // Then
            assertThat(exception.getMessage()).contains("Learning goal");
            assertThat(exception.getMessage()).contains(id.toString());
        }

        @Test
        @DisplayName("contentNotFound should create exception with Content type")
        void contentNotFoundShouldCreateExceptionWithContentType() {
            // Given
            String reference = "vocab-list-2024";

            // When
            EntityNotFoundException exception = EntityNotFoundException.contentNotFound(reference);

            // Then
            assertThat(exception.getMessage()).contains("Content");
            assertThat(exception.getMessage()).contains(reference);
        }
    }

    @Nested
    @DisplayName("Exception hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("should extend LanguageLearningException")
        void shouldExtendLanguageLearningException() {
            // When
            EntityNotFoundException exception = new EntityNotFoundException("Test");

            // Then
            assertThat(exception).isInstanceOf(LanguageLearningException.class);
        }

        @Test
        @DisplayName("should be a RuntimeException")
        void shouldBeRuntimeException() {
            // When
            EntityNotFoundException exception = new EntityNotFoundException("Test");

            // Then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}