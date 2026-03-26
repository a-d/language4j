package dev.languagelearning.content.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates and normalizes vocabulary JSON responses from the LLM.
 * <p>
 * Ensures the vocabulary response has the correct structure expected by the frontend:
 * <pre>
 * {
 *   "vocabulary": [
 *     {
 *       "word": "...",
 *       "pronunciation": "...",
 *       "translation": "...",
 *       "partOfSpeech": "...",
 *       "example": "...",
 *       "exampleTranslation": "...",
 *       "usageNote": "..."
 *     }
 *   ]
 * }
 * </pre>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VocabularyJsonValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Validates and normalizes vocabulary JSON.
     * <p>
     * Handles various LLM output formats:
     * <ul>
     *   <li>{@code { "vocabulary": [...] }} - Expected format, returned as-is</li>
     *   <li>{@code { "words": [...] }} - Renamed to "vocabulary"</li>
     *   <li>{@code { "items": [...] }} - Renamed to "vocabulary"</li>
     *   <li>{@code [...]} - Array wrapped in {"vocabulary": [...]}</li>
     * </ul>
     * <p>
     * Also sanitizes JSON to fix common LLM issues:
     * <ul>
     *   <li>Invalid control characters inside strings</li>
     *   <li>Bad escape sequences</li>
     *   <li>Unescaped special characters</li>
     * </ul>
     *
     * @param json the raw JSON string
     * @return normalized JSON string with correct structure
     */
    public static String validateAndNormalize(String json) {
        if (json == null || json.isBlank()) {
            log.warn("Empty vocabulary JSON received");
            return createEmptyVocabulary();
        }

        // Sanitize JSON to fix common LLM issues
        String sanitizedJson = sanitizeJson(json);

        try {
            JsonNode root = OBJECT_MAPPER.readTree(sanitizedJson);
            
            // Case 1: Already has "vocabulary" array
            if (root.has("vocabulary") && root.get("vocabulary").isArray()) {
                return normalizeVocabularyItems(root);
            }
            
            // Case 2: Has "words" array instead
            if (root.has("words") && root.get("words").isArray()) {
                ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
                normalized.set("vocabulary", normalizeItems((ArrayNode) root.get("words")));
                return OBJECT_MAPPER.writeValueAsString(normalized);
            }
            
            // Case 3: Has "items" array instead
            if (root.has("items") && root.get("items").isArray()) {
                ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
                normalized.set("vocabulary", normalizeItems((ArrayNode) root.get("items")));
                return OBJECT_MAPPER.writeValueAsString(normalized);
            }
            
            // Case 4: Root is an array
            if (root.isArray()) {
                ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
                normalized.set("vocabulary", normalizeItems((ArrayNode) root));
                return OBJECT_MAPPER.writeValueAsString(normalized);
            }
            
            // Case 5: Unknown structure - try to find any array
            for (var field : iterable(root.fields())) {
                if (field.getValue().isArray()) {
                    log.info("Found vocabulary array in field: {}", field.getKey());
                    ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
                    normalized.set("vocabulary", normalizeItems((ArrayNode) field.getValue()));
                    return OBJECT_MAPPER.writeValueAsString(normalized);
                }
            }
            
            log.warn("Could not find vocabulary array in JSON, returning original");
            return json;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse vocabulary JSON: {}", e.getMessage());
            return json; // Return original if parsing fails
        }
    }

    /**
     * Normalizes the root node that already has "vocabulary" array.
     */
    private static String normalizeVocabularyItems(JsonNode root) throws JsonProcessingException {
        ObjectNode normalized = OBJECT_MAPPER.createObjectNode();
        normalized.set("vocabulary", normalizeItems((ArrayNode) root.get("vocabulary")));
        return OBJECT_MAPPER.writeValueAsString(normalized);
    }

    /**
     * Normalizes vocabulary items to ensure consistent field names.
     */
    private static ArrayNode normalizeItems(ArrayNode items) {
        ArrayNode normalized = OBJECT_MAPPER.createArrayNode();
        
        for (JsonNode item : items) {
            if (!item.isObject()) continue;
            
            ObjectNode normalizedItem = OBJECT_MAPPER.createObjectNode();
            
            // Word field (required)
            normalizedItem.put("word", getStringField(item, "word", "term", "phrase", "vocabulary"));
            
            // Translation field (required)
            normalizedItem.put("translation", getStringField(item, "translation", "meaning", "definition"));
            
            // Optional fields
            String pronunciation = getStringField(item, "pronunciation", "phonetic", "ipa");
            if (pronunciation != null) normalizedItem.put("pronunciation", pronunciation);
            
            String partOfSpeech = getStringField(item, "partOfSpeech", "pos", "type", "wordType", "part_of_speech");
            if (partOfSpeech != null) normalizedItem.put("partOfSpeech", partOfSpeech);
            
            String example = getStringField(item, "example", "exampleSentence", "sentence", "usage");
            if (example != null) normalizedItem.put("example", example);
            
            String exampleTranslation = getStringField(item, "exampleTranslation", "translatedExample", "example_translation");
            if (exampleTranslation != null) normalizedItem.put("exampleTranslation", exampleTranslation);
            
            String usageNote = getStringField(item, "usageNote", "note", "notes", "usage_note", "tip");
            if (usageNote != null) normalizedItem.put("usageNote", usageNote);
            
            normalized.add(normalizedItem);
        }
        
        return normalized;
    }

    /**
     * Gets a string field from a JSON node, trying multiple possible field names.
     */
    private static String getStringField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.isTextual()) {
                return field.asText();
            }
        }
        return null;
    }

    /**
     * Creates an empty vocabulary response.
     */
    private static String createEmptyVocabulary() {
        return "{\"vocabulary\":[]}";
    }

    /**
     * Helper to convert Iterator to Iterable for enhanced for-loop.
     */
    private static <T> Iterable<T> iterable(java.util.Iterator<T> iterator) {
        return () -> iterator;
    }

    /**
     * Sanitizes JSON string to fix common issues from LLM output.
     * <p>
     * Fixes:
     * <ul>
     *   <li>Invalid control characters (ASCII 0-31) inside string values</li>
     *   <li>Invalid escape sequences like \' (not valid in JSON)</li>
     *   <li>Unescaped newlines and tabs inside strings</li>
     * </ul>
     *
     * @param json the raw JSON string
     * @return sanitized JSON string
     */
    private static String sanitizeJson(String json) {
        if (json == null) return null;
        
        StringBuilder result = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                // Handle escape sequences
                switch (c) {
                    case '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                        // Valid JSON escape sequences
                        result.append(c);
                    }
                    case 'u' -> {
                        // Unicode escape - needs 4 hex digits
                        result.append(c);
                    }
                    case '\'' -> {
                        // \' is not valid in JSON, replace with just '
                        result.append('\'');
                    }
                    default -> {
                        // Invalid escape - remove the backslash and keep the character
                        // or escape it properly if needed
                        if (c == '\n') {
                            result.append('n');
                        } else if (c == '\r') {
                            result.append('r');
                        } else if (c == '\t') {
                            result.append('t');
                        } else {
                            // Unknown escape - just append the character without backslash
                            result.append(c);
                        }
                    }
                }
                escaped = false;
            } else if (c == '\\' && inString) {
                result.append(c);
                escaped = true;
            } else if (c == '"') {
                result.append(c);
                inString = !inString;
            } else if (inString) {
                // Inside a string - handle control characters
                if (c < 32) {
                    // Replace control characters with their escape sequences
                    switch (c) {
                        case '\n' -> result.append("\\n");
                        case '\r' -> result.append("\\r");
                        case '\t' -> result.append("\\t");
                        case '\b' -> result.append("\\b");
                        case '\f' -> result.append("\\f");
                        default -> {
                            // Other control characters - encode as unicode
                            result.append(String.format("\\u%04x", (int) c));
                        }
                    }
                } else {
                    result.append(c);
                }
            } else {
                // Outside of string - keep as is
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
