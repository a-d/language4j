package dev.languagelearning.content.service.impl;

import dev.languagelearning.core.domain.ExerciseGenerationType;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.UserService;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.prompts.LanguageLearningPrompts;
import dev.languagelearning.llm.service.LlmJsonGenerator;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContentGenerationServiceImpl}.
 * <p>
 * The service uses the User object's getNativeLanguageName() and getTargetLanguageName()
 * methods to get language names from ISO codes (e.g., "de" -> "German").
 */
@ExtendWith(MockitoExtension.class)
class ContentGenerationServiceImplTest {

    @Mock
    private LlmService llmService;

    @Mock
    private LlmJsonGenerator llmJsonGenerator;

    @Mock
    private UserService userService;

    @InjectMocks
    private ContentGenerationServiceImpl contentGenerationService;

    @Captor
    private ArgumentCaptor<PromptTemplate> templateCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    private User testUser;

    // User has nativeLanguage="de" which maps to "German"
    // User has targetLanguage="fr" which maps to "French"
    private static final String NATIVE_LANGUAGE = "German";
    private static final String TARGET_LANGUAGE = "French";
    private static final String GENERATED_CONTENT = "Generated LLM content";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
        lenient().when(llmService.generate(any(PromptTemplate.class), any())).thenReturn(GENERATED_CONTENT);
        lenient().when(llmJsonGenerator.generateJson(any(PromptTemplate.class), any())).thenReturn(GENERATED_CONTENT);
    }

    @Nested
    @DisplayName("generateLesson")
    class GenerateLesson {

        @Test
        @DisplayName("should generate lesson with correct parameters")
        void shouldGenerateLessonWithCorrectParameters() {
            // Given
            String topic = "Greetings";

            // When
            String result = contentGenerationService.generateLesson(topic);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_LESSON), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("nativeLanguage", NATIVE_LANGUAGE);
            assertThat(variables).containsEntry("targetLanguage", TARGET_LANGUAGE);
            assertThat(variables).containsEntry("skillLevel", SkillLevel.A1.name());
            assertThat(variables).containsEntry("topic", topic);
        }

        @Test
        @DisplayName("should use current user's skill level")
        void shouldUseCurrentUsersSkillLevel() {
            // Given
            testUser.setSkillLevel(SkillLevel.B2);

            // When
            contentGenerationService.generateLesson("Travel");

            // Then
            verify(llmService).generate(any(PromptTemplate.class), variablesCaptor.capture());
            assertThat(variablesCaptor.getValue()).containsEntry("skillLevel", "B2");
        }
    }

    @Nested
    @DisplayName("generateVocabulary")
    class GenerateVocabulary {

        @Test
        @DisplayName("should generate vocabulary with correct parameters")
        void shouldGenerateVocabularyWithCorrectParameters() {
            // Given
            String topic = "Food";
            int wordCount = 15;

            // When
            String result = contentGenerationService.generateVocabulary(topic, wordCount);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_VOCABULARY_LIST), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("wordCount", wordCount);
        }
    }

    @Nested
    @DisplayName("generateExercises")
    class GenerateExercises {

        @Test
        @DisplayName("should generate text completion exercises with correct parameters")
        void shouldGenerateTextCompletionExercisesWithCorrectParameters() {
            // Given
            String topic = "Daily Routines";
            int questionCount = 5;

            // When
            String result = contentGenerationService.generateExercises(
                    ExerciseGenerationType.TEXT_COMPLETION, topic, questionCount, null);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_TEXT_COMPLETION), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("questionCount", questionCount);
        }

        @Test
        @DisplayName("should generate drag-drop exercises with correct parameters")
        void shouldGenerateDragDropExercisesWithCorrectParameters() {
            // Given
            String topic = "Shopping";
            int sentenceCount = 8;

            // When
            String result = contentGenerationService.generateExercises(
                    ExerciseGenerationType.DRAG_DROP, topic, sentenceCount, null);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_DRAG_DROP), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("sentenceCount", sentenceCount);
        }

        @Test
        @DisplayName("should generate translation exercises with correct parameters")
        void shouldGenerateTranslationExercisesWithCorrectParameters() {
            // Given
            String topic = "At the Restaurant";
            int sentenceCount = 6;

            // When
            String result = contentGenerationService.generateExercises(
                    ExerciseGenerationType.TRANSLATION, topic, sentenceCount, null);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_TRANSLATION_EXERCISE), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("sentenceCount", sentenceCount);
        }

        @Test
        @DisplayName("should throw exception for unimplemented exercise type")
        void shouldThrowExceptionForUnimplementedExerciseType() {
            // Given
            String topic = "Test";

            // When/Then
            assertThatThrownBy(() -> 
                    contentGenerationService.generateExercises(ExerciseGenerationType.HANGMAN, topic, 5, null))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("HANGMAN")
                    .hasMessageContaining("not yet implemented");
        }

        @Test
        @DisplayName("should handle listening comprehension with options")
        void shouldHandleListeningComprehensionWithOptions() {
            // Given
            String topic = "Family";
            Map<String, Object> options = Map.of("wordCount", 150, "statementCount", 6);

            // When
            String result = contentGenerationService.generateExercises(
                    ExerciseGenerationType.LISTENING_COMPREHENSION, topic, 1, options);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_LISTENING_COMPREHENSION), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("wordCount", 150);
            assertThat(variables).containsEntry("statementCount", 6);
        }
    }

    @Nested
    @DisplayName("generateFlashcards")
    class GenerateFlashcards {

        @Test
        @DisplayName("should generate flashcards with correct parameters")
        void shouldGenerateFlashcardsWithCorrectParameters() {
            // Given
            String topic = "Colors";
            int cardCount = 12;

            // When
            String result = contentGenerationService.generateFlashcards(topic, cardCount);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.GENERATE_FLASHCARDS), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("cardCount", cardCount);
        }
    }

    @Nested
    @DisplayName("generateRoleplayScenario")
    class GenerateRoleplayScenario {

        @Test
        @DisplayName("should generate roleplay scenario with correct parameters")
        void shouldGenerateRoleplayScenarioWithCorrectParameters() {
            // Given
            String scenario = "Ordering coffee at a café";

            // When
            String result = contentGenerationService.generateRoleplayScenario(scenario);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_ROLEPLAY_SCENARIO), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("scenario", scenario);
            assertThat(variables).containsEntry("nativeLanguage", NATIVE_LANGUAGE);
            assertThat(variables).containsEntry("targetLanguage", TARGET_LANGUAGE);
            assertThat(variables).containsEntry("skillLevel", SkillLevel.A1.name());
        }
    }

    @Nested
    @DisplayName("generateLearningPlan")
    class GenerateLearningPlan {

        @Test
        @DisplayName("should generate learning plan with all goals")
        void shouldGenerateLearningPlanWithAllGoals() {
            // Given
            String dailyGoal = "Learn 10 new words";
            String weeklyGoal = "Complete 3 lessons";
            String monthlyGoal = "Hold a basic conversation";

            // When
            String result = contentGenerationService.generateLearningPlan(dailyGoal, weeklyGoal, monthlyGoal);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.CREATE_LEARNING_PLAN), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("dailyGoal", dailyGoal);
            assertThat(variables).containsEntry("weeklyGoal", weeklyGoal);
            assertThat(variables).containsEntry("monthlyGoal", monthlyGoal);
            assertThat(variables).containsEntry("nativeLanguage", NATIVE_LANGUAGE);
            assertThat(variables).containsEntry("targetLanguage", TARGET_LANGUAGE);
            assertThat(variables).containsEntry("skillLevel", SkillLevel.A1.name());
        }
    }

    @Nested
    @DisplayName("evaluateResponse")
    class EvaluateResponse {

        @Test
        @DisplayName("should evaluate response with correct parameters")
        void shouldEvaluateResponseWithCorrectParameters() {
            // Given
            String exercise = "Fill in the blank: Je ___ français.";
            String userResponse = "parle";
            String expectedAnswer = "parle";

            // When
            String result = contentGenerationService.evaluateResponse(exercise, userResponse, expectedAnswer);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmJsonGenerator).generateJson(eq(LanguageLearningPrompts.EVALUATE_RESPONSE), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("exercise", exercise);
            assertThat(variables).containsEntry("response", userResponse);
            assertThat(variables).containsEntry("expected", expectedAnswer);
        }

        @Test
        @DisplayName("should include user context in evaluation")
        void shouldIncludeUserContextInEvaluation() {
            // Given
            testUser.setSkillLevel(SkillLevel.C1);
            String exercise = "Translate: The weather is beautiful today.";
            String userResponse = "Il fait beau aujourd'hui.";
            String expectedAnswer = "Il fait beau aujourd'hui.";

            // When
            contentGenerationService.evaluateResponse(exercise, userResponse, expectedAnswer);

            // Then
            verify(llmJsonGenerator).generateJson(any(PromptTemplate.class), variablesCaptor.capture());
            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("skillLevel", "C1");
            assertThat(variables).containsEntry("nativeLanguage", NATIVE_LANGUAGE);
            assertThat(variables).containsEntry("targetLanguage", TARGET_LANGUAGE);
        }
    }

    @Nested
    @DisplayName("Integration scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("should always fetch current user for each operation")
        void shouldAlwaysFetchCurrentUserForEachOperation() {
            // Given/When
            contentGenerationService.generateLesson("Topic 1");
            contentGenerationService.generateVocabulary("Topic 2", 10);
            contentGenerationService.generateFlashcards("Topic 3", 5);

            // Then
            verify(userService, times(3)).getCurrentUser();
        }

        @Test
        @DisplayName("should reflect skill level changes between calls")
        void shouldReflectSkillLevelChangesBetweenCalls() {
            // Given
            testUser.setSkillLevel(SkillLevel.A1);

            // When - first call
            contentGenerationService.generateLesson("Basics");
            
            // Change skill level
            testUser.setSkillLevel(SkillLevel.B1);
            
            // When - second call
            contentGenerationService.generateLesson("Intermediate");

            // Then
            verify(llmService, times(2)).generate(any(PromptTemplate.class), variablesCaptor.capture());
            
            // Get all captured values
            var allValues = variablesCaptor.getAllValues();
            assertThat(allValues.get(0)).containsEntry("skillLevel", "A1");
            assertThat(allValues.get(1)).containsEntry("skillLevel", "B1");
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setDisplayName("Test User");
        user.setNativeLanguage("de");
        user.setTargetLanguage("fr");
        user.setSkillLevel(SkillLevel.A1);
        user.setAssessmentCompleted(false);
        user.setTimezone("UTC");
        return user;
    }
}