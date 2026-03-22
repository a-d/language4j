package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating vocabulary.
 */
public record GenerateVocabularyRequest(
        @Nonnull String topic,
        int wordCount
) {
    public GenerateVocabularyRequest {
        if (wordCount <= 0) {
            wordCount = 10;
        }
    }
}