package dev.languagelearning.content.service.impl;

import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.content.util.VocabularyJsonValidator;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.prompts.LanguageLearningPrompts;
import dev.languagelearning.llm.service.LlmJsonGenerator;
import dev.languagelearning.learning.service.UserService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Default implementation of ContentGenerationService using LLM.
 * <p>
 * Uses {@link LlmJsonGenerator} for JSON content generation, which provides:
 * <ul>
 *   <li>Automatic JSON sanitization to fix LLM output issues</li>
 *   <li>Automatic retry on JSON parse failures</li>
 *   <li>Validation that output is parseable JSON</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentGenerationServiceImpl implements ContentGenerationService {

    private final LlmService llmService;
    private final LlmJsonGenerator llmJsonGenerator;
    private final UserService userService;
    private final LanguageConfig languageConfig;

    @Override
    @Nonnull
    public String generateLesson(@Nonnull String topic) {
        User user = userService.getCurrentUser();
        log.info("Generating lesson on topic '{}' for user {} at level {}", 
                topic, user.getId(), user.getSkillLevel());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic
        );

        return llmService.generate(LanguageLearningPrompts.GENERATE_LESSON, variables);
    }

    @Override
    @Nonnull
    public String generateVocabulary(@Nonnull String topic, int wordCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} vocabulary words on topic '{}' for level {}", 
                wordCount, topic, user.getSkillLevel());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "wordCount", wordCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        String json = llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_VOCABULARY_LIST, variables);
        return VocabularyJsonValidator.validateAndNormalize(json);
    }

    @Override
    @Nonnull
    public String generateTextCompletionExercises(@Nonnull String topic, int questionCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} text completion exercises on topic '{}'", questionCount, topic);

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "questionCount", questionCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_TEXT_COMPLETION, variables);
    }

    @Override
    @Nonnull
    public String generateDragDropExercises(@Nonnull String topic, int sentenceCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} drag-drop exercises on topic '{}'", sentenceCount, topic);

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "sentenceCount", sentenceCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_DRAG_DROP, variables);
    }

    @Override
    @Nonnull
    public String generateTranslationExercises(@Nonnull String topic, int sentenceCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} translation exercises on topic '{}'", sentenceCount, topic);

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "sentenceCount", sentenceCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_TRANSLATION_EXERCISE, variables);
    }

    @Override
    @Nonnull
    public String generateFlashcards(@Nonnull String topic, int cardCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} flashcards on topic '{}'", cardCount, topic);

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "cardCount", cardCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_FLASHCARDS, variables);
    }

    @Override
    @Nonnull
    public String generateRoleplayScenario(@Nonnull String scenario) {
        User user = userService.getCurrentUser();
        log.info("Generating roleplay scenario: '{}'", scenario);

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "scenario", scenario
        );

        return llmService.generate(LanguageLearningPrompts.GENERATE_ROLEPLAY_SCENARIO, variables);
    }

    @Override
    @Nonnull
    public String generateLearningPlan(@Nonnull String dailyGoal, @Nonnull String weeklyGoal, @Nonnull String monthlyGoal) {
        User user = userService.getCurrentUser();
        log.info("Generating learning plan for user {}", user.getId());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "dailyGoal", dailyGoal,
                "weeklyGoal", weeklyGoal,
                "monthlyGoal", monthlyGoal
        );

        return llmService.generate(LanguageLearningPrompts.CREATE_LEARNING_PLAN, variables);
    }

    @Override
    @Nonnull
    public String evaluateResponse(@Nonnull String exercise, @Nonnull String userResponse, @Nonnull String expectedAnswer) {
        User user = userService.getCurrentUser();
        log.info("Evaluating response for user {}", user.getId());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "exercise", exercise,
                "response", userResponse,
                "expected", expectedAnswer
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.EVALUATE_RESPONSE, variables);
    }

    @Override
    @Nonnull
    public String generateListeningExercises(@Nonnull String topic, int exerciseCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} listening exercises on topic '{}' for level {}", 
                exerciseCount, topic, user.getSkillLevel());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "exerciseCount", exerciseCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_LISTENING_EXERCISE, variables);
    }

    @Override
    @Nonnull
    public String generateSpeakingExercises(@Nonnull String topic, int exerciseCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} speaking exercises on topic '{}' for level {}", 
                exerciseCount, topic, user.getSkillLevel());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "exerciseCount", exerciseCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_SPEAKING_EXERCISE, variables);
    }

    @Override
    @Nonnull
    public String evaluatePronunciation(@Nonnull String expectedText, @Nonnull String transcription) {
        User user = userService.getCurrentUser();
        log.info("Evaluating pronunciation for user {}", user.getId());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "expected", expectedText,
                "transcription", transcription
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.EVALUATE_PRONUNCIATION, variables);
    }

    @Override
    @Nonnull
    public String generateVisualVocabulary(@Nonnull String topic, int wordCount) {
        User user = userService.getCurrentUser();
        log.info("Generating {} visual vocabulary words on topic '{}' for level {}",
                wordCount, topic, user.getSkillLevel());

        Map<String, Object> variables = Map.of(
                "nativeLanguage", languageConfig.getNativeName(),
                "targetLanguage", languageConfig.getTargetName(),
                "skillLevel", user.getSkillLevel().name(),
                "topic", topic,
                "wordCount", wordCount
        );

        // Use LlmJsonGenerator for validated JSON with retry
        return llmJsonGenerator.generateJson(LanguageLearningPrompts.GENERATE_VISUAL_VOCABULARY, variables);
    }
}
