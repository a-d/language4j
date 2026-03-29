package dev.languagelearning.content.util;

import jakarta.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting JSON from LLM responses.
 * <p>
 * LLM responses often contain JSON wrapped in markdown code blocks or
 * surrounded by explanatory text. This class provides methods to extract
 * the raw JSON content from such responses.
 */
public final class JsonExtractor {

    // Pattern to match markdown code blocks with optional language tag
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?(.+?)\\n?```",
            Pattern.DOTALL
    );

    // Pattern to match JSON object (starts with { ends with })
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "(\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*})",
            Pattern.DOTALL
    );

    // Pattern to match JSON array (starts with [ ends with ])
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
            "(\\[[^\\[\\]]*(?:\\[[^\\[\\]]*][^\\[\\]]*)*])",
            Pattern.DOTALL
    );

    private JsonExtractor() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts JSON content from an LLM response.
     * <p>
     * The extraction follows this priority:
     * <ol>
     *   <li>If input is null, returns null</li>
     *   <li>If input is blank, returns it as-is</li>
     *   <li>If input starts with { or [, assumes it's clean JSON and returns as-is</li>
     *   <li>Attempts to extract from markdown code block (```json or ```)</li>
     *   <li>Attempts to find JSON object or array in the text</li>
     *   <li>Returns original input if no JSON found</li>
     * </ol>
     *
     * @param llmResponse the raw response from an LLM
     * @return the extracted JSON string, or the original input if no JSON found
     */
    @Nullable
    public static String extractJson(@Nullable String llmResponse) {
        if (llmResponse == null) {
            return null;
        }

        if (llmResponse.isBlank()) {
            return llmResponse;
        }

        String trimmed = llmResponse.trim();

        // If it already looks like clean JSON, return as-is
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        // Try to extract from markdown code block first
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(llmResponse);
        if (codeBlockMatcher.find()) {
            return codeBlockMatcher.group(1).trim();
        }

        // Try to find a JSON object in the text
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(llmResponse);
        if (objectMatcher.find()) {
            return objectMatcher.group(1).trim();
        }

        // Try to find a JSON array in the text
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(llmResponse);
        if (arrayMatcher.find()) {
            return arrayMatcher.group(1).trim();
        }

        // No JSON found, return original
        return llmResponse;
    }

    /**
     * Extracts JSON content and attempts to find deeply nested structures.
     * <p>
     * This is a more sophisticated version that handles deeply nested JSON
     * by tracking brace/bracket depth.
     *
     * @param llmResponse the raw response from an LLM
     * @return the extracted JSON string, or the original input if no JSON found
     */
    @Nullable
    public static String extractJsonDeep(@Nullable String llmResponse) {
        if (llmResponse == null) {
            return null;
        }

        if (llmResponse.isBlank()) {
            return llmResponse;
        }

        // Try code block first
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(llmResponse);
        if (codeBlockMatcher.find()) {
            return codeBlockMatcher.group(1).trim();
        }

        // Find JSON by tracking brace depth
        String trimmed = llmResponse.trim();
        int start = -1;
        char openChar = 0;
        char closeChar = 0;

        // Find the start of JSON
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') {
                start = i;
                openChar = '{';
                closeChar = '}';
                break;
            } else if (c == '[') {
                start = i;
                openChar = '[';
                closeChar = ']';
                break;
            }
        }

        if (start == -1) {
            return llmResponse;
        }

        // Track depth to find matching close
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == openChar) {
                    depth++;
                } else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        return trimmed.substring(start, i + 1);
                    }
                }
            }
        }

        // No complete JSON found
        return llmResponse;
    }
}