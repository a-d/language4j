package dev.languagelearning.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.service.I18nService;
import dev.languagelearning.llm.LlmService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of I18nService that manages UI translations.
 * <p>
 * Bundled translations (en, de) are loaded from classpath resources.
 * New translations are generated via LLM and cached to the data directory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class I18nServiceImpl implements I18nService {

    private static final String I18N_RESOURCE_PATH = "i18n/";
    private static final String MESSAGES_FILE = "messages.json";
    private static final Set<String> BUNDLED_LANGUAGES = Set.of("en", "de");
    private static final int BATCH_SIZE = 50; // Number of keys to translate per LLM call

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();
    private Map<String, MessageMetadata> messageMetadata;
    private Map<String, String> englishTranslations;
    private Path generatedTranslationsPath;

    @PostConstruct
    void init() {
        // Load message metadata
        loadMessageMetadata();
        
        // Load English translations as base
        englishTranslations = loadBundledTranslations("en");
        
        // Pre-load bundled translations
        for (String lang : BUNDLED_LANGUAGES) {
            try {
                translationCache.put(lang, loadBundledTranslations(lang));
                log.info("Loaded bundled translations for: {}", lang);
            } catch (Exception e) {
                log.warn("Failed to load bundled translations for {}: {}", lang, e.getMessage());
            }
        }
        
        // Set up path for generated translations
        generatedTranslationsPath = Paths.get("data", "i18n");
        try {
            Files.createDirectories(generatedTranslationsPath);
            log.info("Generated translations directory: {}", generatedTranslationsPath.toAbsolutePath());
            
            // Load any previously generated translations
            loadGeneratedTranslations();
        } catch (IOException e) {
            log.warn("Could not create generated translations directory: {}", e.getMessage());
        }
    }

    @Override
    @Nonnull
    public Map<String, String> getTranslations(@Nonnull String languageCode) {
        return getTranslations(languageCode, false);
    }

    @Override
    @Nonnull
    public Map<String, String> getTranslations(@Nonnull String languageCode, boolean forceRegenerate) {
        String lang = languageCode.toLowerCase();
        
        // Return cached if available and not forcing regeneration
        if (!forceRegenerate && translationCache.containsKey(lang)) {
            return translationCache.get(lang);
        }
        
        // For bundled languages, always use bundled (no regeneration)
        if (BUNDLED_LANGUAGES.contains(lang)) {
            return translationCache.getOrDefault(lang, englishTranslations);
        }
        
        // Generate translations for new language
        log.info("Generating translations for language: {}", lang);
        Map<String, String> translations = generateTranslations(lang);
        
        // Cache and persist
        translationCache.put(lang, translations);
        saveGeneratedTranslations(lang, translations);
        
        return translations;
    }

    @Override
    @Nonnull
    public List<String> getAvailableLanguages() {
        Set<String> languages = new TreeSet<>(translationCache.keySet());
        
        // Also check for files in generated directory
        if (generatedTranslationsPath != null && Files.exists(generatedTranslationsPath)) {
            try (var files = Files.list(generatedTranslationsPath)) {
                files.filter(p -> p.toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString().replace(".json", ""))
                        .forEach(languages::add);
            } catch (IOException e) {
                log.warn("Error listing generated translations: {}", e.getMessage());
            }
        }
        
        return new ArrayList<>(languages);
    }

    @Override
    public boolean hasTranslations(@Nonnull String languageCode) {
        String lang = languageCode.toLowerCase();
        
        // Check cache
        if (translationCache.containsKey(lang)) {
            return true;
        }
        
        // Check bundled
        if (BUNDLED_LANGUAGES.contains(lang)) {
            return true;
        }
        
        // Check generated file
        if (generatedTranslationsPath != null) {
            Path langFile = generatedTranslationsPath.resolve(lang + ".json");
            return Files.exists(langFile);
        }
        
        return false;
    }

    @Override
    @Nonnull
    public Map<String, MessageMetadata> getMessageMetadata() {
        return messageMetadata != null ? messageMetadata : Map.of();
    }

    // ==================== Private Methods ====================

    private void loadMessageMetadata() {
        try {
            ClassPathResource resource = new ClassPathResource(I18N_RESOURCE_PATH + MESSAGES_FILE);
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                messageMetadata = new HashMap<>();
                
                root.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    JsonNode meta = entry.getValue();
                    
                    String context = meta.has("context") ? meta.get("context").asText() : null;
                    Integer maxLength = meta.has("maxLength") ? meta.get("maxLength").asInt() : null;
                    List<String> parameters = null;
                    if (meta.has("parameters")) {
                        parameters = new ArrayList<>();
                        for (JsonNode param : meta.get("parameters")) {
                            parameters.add(param.asText());
                        }
                    }
                    String example = meta.has("example") ? meta.get("example").asText() : null;
                    String note = meta.has("note") ? meta.get("note").asText() : null;
                    
                    messageMetadata.put(key, new MessageMetadata(context, maxLength, parameters, example, note));
                });
                
                log.info("Loaded message metadata with {} keys", messageMetadata.size());
            }
        } catch (IOException e) {
            log.error("Failed to load message metadata: {}", e.getMessage());
            messageMetadata = Map.of();
        }
    }

    private Map<String, String> loadBundledTranslations(String languageCode) {
        try {
            ClassPathResource resource = new ClassPathResource(I18N_RESOURCE_PATH + languageCode + ".json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<Map<String, String>>() {});
            }
        } catch (IOException e) {
            log.error("Failed to load bundled translations for {}: {}", languageCode, e.getMessage());
            return Map.of();
        }
    }

    private void loadGeneratedTranslations() {
        if (generatedTranslationsPath == null || !Files.exists(generatedTranslationsPath)) {
            return;
        }
        
        try (var files = Files.list(generatedTranslationsPath)) {
            files.filter(p -> p.toString().endsWith(".json"))
                    .forEach(path -> {
                        String lang = path.getFileName().toString().replace(".json", "");
                        if (!BUNDLED_LANGUAGES.contains(lang)) {
                            try {
                                Map<String, String> translations = objectMapper.readValue(
                                        path.toFile(), 
                                        new TypeReference<Map<String, String>>() {}
                                );
                                translationCache.put(lang, translations);
                                log.info("Loaded generated translations for: {}", lang);
                            } catch (IOException e) {
                                log.warn("Failed to load generated translations for {}: {}", lang, e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            log.warn("Error loading generated translations: {}", e.getMessage());
        }
    }

    private void saveGeneratedTranslations(String languageCode, Map<String, String> translations) {
        if (generatedTranslationsPath == null) {
            log.warn("Cannot save translations - no generated path configured");
            return;
        }
        
        try {
            Path langFile = generatedTranslationsPath.resolve(languageCode + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(langFile.toFile(), translations);
            log.info("Saved generated translations for {} to {}", languageCode, langFile);
        } catch (IOException e) {
            log.error("Failed to save generated translations for {}: {}", languageCode, e.getMessage());
        }
    }

    private Map<String, String> generateTranslations(String targetLanguageCode) {
        Map<String, String> result = new LinkedHashMap<>();
        String targetLanguageName = getLanguageName(targetLanguageCode);
        
        // Group keys into batches for efficient LLM calls
        List<String> allKeys = new ArrayList<>(englishTranslations.keySet());
        
        for (int i = 0; i < allKeys.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allKeys.size());
            List<String> batchKeys = allKeys.subList(i, end);
            
            log.info("Translating batch {}-{} of {} keys to {}", 
                    i + 1, end, allKeys.size(), targetLanguageName);
            
            Map<String, String> batchTranslations = generateBatch(batchKeys, targetLanguageName);
            result.putAll(batchTranslations);
        }
        
        // Fill in any missing keys with English fallback
        for (String key : englishTranslations.keySet()) {
            if (!result.containsKey(key)) {
                log.warn("Missing translation for key: {} - using English fallback", key);
                result.put(key, englishTranslations.get(key));
            }
        }
        
        return result;
    }

    private Map<String, String> generateBatch(List<String> keys, String targetLanguage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional translator for a language learning application UI.\n");
        prompt.append("Translate the following UI strings from English to ").append(targetLanguage).append(".\n\n");
        prompt.append("IMPORTANT RULES:\n");
        prompt.append("- Keep the same meaning and tone\n");
        prompt.append("- Preserve all placeholders like {target}, {native}, {count}, etc. - do NOT translate them\n");
        prompt.append("- Preserve emojis exactly as they appear\n");
        prompt.append("- Keep translations concise - they must fit in UI elements\n");
        prompt.append("- Use informal/friendly 'you' form appropriate for a learning app\n\n");
        prompt.append("Output format: Return ONLY a valid JSON object mapping keys to translated values.\n");
        prompt.append("Do not include any explanation, markdown formatting, or code blocks.\n\n");
        prompt.append("Translations to perform:\n\n");
        
        for (String key : keys) {
            String englishText = englishTranslations.get(key);
            MessageMetadata meta = messageMetadata.get(key);
            
            prompt.append("Key: \"").append(key).append("\"\n");
            prompt.append("English: \"").append(englishText).append("\"\n");
            
            if (meta != null && meta.context() != null) {
                prompt.append("Context: ").append(meta.context()).append("\n");
            }
            if (meta != null && meta.maxLength() != null) {
                prompt.append("Max length: ").append(meta.maxLength()).append(" characters\n");
            }
            if (meta != null && meta.parameters() != null && !meta.parameters().isEmpty()) {
                prompt.append("Parameters (keep unchanged): ").append(String.join(", ", meta.parameters())).append("\n");
            }
            if (meta != null && meta.note() != null) {
                prompt.append("Note: ").append(meta.note()).append("\n");
            }
            prompt.append("\n");
        }
        
        try {
            String response = llmService.generate(prompt.toString());
            
            // Clean up response - remove markdown code blocks if present
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            } else if (response.startsWith("```")) {
                response = response.substring(3);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();
            
            return objectMapper.readValue(response, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to generate translations batch: {}", e.getMessage());
            
            // Return English fallback for this batch
            Map<String, String> fallback = new LinkedHashMap<>();
            for (String key : keys) {
                fallback.put(key, englishTranslations.get(key));
            }
            return fallback;
        }
    }

    private String getLanguageName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "en" -> "English";
            case "de" -> "German";
            case "fr" -> "French";
            case "es" -> "Spanish";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "nl" -> "Dutch";
            case "pl" -> "Polish";
            case "ru" -> "Russian";
            case "ja" -> "Japanese";
            case "zh" -> "Chinese";
            case "ko" -> "Korean";
            case "ar" -> "Arabic";
            case "tr" -> "Turkish";
            case "vi" -> "Vietnamese";
            case "th" -> "Thai";
            case "sv" -> "Swedish";
            case "da" -> "Danish";
            case "no" -> "Norwegian";
            case "fi" -> "Finnish";
            case "cs" -> "Czech";
            case "hu" -> "Hungarian";
            case "ro" -> "Romanian";
            case "uk" -> "Ukrainian";
            case "el" -> "Greek";
            case "he" -> "Hebrew";
            case "hi" -> "Hindi";
            case "id" -> "Indonesian";
            default -> languageCode.toUpperCase() + " language";
        };
    }
}