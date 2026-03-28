package dev.languagelearning.content.service.impl;

import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.content.util.VocabularyJsonValidator;
import dev.languagelearning.core.domain.ExerciseGenerationType;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.prompts.LanguageLearningPrompts;
import dev.languagelearning.llm.service.LlmJsonGenerator;
import dev.languagelearning.learning.service.UserService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

    // ==================== Exercise Generation ====================

    @Override
    @Nonnull
    public String generateExercises(
            @Nonnull ExerciseGenerationType type,
            @Nonnull String topic,
            int count,
            Map<String, Object> options
    ) {
        User user = userService.getCurrentUser();
        log.info("Generating {} {} exercise(s) on topic '{}' for level {}",
                count, type.name(), topic, user.getSkillLevel());

        // Build base variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("nativeLanguage", languageConfig.getNativeName());
        variables.put("targetLanguage", languageConfig.getTargetName());
        variables.put("skillLevel", user.getSkillLevel().name());
        variables.put("topic", topic);

        // Add count with type-specific parameter name
        addCountVariable(type, count, variables);

        // Add type-specific options
        if (options != null) {
            addTypeSpecificOptions(type, options, variables);
        }

        // Get the appropriate prompt template
        PromptTemplate prompt = getPromptForExerciseType(type);

        // Generate and return
        return llmJsonGenerator.generateJson(prompt, variables);
    }

    /**
     * Adds the count variable with the appropriate parameter name for the exercise type.
     */
    private void addCountVariable(ExerciseGenerationType type, int count, Map<String, Object> variables) {
        switch (type) {
            case TEXT_COMPLETION -> variables.put("questionCount", count);
            case DRAG_DROP, TRANSLATION -> variables.put("sentenceCount", count);
            case LISTENING, SPEAKING -> variables.put("exerciseCount", count);
            case LISTENING_COMPREHENSION -> variables.put("statementCount", count);
            default -> variables.put("count", count);
        }
    }

    /**
     * Adds type-specific options to the variables map.
     */
    private void addTypeSpecificOptions(ExerciseGenerationType type, Map<String, Object> options, Map<String, Object> variables) {
        switch (type) {
            case LISTENING_COMPREHENSION -> {
                // Listening comprehension has wordCount and statementCount options
                if (options.containsKey("wordCount")) {
                    variables.put("wordCount", options.get("wordCount"));
                } else {
                    variables.put("wordCount", 100); // default
                }
                if (options.containsKey("statementCount")) {
                    variables.put("statementCount", options.get("statementCount"));
                }
            }
            // Add more type-specific option handling as needed
            default -> {
                // For other types, pass through any options that match prompt variables
                variables.putAll(options);
            }
        }
    }

    /**
     * Maps exercise types to their corresponding prompt templates.
     *
     * @throws UnsupportedOperationException if the exercise type is not yet implemented
     */
    private PromptTemplate getPromptForExerciseType(ExerciseGenerationType type) {
        return switch (type) {
            case TEXT_COMPLETION -> LanguageLearningPrompts.GENERATE_TEXT_COMPLETION;
            case DRAG_DROP -> LanguageLearningPrompts.GENERATE_DRAG_DROP;
            case TRANSLATION -> LanguageLearningPrompts.GENERATE_TRANSLATION_EXERCISE;
            case LISTENING -> LanguageLearningPrompts.GENERATE_LISTENING_EXERCISE;
            case LISTENING_COMPREHENSION -> LanguageLearningPrompts.GENERATE_LISTENING_COMPREHENSION;
            case SPEAKING -> LanguageLearningPrompts.GENERATE_SPEAKING_EXERCISE;
            // Future types - not yet implemented
            case WORD_SCRAMBLE, HANGMAN, CONJUGATION_DRILL, ARTICLE_PRACTICE,
                 MULTIPLE_CHOICE, SENTENCE_CORRECTION, DICTATION ->
                    throw new UnsupportedOperationException(
                            "Exercise type " + type.name() + " is not yet implemented. " +
                            "See LEARNING-ACTIVITIES.md for planned implementation details."
                    );
        };
    }

    // ==================== Lesson Generation ====================

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

    // ==================== Flashcards ====================

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

    // ==================== Roleplay ====================

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

    // ==================== Learning Plan ====================

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

    // ==================== Evaluation ====================

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

    // ==================== Visual Vocabulary ====================

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