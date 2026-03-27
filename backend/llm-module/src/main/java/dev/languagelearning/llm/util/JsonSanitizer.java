package dev.languagelearning.llm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for sanitizing and extracting JSON from LLM responses.
 * <p>
 * LLMs often produce malformed JSON with:
 * <ul>
 *   <li>Control characters (newlines, tabs) inside string values</li>
 *   <li>Invalid escape sequences (\' instead of ')</li>
 *   <li>Markdown code blocks wrapping the JSON</li>
 *   <li>Explanatory text before/after JSON</li>
 *   <li>Trailing commas</li>
 * </ul>
 * This utility handles all these cases.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Pattern to match JSON in markdown code blocks.
     */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts, sanitizes, and validates JSON from an LLM response.
     * <p>
     * Processing steps:
     * <ol>
     *   <li>Extract JSON from markdown code blocks if present</li>
     *   <li>Strip leading/trailing non-JSON text</li>
     *   <li>Sanitize control characters and escape sequences</li>
     *   <li>Fix structural issues (trailing commas, etc.)</li>
     *   <li>Validate that result is parseable JSON</li>
     * </ol>
     *
     * @param llmResponse the raw LLM response
     * @return sanitized and validated JSON string
     * @throws JsonSanitizationException if JSON cannot be extracted or fixed
     */
    public static String sanitizeAndValidate(String llmResponse) throws JsonSanitizationException {
        if (llmResponse == null || llmResponse.isBlank()) {
            throw new JsonSanitizationException("Empty LLM response");
        }

        String json = llmResponse;

        // Step 1: Extract from markdown code blocks
        json = extractFromCodeBlock(json);

        // Step 2: Strip non-JSON prefix/suffix
        json = stripNonJsonText(json);

        // Step 3: Sanitize control characters and escape sequences
        json = sanitizeJsonString(json);

        // Step 4: Fix structural issues
        json = fixStructuralIssues(json);

        // Step 5: Validate
        if (!isValidJson(json)) {
            log.warn("JSON still invalid after sanitization, attempting aggressive repair");
            json = aggressiveRepair(json);
            
            if (!isValidJson(json)) {
                throw new JsonSanitizationException("Failed to produce valid JSON from LLM response");
            }
        }

        log.debug("JSON sanitization successful");
        return json;
    }

    /**
     * Extracts JSON without throwing exception on failure.
     * Returns the original string if extraction fails.
     *
     * @param llmResponse the raw LLM response
     * @return extracted/sanitized JSON or original string
     */
    public static String extractJsonBestEffort(String llmResponse) {
        try {
            return sanitizeAndValidate(llmResponse);
        } catch (JsonSanitizationException e) {
            log.warn("JSON extraction failed, returning original: {}", e.getMessage());
            return llmResponse;
        }
    }

    /**
     * Checks if a string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Parses JSON to a JsonNode, returning null if invalid.
     */
    public static JsonNode parseJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Extracts JSON from markdown code blocks.
     */
    private static String extractFromCodeBlock(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            log.debug("Extracted JSON from code block");
            return matcher.group(1).trim();
        }
        return text.trim();
    }

    /**
     * Strips non-JSON text before and after the JSON content.
     */
    private static String stripNonJsonText(String text) {
        String trimmed = text.trim();
        
        // Find first { or [
        int firstBrace = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{' || c == '[') {
                firstBrace = i;
                break;
            }
        }
        
        if (firstBrace > 0) {
            trimmed = trimmed.substring(firstBrace);
            log.debug("Stripped {} leading characters", firstBrace);
        }
        
        // Find matching closing brace/bracket from the end
        int lastBrace = Math.max(trimmed.lastIndexOf('}'), trimmed.lastIndexOf(']'));
        if (lastBrace >= 0 && lastBrace < trimmed.length() - 1) {
            int stripped = trimmed.length() - lastBrace - 1;
            trimmed = trimmed.substring(0, lastBrace + 1);
            log.debug("Stripped {} trailing characters", stripped);
        }
        
        return trimmed;
    }

    /**
     * Sanitizes JSON string content - handles control characters and escape sequences.
     */
    private static String sanitizeJsonString(String json) {
        StringBuilder result = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                // Handle escape sequences
                switch (c) {
                    case '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                        // Valid JSON escape sequences - keep as-is
                        result.append(c);
                    }
                    case 'u' -> {
                        // Unicode escape - validate 4 hex digits follow
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            if (hex.matches("[0-9a-fA-F]{4}")) {
                                result.append(c);
                            } else {
                                // Invalid unicode - skip backslash
                                result.deleteCharAt(result.length() - 1);
                                result.append(c);
                            }
                        } else {
                            result.append(c);
                        }
                    }
                    case '\'' -> {
                        // \' is not valid in JSON - convert to just '
                        result.deleteCharAt(result.length() - 1); // Remove the backslash
                        result.append('\'');
                    }
                    case '\n' -> {
                        // Literal newline after backslash - convert to \n
                        result.append('n');
                    }
                    case '\r' -> {
                        // Literal CR after backslash - convert to \r
                        result.append('r');
                    }
                    case '\t' -> {
                        // Literal tab after backslash - convert to \t
                        result.append('t');
                    }
                    default -> {
                        // Unknown escape - remove backslash, keep character
                        result.deleteCharAt(result.length() - 1);
                        result.append(c);
                    }
                }
                escaped = false;
            } else if (c == '\\' && inString) {
                result.append(c);
                escaped = true;
            } else if (c == '"') {
                // Check if this quote is escaped by counting preceding backslashes
                int backslashCount = 0;
                for (int j = result.length() - 1; j >= 0 && result.charAt(j) == '\\'; j--) {
                    backslashCount++;
                }
                if (backslashCount % 2 == 0) {
                    // Not escaped - toggle string state
                    inString = !inString;
                }
                result.append(c);
            } else if (inString) {
                // Inside a string - handle control characters
                if (c < 32) {
                    switch (c) {
                        case '\n' -> result.append("\\n");
                        case '\r' -> result.append("\\r");
                        case '\t' -> result.append("\\t");
                        case '\b' -> result.append("\\b");
                        case '\f' -> result.append("\\f");
                        default -> result.append(String.format("\\u%04x", (int) c));
                    }
                } else if (c == 127 || c == 0x2028 || c == 0x2029) {
                    // DEL, Line Separator, Paragraph Separator
                    result.append(String.format("\\u%04x", (int) c));
                } else {
                    result.append(c);
                }
            } else {
                // Outside string - keep whitespace, drop other control chars
                if (c >= 32 || c == '\n' || c == '\r' || c == '\t') {
                    result.append(c);
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Fixes structural JSON issues like trailing commas.
     */
    private static String fixStructuralIssues(String json) {
        // Remove trailing commas before } or ]
        String fixed = json.replaceAll(",\\s*([}\\]])", "$1");
        
        // Remove BOM and zero-width characters
        fixed = fixed.replace("\uFEFF", "")
                     .replace("\u200B", "")
                     .replace("\u200C", "")
                     .replace("\u200D", "")
                     .replace("\u2060", "");
        
        return fixed;
    }

    /**
     * Aggressive repair for severely malformed JSON.
     */
    private static String aggressiveRepair(String json) {
        // Try to find and extract just the JSON structure
        String trimmed = json.trim();
        
        // If it's an object
        if (trimmed.startsWith("{")) {
            int depth = 0;
            int end = -1;
            boolean inString = false;
            
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }
            
            if (end > 0) {
                return trimmed.substring(0, end + 1);
            }
        }
        
        // If it's an array
        if (trimmed.startsWith("[")) {
            int depth = 0;
            int end = -1;
            boolean inString = false;
            
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                    inString = !inString;
                } else if (!inString) {
                    if (c == '[') depth++;
                    else if (c == ']') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }
            
            if (end > 0) {
                return trimmed.substring(0, end + 1);
            }
        }
        
        return json;
    }

    /**
     * Exception thrown when JSON sanitization fails.
     */
    public static class JsonSanitizationException extends Exception {
        public JsonSanitizationException(String message) {
            super(message);
        }

        public JsonSanitizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}