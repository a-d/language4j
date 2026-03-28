package dev.languagelearning.api.dto;

import dev.languagelearning.core.domain.ExerciseGenerationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * Unified request DTO for generating any type of exercise.
 * <p>
 * This replaces the individual request DTOs for each exercise type,
 * consolidating them into a single flexible request format.
 * <p>
 * Example requests:
 * <pre>{@code
 * // Simple text completion
 * { "type": "TEXT_COMPLETION", "topic": "past tense verbs" }
 * 
 * // Translation with custom count
 * { "type": "TRANSLATION", "topic": "restaurant phrases", "count": 10 }
 * 
 * // Listening comprehension with options
 * { "type": "LISTENING_COMPREHENSION", "topic": "daily routines", 
 *   "options": { "wordCount": 150, "statementCount": 6 } }
 * }</pre>
 */
@Schema(description = "Unified request to generate exercises of any type")
public record GenerateExercisesRequest(

        @Schema(
                description = "Type of exercise to generate",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "TEXT_COMPLETION"
        )
        @Nonnull
        ExerciseGenerationType type,

        @Schema(
                description = "Topic for exercise generation",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "past tense verbs"
        )
        @Nonnull
        String topic,

        @Schema(
                description = "Number of exercises to generate. If not specified, uses the default for the exercise type.",
                example = "5",
                minimum = "1",
                maximum = "20"
        )
        @Nullable
        @Min(1)
        @Max(20)
        Integer count,

        @Schema(
                description = "Type-specific options. Used for exercise types that require additional parameters, " +
                        "such as LISTENING_COMPREHENSION (wordCount, statementCount).",
                example = "{\"wordCount\": 100, \"statementCount\": 5}"
        )
        @Nullable
        Map<String, Object> options

) {
    /**
     * Gets the count to use for generation, falling back to the type's default if not specified.
     *
     * @return the count to use
     */
    public int getEffectiveCount() {
        return count != null && count > 0 ? count : type.getDefaultCount();
    }

    /**
     * Gets an option value as an integer, with a default fallback.
     *
     * @param key the option key
     * @param defaultValue the default value if not present
     * @return the option value as integer
     */
    public int getOptionAsInt(String key, int defaultValue) {
        if (options == null || !options.containsKey(key)) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets an option value as a string, with a default fallback.
     *
     * @param key the option key
     * @param defaultValue the default value if not present
     * @return the option value as string
     */
    public String getOptionAsString(String key, String defaultValue) {
        if (options == null || !options.containsKey(key)) {
            return defaultValue;
        }
        Object value = options.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}