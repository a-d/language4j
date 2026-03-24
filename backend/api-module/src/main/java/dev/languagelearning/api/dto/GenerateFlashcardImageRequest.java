package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Request DTO for generating a flashcard image.
 * <p>
 * Creates an educational illustration for a vocabulary word,
 * optimized for use in learning flashcards.
 */
public record GenerateFlashcardImageRequest(
        @Nonnull String word,
        @Nullable String context
) {
    /**
     * Constructor with just the word.
     */
    public GenerateFlashcardImageRequest(@Nonnull String word) {
        this(word, null);
    }
}