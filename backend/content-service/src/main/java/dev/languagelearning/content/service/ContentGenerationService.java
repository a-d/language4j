package dev.languagelearning.content.service;

import dev.languagelearning.core.domain.ExerciseGenerationType;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Service for generating learning content using LLM.
 */
public interface ContentGenerationService {

    /**
     * Generates a lesson on the specified topic.
     *
     * @param topic the lesson topic
     * @return the generated lesson content in markdown format
     */
    @Nonnull
    String generateLesson(@Nonnull String topic);

    /**
     * Generates a vocabulary list for the specified topic.
     *
     * @param topic the vocabulary topic
     * @param wordCount number of words to generate
     * @return the generated vocabulary in markdown format
     */
    @Nonnull
    String generateVocabulary(@Nonnull String topic, int wordCount);

    /**
     * Generates exercises of any type using the unified generation API.
     * <p>
     * This is the single method for all exercise generation, supporting multiple types:
     * <ul>
     *   <li>{@link ExerciseGenerationType#TEXT_COMPLETION} - Fill-in-the-blank exercises</li>
     *   <li>{@link ExerciseGenerationType#DRAG_DROP} - Word-ordering exercises</li>
     *   <li>{@link ExerciseGenerationType#TRANSLATION} - Translation exercises</li>
     *   <li>{@link ExerciseGenerationType#LISTENING} - Listen and transcribe exercises</li>
     *   <li>{@link ExerciseGenerationType#LISTENING_COMPREHENSION} - Story with true/false statements</li>
     *   <li>{@link ExerciseGenerationType#SPEAKING} - Pronunciation exercises</li>
     * </ul>
     *
     * @param type the type of exercise to generate
     * @param topic the exercise topic
     * @param count number of exercises to generate
     * @param options type-specific options (may be null). For LISTENING_COMPREHENSION,
     *                supports "wordCount" and "statementCount" options.
     * @return JSON string with exercises
     * @throws UnsupportedOperationException if the exercise type is not yet implemented
     */
    @Nonnull
    String generateExercises(
            @Nonnull ExerciseGenerationType type,
            @Nonnull String topic,
            int count,
            Map<String, Object> options
    );

    /**
     * Generates flashcard data for vocabulary.
     *
     * @param topic the topic
     * @param cardCount number of cards
     * @return JSON string with flashcard data
     */
    @Nonnull
    String generateFlashcards(@Nonnull String topic, int cardCount);

    /**
     * Generates a roleplay scenario.
     *
     * @param scenario the scenario description
     * @return the scenario content in markdown format
     */
    @Nonnull
    String generateRoleplayScenario(@Nonnull String scenario);

    /**
     * Generates a learning plan based on user preferences.
     *
     * @param dailyGoal daily goal description
     * @param weeklyGoal weekly goal description
     * @param monthlyGoal monthly goal description
     * @return the learning plan in markdown format
     */
    @Nonnull
    String generateLearningPlan(@Nonnull String dailyGoal, @Nonnull String weeklyGoal, @Nonnull String monthlyGoal);

    /**
     * Evaluates a user's exercise response.
     *
     * @param exercise the exercise text
     * @param userResponse the user's response
     * @param expectedAnswer the expected answer
     * @return JSON string with evaluation results
     */
    @Nonnull
    String evaluateResponse(@Nonnull String exercise, @Nonnull String userResponse, @Nonnull String expectedAnswer);

    /**
     * Evaluates pronunciation by comparing expected text with transcription.
     *
     * @param expectedText the text the student was supposed to say
     * @param transcription the transcribed speech from the student
     * @return JSON string with evaluation results including accuracy and tips
     */
    @Nonnull
    String evaluatePronunciation(@Nonnull String expectedText, @Nonnull String transcription);

    /**
     * Generates vocabulary for visual learning cards.
     * <p>
     * Each item includes native word, target word, pronunciation,
     * example sentence, and image description for AI image generation.
     *
     * @param topic the vocabulary topic
     * @param wordCount number of words to generate
     * @return JSON string with vocabulary for visual cards
     */
    @Nonnull
    String generateVisualVocabulary(@Nonnull String topic, int wordCount);
}