package dev.languagelearning.core.domain;

/**
 * Types of learning activities that can be embedded within chat messages.
 * <p>
 * These correspond to the content types that can be rendered inline
 * in the chat interface.
 */
public enum EmbeddedActivityType {
    /**
     * Vocabulary list with word cards.
     */
    VOCABULARY,

    /**
     * Flashcard deck for review.
     */
    FLASHCARDS,

    /**
     * Visual learning cards with images.
     */
    VISUAL_CARDS,

    /**
     * Fill-in-the-blank exercises.
     */
    TEXT_COMPLETION,

    /**
     * Word ordering / drag-drop exercises.
     */
    DRAG_DROP,

    /**
     * Translation exercises.
     */
    TRANSLATION,

    /**
     * Listening comprehension exercises (transcription).
     */
    LISTENING,

    /**
     * Listening comprehension story with true/false statements.
     * User listens to a story and answers comprehension questions.
     */
    LISTENING_COMPREHENSION,

    /**
     * Speaking/pronunciation exercises.
     */
    SPEAKING,

    /**
     * Lesson content in markdown.
     */
    LESSON,

    /**
     * Roleplay scenario.
     */
    SCENARIO,

    /**
     * Learning plan overview.
     */
    LEARNING_PLAN,

    /**
     * Activity completion summary.
     */
    SUMMARY,

    /**
     * Pair matching exercise (two columns).
     * Match source words with target words.
     */
    PAIR_MATCHING,

    /**
     * Memory card game.
     * Find matching word pairs by flipping cards.
     */
    MEMORY_GAME
}
