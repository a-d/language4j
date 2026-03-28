package dev.languagelearning.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of exercises that can be generated via the unified exercise generation API.
 * <p>
 * Each type has a default count and maps to a specific LLM prompt template.
 */
@Getter
@RequiredArgsConstructor
public enum ExerciseGenerationType {

    /**
     * Fill-in-the-blank exercises for grammar and vocabulary practice.
     */
    TEXT_COMPLETION(5, "Fill-in-the-blank exercises"),

    /**
     * Word-ordering exercises where users arrange words to form sentences.
     */
    DRAG_DROP(5, "Word ordering exercises"),

    /**
     * Translation exercises from native to target language.
     */
    TRANSLATION(5, "Translation exercises"),

    /**
     * Listening exercises where users transcribe what they hear.
     */
    LISTENING(5, "Listening transcription exercises"),

    /**
     * Listening comprehension with a story and true/false statements.
     */
    LISTENING_COMPREHENSION(1, "Listening comprehension with story"),

    /**
     * Speaking/pronunciation exercises with model audio.
     */
    SPEAKING(5, "Speaking and pronunciation exercises"),

    // ==================== Future Types ====================
    // These are planned for future implementation

    /**
     * Unscramble letters to form the correct word.
     */
    WORD_SCRAMBLE(5, "Word scramble puzzles"),

    /**
     * Classic word guessing game with limited attempts.
     */
    HANGMAN(1, "Hangman word guessing"),

    /**
     * Rapid-fire verb conjugation practice.
     */
    CONJUGATION_DRILL(10, "Verb conjugation drills"),

    /**
     * Practice noun genders by selecting correct articles.
     */
    ARTICLE_PRACTICE(10, "Article/gender practice"),

    /**
     * Classic multiple choice questions.
     */
    MULTIPLE_CHOICE(5, "Multiple choice quiz"),

    /**
     * Find and fix grammatical or spelling errors.
     */
    SENTENCE_CORRECTION(5, "Sentence correction exercises"),

    /**
     * Full dictation - listen and write down sentences/paragraphs.
     */
    DICTATION(3, "Dictation exercises");

    /**
     * Default number of exercises to generate if not specified.
     */
    private final int defaultCount;

    /**
     * Human-readable description of the exercise type.
     */
    private final String description;
}