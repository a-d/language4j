package dev.languagelearning.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;
import java.util.Map;

/**
 * Represents a user of the language learning platform.
 * <p>
 * Each user has a native language and a target language they are learning,
 * along with their current skill level assessment.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    private static final String ISO_639_1_PATTERN = "^[a-z]{2}$";

    /**
     * Display name for the user.
     */
    @NotBlank
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Native language code (ISO 639-1).
     */
    @NotBlank
    @Pattern(regexp = ISO_639_1_PATTERN)
    @Column(name = "native_language", nullable = false, length = 2)
    private String nativeLanguage;

    /**
     * Target language code (ISO 639-1).
     */
    @NotBlank
    @Pattern(regexp = ISO_639_1_PATTERN)
    @Column(name = "target_language", nullable = false, length = 2)
    private String targetLanguage;

    /**
     * Current assessed skill level in the target language.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false)
    private SkillLevel skillLevel = SkillLevel.A1;

    /**
     * Whether the user has completed the initial skill assessment.
     */
    @Column(name = "assessment_completed", nullable = false)
    private boolean assessmentCompleted = false;

    /**
     * User's timezone for scheduling and goal tracking.
     */
    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    /**
     * Creates a language pair string for content organization.
     *
     * @return language pair in format "native-target" (e.g., "de-fr")
     */
    public String getLanguagePair() {
        return nativeLanguage + "-" + targetLanguage;
    }

    /**
     * Factory method to create a new user.
     *
     * @param displayName    the user's display name
     * @param nativeLanguage the user's native language code
     * @param targetLanguage the target language to learn
     * @return a new User instance
     */
    public static User of(String displayName, String nativeLanguage, String targetLanguage) {
        User user = new User();
        user.setDisplayName(displayName);
        user.setNativeLanguage(nativeLanguage);
        user.setTargetLanguage(targetLanguage);
        return user;
    }

    /**
     * Map of ISO 639-1 codes to English language names.
     * Used when Java's Locale doesn't have the language name.
     */
    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("de", "German"),
            Map.entry("en", "English"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("it", "Italian"),
            Map.entry("ja", "Japanese"),
            Map.entry("ko", "Korean"),
            Map.entry("nl", "Dutch"),
            Map.entry("pl", "Polish"),
            Map.entry("pt", "Portuguese"),
            Map.entry("ru", "Russian"),
            Map.entry("zh", "Chinese"),
            Map.entry("ar", "Arabic"),
            Map.entry("hi", "Hindi"),
            Map.entry("tr", "Turkish"),
            Map.entry("sv", "Swedish"),
            Map.entry("da", "Danish"),
            Map.entry("fi", "Finnish"),
            Map.entry("no", "Norwegian"),
            Map.entry("cs", "Czech"),
            Map.entry("el", "Greek"),
            Map.entry("he", "Hebrew"),
            Map.entry("hu", "Hungarian"),
            Map.entry("id", "Indonesian"),
            Map.entry("th", "Thai"),
            Map.entry("uk", "Ukrainian"),
            Map.entry("vi", "Vietnamese")
    );

    /**
     * Gets the human-readable name of the native language.
     *
     * @return the native language name in English (e.g., "German")
     */
    public String getNativeLanguageName() {
        return getLanguageName(nativeLanguage);
    }

    /**
     * Gets the human-readable name of the target language.
     *
     * @return the target language name in English (e.g., "French")
     */
    public String getTargetLanguageName() {
        return getLanguageName(targetLanguage);
    }

    /**
     * Converts an ISO 639-1 language code to a human-readable name.
     *
     * @param languageCode the ISO 639-1 code (e.g., "de", "fr")
     * @return the language name in English
     */
    private static String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "Unknown";
        }

        // Check our map first
        String name = LANGUAGE_NAMES.get(languageCode.toLowerCase());
        if (name != null) {
            return name;
        }

        // Fall back to Java's Locale
        Locale locale = Locale.forLanguageTag(languageCode);
        String displayName = locale.getDisplayLanguage(Locale.ENGLISH);
        if (displayName != null && !displayName.isEmpty() && !displayName.equals(languageCode)) {
            return displayName;
        }

        // Last resort: return the code itself
        return languageCode.toUpperCase();
    }
}
