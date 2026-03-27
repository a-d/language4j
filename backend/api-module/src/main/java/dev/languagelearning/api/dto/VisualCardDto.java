package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing a single visual learning card with bilingual content.
 * <p>
 * A visual card contains:
 * <ul>
 *   <li>Word in native language (shown on front)</li>
 *   <li>Word in target language (shown on back after flip)</li>
 *   <li>AI-generated image for visual learning</li>
 *   <li>Optional example sentence in target language</li>
 * </ul>
 */
@Schema(description = "A visual learning card with bilingual content and AI-generated image")
public record VisualCardDto(
        @Schema(
                description = "Word in the user's native language (displayed on front)",
                example = "the apple"
        )
        String nativeWord,

        @Schema(
                description = "Word in the target language (displayed on back after flip)",
                example = "la pomme"
        )
        String targetWord,

        @Schema(
                description = "URL of the AI-generated image",
                example = "https://oaidalleapiprodscus.blob.core.windows.net/..."
        )
        String imageUrl,

        @Schema(
                description = "Example sentence using the word in target language",
                example = "Je mange une pomme rouge."
        )
        String exampleSentence,

        @Schema(
                description = "Phonetic pronunciation guide (if available)",
                example = "la pɔm"
        )
        String pronunciation
) {
    /**
     * Creates a VisualCardDto with required fields only.
     */
    public static VisualCardDto of(String nativeWord, String targetWord, String imageUrl) {
        return new VisualCardDto(nativeWord, targetWord, imageUrl, null, null);
    }

    /**
     * Creates a VisualCardDto with example sentence.
     */
    public static VisualCardDto withExample(String nativeWord, String targetWord, String imageUrl, String exampleSentence) {
        return new VisualCardDto(nativeWord, targetWord, imageUrl, exampleSentence, null);
    }
}