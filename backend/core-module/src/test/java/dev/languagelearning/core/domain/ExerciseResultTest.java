package dev.languagelearning.core.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExerciseResult}.
 */
class ExerciseResultTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setDisplayName("Test User");
        testUser.setNativeLanguage("de");
        testUser.setTargetLanguage("fr");
        testUser.setSkillLevel(SkillLevel.A1);
    }

    @Nested
    @DisplayName("Factory method of()")
    class FactoryMethod {

        @Test
        @DisplayName("should create exercise result with default passing threshold")
        void shouldCreateExerciseResultWithDefaultPassingThreshold() {
            // When
            ExerciseResult result = ExerciseResult.of(
                    testUser,
                    ExerciseType.TEXT_COMPLETION,
                    80,
                    Duration.ofMinutes(5),
                    SkillLevel.A1
            );

            // Then
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getExerciseType()).isEqualTo(ExerciseType.TEXT_COMPLETION);
            assertThat(result.getScore()).isEqualTo(80);
            assertThat(result.getTimeSpentSeconds()).isEqualTo(300);
            assertThat(result.getSkillLevelAtTime()).isEqualTo(SkillLevel.A1);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("should mark as passed when score is exactly 70")
        void shouldMarkAsPassedWhenScoreIsExactly70() {
            // When
            ExerciseResult result = ExerciseResult.of(
                    testUser,
                    ExerciseType.TRANSLATION,
                    70,
                    Duration.ofMinutes(3),
                    SkillLevel.B1
            );

            // Then
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("should mark as failed when score is below 70")
        void shouldMarkAsFailedWhenScoreBelowThreshold() {
            // When
            ExerciseResult result = ExerciseResult.of(
                    testUser,
                    ExerciseType.DRAG_DROP,
                    69,
                    Duration.ofMinutes(2),
                    SkillLevel.A2
            );

            // Then
            assertThat(result.isPassed()).isFalse();
        }

        @Test
        @DisplayName("should support all exercise types")
        void shouldSupportAllExerciseTypes() {
            for (ExerciseType type : ExerciseType.values()) {
                ExerciseResult result = ExerciseResult.of(
                        testUser, type, 85, Duration.ofMinutes(1), SkillLevel.A1
                );
                assertThat(result.getExerciseType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("Time spent handling")
    class TimeSpentHandling {

        @Test
        @DisplayName("should convert Duration to seconds")
        void shouldConvertDurationToSeconds() {
            // Given
            ExerciseResult result = new ExerciseResult();

            // When
            result.setTimeSpent(Duration.ofMinutes(2).plusSeconds(30));

            // Then
            assertThat(result.getTimeSpentSeconds()).isEqualTo(150);
        }

        @Test
        @DisplayName("should convert seconds to Duration")
        void shouldConvertSecondsToDuration() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setTimeSpentSeconds(90);

            // When
            Duration timeSpent = result.getTimeSpent();

            // Then
            assertThat(timeSpent).isEqualTo(Duration.ofSeconds(90));
            assertThat(timeSpent.toMinutes()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle zero duration")
        void shouldHandleZeroDuration() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setTimeSpent(Duration.ZERO);

            // Then
            assertThat(result.getTimeSpentSeconds()).isEqualTo(0);
            assertThat(result.getTimeSpent()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("should handle long durations")
        void shouldHandleLongDurations() {
            // Given
            ExerciseResult result = new ExerciseResult();
            Duration oneHour = Duration.ofHours(1);

            // When
            result.setTimeSpent(oneHour);

            // Then
            assertThat(result.getTimeSpentSeconds()).isEqualTo(3600);
            assertThat(result.getTimeSpent()).isEqualTo(oneHour);
        }
    }

    @Nested
    @DisplayName("Accuracy calculation")
    class AccuracyCalculation {

        @Test
        @DisplayName("should calculate accuracy percentage")
        void shouldCalculateAccuracyPercentage() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setCorrectAnswers(8);
            result.setTotalQuestions(10);

            // When
            int accuracy = result.getAccuracyPercentage();

            // Then
            assertThat(accuracy).isEqualTo(80);
        }

        @Test
        @DisplayName("should return 0 when total questions is zero")
        void shouldReturnZeroWhenTotalQuestionsIsZero() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setCorrectAnswers(0);
            result.setTotalQuestions(0);

            // When
            int accuracy = result.getAccuracyPercentage();

            // Then
            assertThat(accuracy).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle perfect score")
        void shouldHandlePerfectScore() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setCorrectAnswers(10);
            result.setTotalQuestions(10);

            // When
            int accuracy = result.getAccuracyPercentage();

            // Then
            assertThat(accuracy).isEqualTo(100);
        }

        @Test
        @DisplayName("should handle zero correct answers")
        void shouldHandleZeroCorrectAnswers() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setCorrectAnswers(0);
            result.setTotalQuestions(5);

            // When
            int accuracy = result.getAccuracyPercentage();

            // Then
            assertThat(accuracy).isEqualTo(0);
        }

        @Test
        @DisplayName("should truncate decimal accuracy")
        void shouldTruncateDecimalAccuracy() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setCorrectAnswers(1);
            result.setTotalQuestions(3); // 33.33%

            // When
            int accuracy = result.getAccuracyPercentage();

            // Then
            assertThat(accuracy).isEqualTo(33);
        }
    }

    @Nested
    @DisplayName("Normalized score calculation")
    class NormalizedScoreCalculation {

        @Test
        @DisplayName("should calculate normalized score")
        void shouldCalculateNormalizedScore() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setScore(75);
            result.setMaxScore(100);

            // When
            int normalized = result.getNormalizedScore();

            // Then
            assertThat(normalized).isEqualTo(75);
        }

        @Test
        @DisplayName("should normalize when max score is not 100")
        void shouldNormalizeWhenMaxScoreIsNot100() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setScore(15);
            result.setMaxScore(20);

            // When
            int normalized = result.getNormalizedScore();

            // Then
            assertThat(normalized).isEqualTo(75);
        }

        @Test
        @DisplayName("should return 0 when max score is zero")
        void shouldReturnZeroWhenMaxScoreIsZero() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setScore(10);
            result.setMaxScore(0);

            // When
            int normalized = result.getNormalizedScore();

            // Then
            assertThat(normalized).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle perfect normalized score")
        void shouldHandlePerfectNormalizedScore() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setScore(50);
            result.setMaxScore(50);

            // When
            int normalized = result.getNormalizedScore();

            // Then
            assertThat(normalized).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Feedback handling")
    class FeedbackHandling {

        @Test
        @DisplayName("should return empty Optional when feedback is null")
        void shouldReturnEmptyOptionalWhenFeedbackIsNull() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setFeedback(null);

            // When/Then
            assertThat(result.getFeedback()).isEmpty();
        }

        @Test
        @DisplayName("should return Optional with feedback when present")
        void shouldReturnOptionalWithFeedbackWhenPresent() {
            // Given
            ExerciseResult result = new ExerciseResult();
            result.setFeedback("Great job on this exercise!");

            // When/Then
            assertThat(result.getFeedback()).isPresent();
            assertThat(result.getFeedback().get()).isEqualTo("Great job on this exercise!");
        }
    }

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("should have default max score of 100")
        void shouldHaveDefaultMaxScoreOf100() {
            // Given
            ExerciseResult result = new ExerciseResult();

            // Then
            assertThat(result.getMaxScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("should have default correct answers of 0")
        void shouldHaveDefaultCorrectAnswersOfZero() {
            // Given
            ExerciseResult result = new ExerciseResult();

            // Then
            assertThat(result.getCorrectAnswers()).isEqualTo(0);
        }

        @Test
        @DisplayName("should have default total questions of 0")
        void shouldHaveDefaultTotalQuestionsOfZero() {
            // Given
            ExerciseResult result = new ExerciseResult();

            // Then
            assertThat(result.getTotalQuestions()).isEqualTo(0);
        }
    }
}