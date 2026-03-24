package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Request DTO for generating an image.
 */
public record GenerateImageRequest(
        @Nonnull String prompt,
        @Nullable String size,
        @Nullable String quality,
        @Nullable String style
) {
    /**
     * Default constructor with prompt only.
     */
    public GenerateImageRequest(@Nonnull String prompt) {
        this(prompt, null, null, null);
    }
}