package dev.languagelearning.content.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting JSON from LLM responses.
 * <p>
 * LLMs often wrap JSON responses in markdown code blocks or add explanatory text.
 * This utility extracts the actual JSON content.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonExtractor {

    /**
     * Pattern to match JSON in markdown code blocks: ```json ... ``` or ``` ... ```
     */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern to match a JSON object: { ... }
     */
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "\\{[\\s\\S]*\\}",
            Pattern.DOTALL
    );

    /**
     * Pattern to match a JSON array: [ ... ]
     */
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
            "\\[[\\s\\S]*\\]",
            Pattern.DOTALL
    );

    /**
     * Extracts JSON from an LLM response.
     * <p>
     * Handles the following cases:
     * <ul>
     *   <li>JSON wrapped in markdown code blocks (```json ... ``` or ``` ... ```)</li>
     *   <li>JSON with leading/trailing explanatory text</li>
     *   <li>Clean JSON (returned as-is)</li>
     * </ul>
     *
     * @param llmResponse the raw LLM response
     * @return the extracted JSON string, or the original string if no JSON found
     */
    public static String extractJson(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return llmResponse;
        }

        String trimmed = llmResponse.trim();

        // First, try to extract from markdown code blocks
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(trimmed);
        if (codeBlockMatcher.find()) {
            String extracted = codeBlockMatcher.group(1).trim();
            log.debug("Extracted JSON from code block");
            return extracted;
        }

        // If no code block, check if the content already starts with { or [
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        // Try to find a JSON array first (arrays contain objects, so check arrays first)
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(trimmed);
        if (arrayMatcher.find()) {
            String extracted = arrayMatcher.group().trim();
            if (isBalancedJson(extracted)) {
                log.debug("Extracted JSON array from text");
                return extracted;
            }
        }

        // Try to find a JSON object in the response
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(trimmed);
        if (objectMatcher.find()) {
            String extracted = objectMatcher.group().trim();
            // Validate it's balanced (simple check)
            if (isBalancedJson(extracted)) {
                log.debug("Extracted JSON object from text");
                return extracted;
            }
        }

        // Return original if no JSON found
        log.warn("Could not extract JSON from LLM response, returning original");
        return trimmed;
    }

    /**
     * Simple check if JSON braces/brackets are balanced.
     * This is a heuristic, not a full JSON validator.
     */
    private static boolean isBalancedJson(String json) {
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char prevChar = 0;

        for (char c : json.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            } else if (!inString) {
                switch (c) {
                    case '{' -> braceCount++;
                    case '}' -> braceCount--;
                    case '[' -> bracketCount++;
                    case ']' -> bracketCount--;
                }
            }
            prevChar = c;
        }

        return braceCount == 0 && bracketCount == 0;
    }
}