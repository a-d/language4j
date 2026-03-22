package dev.languagelearning.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for language settings.
 * <p>
 * Defines the native language (for UI and explanations) and target language
 * (language being learned) using ISO 639-1 codes.
 * <p>
 * Example configuration:
 * <pre>{@code
 * language-learning:
 *   language:
 *     native: de
 *     target: fr
 * }</pre>
 */
@ConfigurationProperties(prefix = "language-learning.language")
@Validated
@Getter
@Setter
public class LanguageConfig {

    private static final String ISO_639_1_PATTERN = "^[a-z]{2}$";

    /**
     * Native language code (ISO 639-1).
     * Used for UI elements and explanations.
     */
    @NotBlank(message = "Native language code is required")
    @Pattern(regexp = ISO_639_1_PATTERN, message = "Native language must be a valid ISO 639-1 code (e.g., 'de', 'en')")
    private String nativeCode;

    /**
     * Target language code (ISO 639-1).
     * The language being learned.
     */
    @NotBlank(message = "Target language code is required")
    @Pattern(regexp = ISO_639_1_PATTERN, message = "Target language must be a valid ISO 639-1 code (e.g., 'fr', 'es')")
    private String targetCode;

    /**
     * Native language name (human-readable).
     * Used in prompts and content generation.
     */
    private String nativeName = "German";

    /**
     * Target language name (human-readable).
     * Used in prompts and content generation.
     */
    private String targetName = "French";

    /**
     * Creates a language pair string for content organization.
     *
     * @return language pair in format "native-target" (e.g., "de-fr")
     */
    public String getLanguagePair() {
        return nativeCode + "-" + targetCode;
    }
}