package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for generating visual learning cards from a topic.
 * <p>
 * The backend will use LLM to derive vocabulary words from the topic
 * and generate images for each word.
 */
@Schema(description = "Request for generating visual learning cards from a topic")
public record GenerateVisualCardsRequest(
        @Schema(
                description = "Topic or context for vocabulary generation",
                example = "Kitchen items",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Topic is required")
        String topic,

        @Schema(
                description = "Number of cards to generate (1-10)",
                example = "5",
                minimum = "1",
                maximum = "10",
                defaultValue = "5"
        )
        @Min(value = 1, message = "Minimum 1 card")
        @Max(value = 10, message = "Maximum 10 cards")
        Integer cardCount
) {
    /**
     * Returns the card count, defaulting to 5 if not specified.
     */
    public int getCardCountOrDefault() {
        return cardCount != null ? cardCount : 5;
    }
}