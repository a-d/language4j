package dev.languagelearning.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of learning exercises available in the platform.
 */
@Getter
@RequiredArgsConstructor
public enum ExerciseType {

    /**
     * Text completion exercise - fill in the blanks.
     */
    TEXT_COMPLETION("Text Completion", "Fill in the missing words or phrases"),

    /**
     * Drag and drop exercise - arrange words to form sentences.
     */
    DRAG_DROP("Drag & Drop", "Arrange words or phrases in the correct order"),

    /**
     * Multiple choice exercise - select the correct answer.
     */
    MULTIPLE_CHOICE("Multiple Choice", "Select the correct answer from options"),

    /**
     * Translation exercise - translate text between languages.
     */
    TRANSLATION("Translation", "Translate the given text"),

    /**
     * Listening exercise - transcribe or answer questions about audio.
     */
    LISTENING("Listening", "Listen and respond to audio content"),

    /**
     * Speaking exercise - record pronunciation for assessment.
     */
    SPEAKING("Speaking", "Speak and record your pronunciation"),

    /**
     * Flashcard review - spaced repetition vocabulary review.
     */
    FLASHCARD("Flashcard", "Review vocabulary using flashcards"),

    /**
     * Roleplay exercise - interactive conversation practice.
     */
    ROLEPLAY("Roleplay", "Practice conversation in realistic scenarios"),

    /**
     * Dictation exercise - write what you hear.
     */
    DICTATION("Dictation", "Write down what you hear");

    private final String displayName;
    private final String description;

    /**
     * Checks if this exercise type requires audio input/output.
     *
     * @return true if audio is required
     */
    public boolean requiresAudio() {
        return this == LISTENING || this == SPEAKING || this == DICTATION || this == ROLEPLAY;
    }

    /**
     * Checks if this exercise type involves speaking/recording.
     *
     * @return true if speaking is required
     */
    public boolean requiresSpeaking() {
        return this == SPEAKING || this == ROLEPLAY;
    }

    /**
     * Checks if this exercise type is primarily text-based.
     *
     * @return true if text-based
     */
    public boolean isTextBased() {
        return this == TEXT_COMPLETION || this == DRAG_DROP || 
               this == MULTIPLE_CHOICE || this == TRANSLATION || this == FLASHCARD;
    }
}