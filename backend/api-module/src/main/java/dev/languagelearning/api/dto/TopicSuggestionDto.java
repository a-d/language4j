package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for a topic suggestion.
 */
@Schema(description = "A suggested topic for learning activities")
public record TopicSuggestionDto(
        @Schema(description = "The topic name", example = "Greetings and introductions")
        String topic,
        
        @Schema(description = "Brief description of the topic", example = "Learn common phrases for meeting people")
        String description,
        
        @Schema(description = "Emoji icon for the topic", example = "👋")
        String emoji,
        
        @Schema(description = "Whether this topic aligns with user's current daily goals")
        boolean alignsWithGoals
) {}