package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response containing topic suggestions.
 */
@Schema(description = "Response containing topic suggestions for an activity type")
public record TopicSuggestionsResponse(
        @Schema(description = "The activity category these suggestions are for")
        String activityCategory,
        
        @Schema(description = "List of topic suggestions")
        List<TopicSuggestionDto> suggestions,
        
        @Schema(description = "A randomly selected topic (for 'Choose for me' option)")
        String randomTopic
) {
    /**
     * Creates a response with suggestions only.
     */
    public static TopicSuggestionsResponse of(String category, List<TopicSuggestionDto> suggestions) {
        return new TopicSuggestionsResponse(category, suggestions, null);
    }
    
    /**
     * Creates a response with suggestions and a random topic.
     */
    public static TopicSuggestionsResponse withRandom(String category, List<TopicSuggestionDto> suggestions, String randomTopic) {
        return new TopicSuggestionsResponse(category, suggestions, randomTopic);
    }
}