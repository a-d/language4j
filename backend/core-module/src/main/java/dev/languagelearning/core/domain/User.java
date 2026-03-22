package dev.languagelearning.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}