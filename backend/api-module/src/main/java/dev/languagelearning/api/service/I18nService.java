package dev.languagelearning.api.service;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Service for internationalization (i18n) management.
 * <p>
 * Provides access to UI translations for the frontend. Supports:
 * <ul>
 *   <li>Loading pre-defined translations (en, de)</li>
 *   <li>Generating translations for new languages via LLM</li>
 *   <li>Caching generated translations to disk</li>
 * </ul>
 * <p>
 * Translations are stored as JSON files in the resources/i18n directory.
 */
public interface I18nService {

    /**
     * Gets translations for a specific language.
     * <p>
     * If the language file exists, returns cached translations.
     * If not, generates translations using LLM and caches them.
     *
     * @param languageCode ISO 639-1 language code (e.g., "en", "de", "fr")
     * @return map of translation key to translated text
     */
    @Nonnull
    Map<String, String> getTranslations(@Nonnull String languageCode);

    /**
     * Gets translations for a specific language, optionally forcing regeneration.
     *
     * @param languageCode ISO 639-1 language code
     * @param forceRegenerate if true, regenerates translations even if cached
     * @return map of translation key to translated text
     */
    @Nonnull
    Map<String, String> getTranslations(@Nonnull String languageCode, boolean forceRegenerate);

    /**
     * Lists all available language codes with cached translations.
     *
     * @return list of ISO 639-1 language codes
     */
    @Nonnull
    List<String> getAvailableLanguages();

    /**
     * Checks if translations exist for a language (either bundled or generated).
     *
     * @param languageCode ISO 639-1 language code
     * @return true if translations are available without generation
     */
    boolean hasTranslations(@Nonnull String languageCode);

    /**
     * Gets the message metadata including context information.
     * Used by LLM to generate accurate translations.
     *
     * @return map of translation key to metadata (context, parameters, etc.)
     */
    @Nonnull
    Map<String, MessageMetadata> getMessageMetadata();

    /**
     * Metadata for a translation message, used to provide context to LLM.
     */
    record MessageMetadata(
            String context,
            Integer maxLength,
            List<String> parameters,
            String example,
            String note
    ) {
        public static MessageMetadata of(String context) {
            return new MessageMetadata(context, null, null, null, null);
        }
    }
}