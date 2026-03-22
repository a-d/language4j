package dev.languagelearning.content.service.impl;

import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.UserService;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.prompts.LanguageLearningPrompts;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContentGenerationServiceImpl}.
 * <p>
 * Note: The service implementation uses getNativeName() and getTargetName()
 * which may not exist in LanguageConfig. Tests mock these methods to work
 * with the expected implementation.
 */
@ExtendWith(MockitoExtension.class)
class ContentGenerationServiceImplTest {

    @Mock
    private LlmService llmService;

    @Mock
    private UserService userService;

    @Mock
    private LanguageConfig languageConfig;

    @InjectMocks
    private ContentGenerationServiceImpl contentGenerationService;

    @Captor
    private ArgumentCaptor<PromptTemplate> templateCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    private User testUser;

    private static final String NATIVE_LANGUAGE = "German";
    private static final String TARGET_LANGUAGE = "French";
    private static final String GENERATED_CONTENT = "Generated LLM content";

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
        lenient().when(languageConfig.getNativeName()).thenReturn(NATIVE_LANGUAGE);
        lenient().when(languageConfig.getTargetName()).thenReturn(TARGET_LANGUAGE);
        lenient().when(llmService.generate(any(PromptTemplate.class), any())).thenReturn(GENERATED_CONTENT);
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
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_VOCABULARY_LIST), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("wordCount", wordCount);
        }
    }

    @Nested
    @DisplayName("generateTextCompletionExercises")
    class GenerateTextCompletionExercises {

        @Test
        @DisplayName("should generate text completion exercises with correct parameters")
        void shouldGenerateTextCompletionExercisesWithCorrectParameters() {
            // Given
            String topic = "Daily Routines";
            int questionCount = 5;

            // When
            String result = contentGenerationService.generateTextCompletionExercises(topic, questionCount);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_TEXT_COMPLETION), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("questionCount", questionCount);
        }
    }

    @Nested
    @DisplayName("generateDragDropExercises")
    class GenerateDragDropExercises {

        @Test
        @DisplayName("should generate drag-drop exercises with correct parameters")
        void shouldGenerateDragDropExercisesWithCorrectParameters() {
            // Given
            String topic = "Shopping";
            int sentenceCount = 8;

            // When
            String result = contentGenerationService.generateDragDropExercises(topic, sentenceCount);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_DRAG_DROP), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("sentenceCount", sentenceCount);
        }
    }

    @Nested
    @DisplayName("generateTranslationExercises")
    class GenerateTranslationExercises {

        @Test
        @DisplayName("should generate translation exercises with correct parameters")
        void shouldGenerateTranslationExercisesWithCorrectParameters() {
            // Given
            String topic = "At the Restaurant";
            int sentenceCount = 6;

            // When
            String result = contentGenerationService.generateTranslationExercises(topic, sentenceCount);

            // Then
            assertThat(result).isEqualTo(GENERATED_CONTENT);
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_TRANSLATION_EXERCISE), variablesCaptor.capture());

            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("topic", topic);
            assertThat(variables).containsEntry("sentenceCount", sentenceCount);
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
            verify(llmService).generate(eq(LanguageLearningPrompts.GENERATE_FLASHCARDS), variablesCaptor.capture());

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
            verify(llmService).generate(eq(LanguageLearningPrompts.EVALUATE_RESPONSE), variablesCaptor.capture());

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
            verify(llmService).generate(any(PromptTemplate.class), variablesCaptor.capture());
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