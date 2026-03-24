package dev.languagelearning.content.service;

import jakarta.annotation.Nonnull;

import java.util.List;
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
     * Generates fill-in-the-blank exercises.
     *
     * @param topic the exercise topic
     * @param questionCount number of questions
     * @return JSON string with exercises
     */
    @Nonnull
    String generateTextCompletionExercises(@Nonnull String topic, int questionCount);

    /**
     * Generates word-ordering (drag-drop) exercises.
     *
     * @param topic the exercise topic
     * @param sentenceCount number of sentences
     * @return JSON string with exercises
     */
    @Nonnull
    String generateDragDropExercises(@Nonnull String topic, int sentenceCount);

    /**
     * Generates translation exercises.
     *
     * @param topic the exercise topic
     * @param sentenceCount number of sentences
     * @return JSON string with exercises
     */
    @Nonnull
    String generateTranslationExercises(@Nonnull String topic, int sentenceCount);

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
     * Generates listening comprehension exercises.
     * <p>
     * Each exercise includes a sentence to be spoken via TTS, translation, and hint.
     *
     * @param topic the exercise topic
     * @param exerciseCount number of exercises
     * @return JSON string with exercises
     */
    @Nonnull
    String generateListeningExercises(@Nonnull String topic, int exerciseCount);

    /**
     * Generates speaking/pronunciation exercises.
     * <p>
     * Each exercise includes text to speak, translation, pronunciation tips, and common mistakes.
     *
     * @param topic the exercise topic
     * @param exerciseCount number of exercises
     * @return JSON string with exercises
     */
    @Nonnull
    String generateSpeakingExercises(@Nonnull String topic, int exerciseCount);

    /**
     * Evaluates pronunciation by comparing expected text with transcription.
     *
     * @param expectedText the text the student was supposed to say
     * @param transcription the transcribed speech from the student
     * @return JSON string with evaluation results including accuracy and tips
     */
    @Nonnull
    String evaluatePronunciation(@Nonnull String expectedText, @Nonnull String transcription);
}
