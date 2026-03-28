package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to generate topic suggestions for an activity category.
 */
@Schema(description = "Request for generating topic suggestions")
public record GenerateTopicSuggestionsRequest(
        @NotBlank
        @Schema(description = "Activity category", 
                example = "VOCABULARY",
                allowableValues = {"VOCABULARY", "EXERCISE", "LESSON", "SCENARIO", "AUDIO"})
        String category,
        
        @Min(1)
        @Max(10)
        @Schema(description = "Number of suggestions to generate (1-10)", 
                example = "5",
                defaultValue = "5")
        Integer count,
        
        @Schema(description = "Whether to include a random topic selection", 
                defaultValue = "true")
        Boolean includeRandom
) {
    /**
     * Returns count with default value.
     */
    public int getCountOrDefault() {
        return count != null ? count : 5;
    }
    
    /**
     * Returns includeRandom with default value.
     */
    public boolean shouldIncludeRandom() {
        return includeRandom == null || includeRandom;
    }
}