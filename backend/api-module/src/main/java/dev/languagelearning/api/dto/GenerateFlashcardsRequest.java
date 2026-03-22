package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating flashcards.
 */
public record GenerateFlashcardsRequest(
        @Nonnull String topic,
        int cardCount
) {
    public GenerateFlashcardsRequest {
        if (cardCount <= 0) {
            cardCount = 10;
        }
    }
}