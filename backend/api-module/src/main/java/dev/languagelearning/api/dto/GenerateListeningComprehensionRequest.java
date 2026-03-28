package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

/**
 * Request DTO for generating listening comprehension exercises.
 * <p>
 * A listening comprehension exercise consists of:
 * - A short story in the target language
 * - Translation of the story
 * - True/false statements to test comprehension
 */
@Schema(description = "Request to generate a listening comprehension exercise")
public record GenerateListeningComprehensionRequest(
        @Schema(description = "Topic for the story", example = "daily routines", requiredMode = Schema.RequiredMode.REQUIRED)
        @Nonnull String topic,

        @Schema(description = "Approximate word count for the story (default 100)", example = "100", minimum = "30", maximum = "500", defaultValue = "100")
        int wordCount,

        @Schema(description = "Number of true/false statements (default 5)", example = "5", minimum = "3", maximum = "10", defaultValue = "5")
        int statementCount
) {
    public GenerateListeningComprehensionRequest {
        if (wordCount <= 0) {
            wordCount = 100;
        }
        if (statementCount <= 0) {
            statementCount = 5;
        }
    }

    /**
     * Creates a request with default values for wordCount and statementCount.
     * @param topic the topic for the story
     * @return a new request with defaults
     */
    public static GenerateListeningComprehensionRequest withDefaults(@Nonnull String topic) {
        return new GenerateListeningComprehensionRequest(topic, 100, 5);
    }
}