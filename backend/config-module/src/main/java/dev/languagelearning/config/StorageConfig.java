package dev.languagelearning.config;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * Configuration for data storage settings.
 * <p>
 * Defines paths for content storage (Markdown files) and media storage (audio, images).
 * <p>
 * Example configuration:
 * <pre>{@code
 * language-learning:
 *   storage:
 *     content-path: ./content
 *     media-path: ./media
 * }</pre>
 */
@ConfigurationProperties(prefix = "language-learning.storage")
@Validated
@Getter
@Setter
public class StorageConfig {

    /**
     * Base path for storing Markdown content files.
     * Contains lessons, vocabulary, scenarios, and learning cards.
     */
    @NotBlank
    private String contentPath = "./content";

    /**
     * Base path for storing media files.
     * Contains audio recordings and generated images.
     */
    @NotBlank
    private String mediaPath = "./media";

    /**
     * Base path for storing template files.
     */
    @NotBlank
    private String templatesPath = "./content/templates";

    /**
     * Gets the content storage path as a Path object.
     */
    @Nonnull
    public Path getContentBasePath() {
        return Path.of(contentPath);
    }

    /**
     * Gets the media storage path as a Path object.
     */
    @Nonnull
    public Path getMediaBasePath() {
        return Path.of(mediaPath);
    }

    /**
     * Gets the templates storage path as a Path object.
     */
    @Nonnull
    public Path getTemplatesBasePath() {
        return Path.of(templatesPath);
    }

    /**
     * Gets the path for user-specific content.
     *
     * @param userId the user identifier
     * @return path to user's content directory
     */
    @Nonnull
    public Path getUserContentPath(@Nonnull String userId) {
        return getContentBasePath().resolve(userId);
    }

    /**
     * Gets the path for user's lessons in a specific language pair.
     *
     * @param userId       the user identifier
     * @param languagePair the language pair (e.g., "de-fr")
     * @return path to user's lessons directory
     */
    @Nonnull
    public Path getLessonsPath(@Nonnull String userId, @Nonnull String languagePair) {
        return getUserContentPath(userId).resolve("lessons").resolve(languagePair);
    }

    /**
     * Gets the path for user's vocabulary in a specific language pair.
     *
     * @param userId       the user identifier
     * @param languagePair the language pair (e.g., "de-fr")
     * @return path to user's vocabulary directory
     */
    @Nonnull
    public Path getVocabularyPath(@Nonnull String userId, @Nonnull String languagePair) {
        return getUserContentPath(userId).resolve("vocabulary").resolve(languagePair);
    }

    /**
     * Gets the path for user's scenarios in a specific language pair.
     *
     * @param userId       the user identifier
     * @param languagePair the language pair (e.g., "de-fr")
     * @return path to user's scenarios directory
     */
    @Nonnull
    public Path getScenariosPath(@Nonnull String userId, @Nonnull String languagePair) {
        return getUserContentPath(userId).resolve("scenarios").resolve(languagePair);
    }

    /**
     * Gets the path for user's learning cards in a specific language pair.
     *
     * @param userId       the user identifier
     * @param languagePair the language pair (e.g., "de-fr")
     * @return path to user's cards directory
     */
    @Nonnull
    public Path getCardsPath(@Nonnull String userId, @Nonnull String languagePair) {
        return getUserContentPath(userId).resolve("cards").resolve(languagePair);
    }

    /**
     * Gets the path for user's audio files.
     *
     * @param userId the user identifier
     * @return path to user's audio directory
     */
    @Nonnull
    public Path getAudioPath(@Nonnull String userId) {
        return getMediaBasePath().resolve("audio").resolve(userId);
    }

    /**
     * Gets the path for user's image files.
     *
     * @param userId the user identifier
     * @return path to user's images directory
     */
    @Nonnull
    public Path getImagesPath(@Nonnull String userId) {
        return getMediaBasePath().resolve("images").resolve(userId);
    }
}