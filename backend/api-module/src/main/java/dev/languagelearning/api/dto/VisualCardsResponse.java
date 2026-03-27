package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO containing generated visual learning cards.
 */
@Schema(description = "Response containing generated visual learning cards")
public record VisualCardsResponse(
        @Schema(
                description = "The topic used to generate vocabulary",
                example = "Kitchen items"
        )
        String topic,

        @Schema(
                description = "Native language code",
                example = "en"
        )
        String nativeLanguage,

        @Schema(
                description = "Target language code",
                example = "fr"
        )
        String targetLanguage,

        @Schema(
                description = "List of generated visual cards"
        )
        List<VisualCardDto> cards,

        @Schema(
                description = "Number of cards successfully generated"
        )
        int cardCount,

        @Schema(
                description = "Number of cards that failed to generate (if any)"
        )
        int failedCount
) {
    /**
     * Creates a successful response with all cards generated.
     */
    public static VisualCardsResponse success(
            String topic,
            String nativeLanguage,
            String targetLanguage,
            List<VisualCardDto> cards
    ) {
        return new VisualCardsResponse(topic, nativeLanguage, targetLanguage, cards, cards.size(), 0);
    }

    /**
     * Creates a response with partial success (some cards failed).
     */
    public static VisualCardsResponse partial(
            String topic,
            String nativeLanguage,
            String targetLanguage,
            List<VisualCardDto> cards,
            int failedCount
    ) {
        return new VisualCardsResponse(topic, nativeLanguage, targetLanguage, cards, cards.size(), failedCount);
    }
}