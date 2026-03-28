package dev.languagelearning.api.controller;

import dev.languagelearning.api.service.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for internationalization (i18n) endpoints.
 * <p>
 * Provides translations for the frontend UI. Supports:
 * <ul>
 *   <li>Loading bundled translations (en, de)</li>
 *   <li>Generating translations for new languages via LLM</li>
 *   <li>Checking available languages</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/i18n")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "I18n", description = "Internationalization endpoints for UI translations")
public class I18nController {

    private final I18nService i18nService;

    /**
     * Gets translations for a specific language.
     * <p>
     * If the language exists (bundled or previously generated), returns cached translations.
     * If the language is new, generates translations using LLM (may take a few seconds).
     *
     * @param languageCode ISO 639-1 language code (e.g., "en", "de", "fr")
     * @return Map of translation keys to translated text
     */
    @GetMapping("/languages/{languageCode}")
    @Operation(
            summary = "Get translations for a language",
            description = "Returns UI translations for the specified language. Bundled languages (en, de) are returned immediately. " +
                    "New languages trigger LLM-based translation generation and caching."
    )
    @ApiResponse(responseCode = "200", description = "Translations returned successfully")
    public ResponseEntity<Map<String, String>> getTranslations(
            @Parameter(description = "ISO 639-1 language code", example = "fr")
            @PathVariable String languageCode
    ) {
        log.debug("Fetching translations for language: {}", languageCode);
        
        Map<String, String> translations = i18nService.getTranslations(languageCode);
        
        log.info("Returning {} translations for language: {}", translations.size(), languageCode);
        return ResponseEntity.ok(translations);
    }

    /**
     * Force regeneration of translations for a language.
     * <p>
     * This is useful if translations need to be updated after changes to the base English strings.
     * Note: Bundled languages (en, de) cannot be regenerated.
     *
     * @param languageCode ISO 639-1 language code
     * @return Map of translation keys to translated text
     */
    @PostMapping("/languages/{languageCode}/generate")
    @Operation(
            summary = "Generate/regenerate translations",
            description = "Forces regeneration of translations for a language using LLM. " +
                    "Bundled languages (en, de) cannot be regenerated."
    )
    @ApiResponse(responseCode = "200", description = "Translations generated successfully")
    @ApiResponse(responseCode = "400", description = "Cannot regenerate bundled languages")
    public ResponseEntity<Map<String, String>> generateTranslations(
            @Parameter(description = "ISO 639-1 language code", example = "fr")
            @PathVariable String languageCode
    ) {
        String lang = languageCode.toLowerCase();
        
        if ("en".equals(lang) || "de".equals(lang)) {
            log.warn("Attempted to regenerate bundled language: {}", languageCode);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Force regenerating translations for language: {}", languageCode);
        Map<String, String> translations = i18nService.getTranslations(languageCode, true);
        
        return ResponseEntity.ok(translations);
    }

    /**
     * Lists all available languages with translations.
     * <p>
     * Returns both bundled languages and any previously generated languages.
     *
     * @return List of ISO 639-1 language codes
     */
    @GetMapping("/languages")
    @Operation(
            summary = "List available languages",
            description = "Returns list of all languages with available translations (bundled and generated)"
    )
    @ApiResponse(responseCode = "200", description = "Language list returned successfully")
    public ResponseEntity<AvailableLanguagesResponse> getAvailableLanguages() {
        List<String> languages = i18nService.getAvailableLanguages();
        
        return ResponseEntity.ok(new AvailableLanguagesResponse(languages));
    }

    /**
     * Checks if translations exist for a language.
     * <p>
     * Useful for the frontend to determine if translations need to be generated.
     *
     * @param languageCode ISO 639-1 language code
     * @return true if translations are available without generation
     */
    @GetMapping("/languages/{languageCode}/exists")
    @Operation(
            summary = "Check if translations exist",
            description = "Returns whether translations are available for a language without triggering generation"
    )
    @ApiResponse(responseCode = "200", description = "Existence check completed")
    public ResponseEntity<TranslationExistsResponse> hasTranslations(
            @Parameter(description = "ISO 639-1 language code", example = "fr")
            @PathVariable String languageCode
    ) {
        boolean exists = i18nService.hasTranslations(languageCode);
        
        return ResponseEntity.ok(new TranslationExistsResponse(languageCode, exists));
    }

    // Response DTOs

    /**
     * Response for available languages endpoint.
     */
    public record AvailableLanguagesResponse(
            List<String> languages
    ) {}

    /**
     * Response for translation existence check.
     */
    public record TranslationExistsResponse(
            String languageCode,
            boolean exists
    ) {}
}