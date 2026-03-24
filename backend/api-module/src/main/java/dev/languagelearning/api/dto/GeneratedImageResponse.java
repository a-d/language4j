package dev.languagelearning.api.dto;

import jakarta.annotation.Nonnull;

/**
 * Response DTO for generated images.
 *
 * @param url           The URL or data URI of the generated image
 * @param revisedPrompt The prompt that was actually used (may differ from input)
 * @param size          The dimensions of the generated image
 */
public record GeneratedImageResponse(
        @Nonnull String url,
        @Nonnull String revisedPrompt,
        @Nonnull String size
) {
}