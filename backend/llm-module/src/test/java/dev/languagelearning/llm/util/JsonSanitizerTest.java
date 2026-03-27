package dev.languagelearning.llm.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSanitizer}.
 * Tests various malformed JSON scenarios that LLMs commonly produce.
 */
class JsonSanitizerTest {

    @Nested
    @DisplayName("sanitizeAndValidate - Basic JSON extraction")
    class BasicExtraction {

        @Test
        @DisplayName("Should return valid JSON unchanged")
        void shouldReturnValidJsonUnchanged() throws JsonSanitizer.JsonSanitizationException {
            String validJson = "{\"word\":\"Hallo\",\"translation\":\"Hello\"}";
            String result = JsonSanitizer.sanitizeAndValidate(validJson);
            assertEquals(validJson, result);
        }

        @Test
        @DisplayName("Should extract JSON from markdown code block with json tag")
        void shouldExtractFromCodeBlockWithJsonTag() throws JsonSanitizer.JsonSanitizationException {
            String input = "Here's the vocabulary:\n```json\n{\"word\":\"Hallo\"}\n```\nEnjoy!";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            assertEquals("{\"word\":\"Hallo\"}", result);
        }

        @Test
        @DisplayName("Should extract JSON from markdown code block without json tag")
        void shouldExtractFromCodeBlockWithoutTag() throws JsonSanitizer.JsonSanitizationException {
            String input = "```\n{\"word\":\"Hallo\"}\n```";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            assertEquals("{\"word\":\"Hallo\"}", result);
        }

        @Test
        @DisplayName("Should strip leading text before JSON")
        void shouldStripLeadingText() throws JsonSanitizer.JsonSanitizationException {
            String input = "Here is your vocabulary: {\"word\":\"Hallo\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            assertEquals("{\"word\":\"Hallo\"}", result);
        }

        @Test
        @DisplayName("Should strip trailing text after JSON")
        void shouldStripTrailingText() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"word\":\"Hallo\"} I hope this helps!";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            assertEquals("{\"word\":\"Hallo\"}", result);
        }

        @Test
        @DisplayName("Should handle JSON array")
        void shouldHandleJsonArray() throws JsonSanitizer.JsonSanitizationException {
            String input = "[{\"word\":\"Hallo\"},{\"word\":\"Welt\"}]";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            assertEquals(input, result);
        }

        @Test
        @DisplayName("Should throw on empty input")
        void shouldThrowOnEmptyInput() {
            assertThrows(JsonSanitizer.JsonSanitizationException.class, () -> 
                JsonSanitizer.sanitizeAndValidate(""));
        }

        @Test
        @DisplayName("Should throw on null input")
        void shouldThrowOnNullInput() {
            assertThrows(JsonSanitizer.JsonSanitizationException.class, () -> 
                JsonSanitizer.sanitizeAndValidate(null));
        }
    }

    @Nested
    @DisplayName("sanitizeAndValidate - Control character handling")
    class ControlCharacterHandling {

        @Test
        @DisplayName("Should escape literal newline inside string")
        void shouldEscapeLiteralNewlineInsideString() throws JsonSanitizer.JsonSanitizationException {
            // JSON with literal newline inside string value
            String input = "{\"example\":\"Hello\nWorld\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result), "Result should be valid JSON");
            assertTrue(result.contains("\\n"), "Should contain escaped newline");
        }

        @Test
        @DisplayName("Should escape literal tab inside string")
        void shouldEscapeLiteralTabInsideString() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"Hello\tWorld\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertTrue(result.contains("\\t"), "Should contain escaped tab");
        }

        @Test
        @DisplayName("Should escape literal carriage return inside string")
        void shouldEscapeLiteralCrInsideString() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"Hello\rWorld\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertTrue(result.contains("\\r"), "Should contain escaped CR");
        }

        @Test
        @DisplayName("Should preserve already escaped sequences")
        void shouldPreserveAlreadyEscapedSequences() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"Line1\\nLine2\\tTabbed\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertEquals(input, result);
        }

        @Test
        @DisplayName("Should handle multiple control characters")
        void shouldHandleMultipleControlCharacters() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"Line1\nLine2\r\nLine3\tTabbed\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
        }

        @Test
        @DisplayName("Should encode other control characters as unicode")
        void shouldEncodeOtherControlCharsAsUnicode() throws JsonSanitizer.JsonSanitizationException {
            // ASCII 1 (SOH) inside a string
            String input = "{\"example\":\"Hello\u0001World\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertTrue(result.contains("\\u0001"), "Should contain unicode escape");
        }
    }

    @Nested
    @DisplayName("sanitizeAndValidate - Invalid escape sequences")
    class InvalidEscapeSequences {

        @Test
        @DisplayName("Should fix invalid backslash-apostrophe escape")
        void shouldFixBackslashApostrophe() throws JsonSanitizer.JsonSanitizationException {
            // \' is not valid in JSON
            String input = "{\"example\":\"It\\'s great\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertTrue(result.contains("'"), "Should contain apostrophe");
            assertFalse(result.contains("\\'"), "Should not contain invalid escape");
        }

        @Test
        @DisplayName("Should handle backslash followed by literal newline")
        void shouldHandleBackslashLiteralNewline() throws JsonSanitizer.JsonSanitizationException {
            // Backslash followed by actual newline character
            String input = "{\"example\":\"Line1\\\nLine2\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
        }

        @Test
        @DisplayName("Should preserve valid unicode escapes")
        void shouldPreserveValidUnicodeEscapes() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"Caf\\u00e9\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertEquals(input, result);
        }

        @Test
        @DisplayName("Should fix invalid unicode escapes")
        void shouldFixInvalidUnicodeEscapes() throws JsonSanitizer.JsonSanitizationException {
            // Backslash-u followed by non-hex characters
            String input = "{\"example\":\"Test" + "\\u" + "XYZW\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("sanitizeAndValidate - Structural issues")
    class StructuralIssues {

        @Test
        @DisplayName("Should remove trailing comma in object")
        void shouldRemoveTrailingCommaInObject() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"word\":\"Hallo\",\"translation\":\"Hello\",}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertFalse(result.contains(",}"), "Should not contain trailing comma");
        }

        @Test
        @DisplayName("Should remove trailing comma in array")
        void shouldRemoveTrailingCommaInArray() throws JsonSanitizer.JsonSanitizationException {
            String input = "[\"Hallo\",\"Welt\",]";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertFalse(result.contains(",]"), "Should not contain trailing comma");
        }

        @Test
        @DisplayName("Should remove BOM character")
        void shouldRemoveBomCharacter() throws JsonSanitizer.JsonSanitizationException {
            String input = "\uFEFF{\"word\":\"Hallo\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            assertFalse(result.contains("\uFEFF"), "Should not contain BOM");
        }

        @Test
        @DisplayName("Should remove zero-width characters")
        void shouldRemoveZeroWidthCharacters() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"word\":\"\u200BHallo\u200D\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("sanitizeAndValidate - Complex scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("Should handle nested objects with control characters")
        void shouldHandleNestedObjectsWithControlChars() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"vocabulary\":[{\"word\":\"Hallo\",\"example\":\"Hallo!\nWie geht's?\"}]}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            JsonNode node = JsonSanitizer.parseJson(result);
            assertNotNull(node);
            assertTrue(node.has("vocabulary"));
            assertTrue(node.get("vocabulary").isArray());
        }

        @Test
        @DisplayName("Should handle real LLM vocabulary response")
        void shouldHandleRealLlmResponse() throws JsonSanitizer.JsonSanitizationException {
            String input = """
                Here's a vocabulary list for you:
                
                ```json
                {
                  "vocabulary": [
                    {
                      "word": "der Apfel",
                      "translation": "the apple",
                      "example": "Der Apfel ist rot.
                The apple is red."
                    }
                  ]
                }
                ```
                
                Let me know if you need more!
                """;
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
            JsonNode node = JsonSanitizer.parseJson(result);
            assertNotNull(node);
            assertTrue(node.has("vocabulary"));
        }

        @Test
        @DisplayName("Should handle escaped quotes inside strings")
        void shouldHandleEscapedQuotesInsideStrings() throws JsonSanitizer.JsonSanitizationException {
            String input = "{\"example\":\"She said \\\"Hello\\\"\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertEquals(input, result);
            assertTrue(JsonSanitizer.isValidJson(result));
        }

        @Test
        @DisplayName("Should handle backslash before quote correctly")
        void shouldHandleBackslashBeforeQuote() throws JsonSanitizer.JsonSanitizationException {
            // \\\" means escaped backslash followed by quote
            String input = "{\"path\":\"C:\\\\Users\\\\Name\\\\\"}";
            String result = JsonSanitizer.sanitizeAndValidate(input);
            
            assertTrue(JsonSanitizer.isValidJson(result));
        }
    }

    @Nested
    @DisplayName("isValidJson")
    class IsValidJson {

        @Test
        @DisplayName("Should return true for valid JSON object")
        void shouldReturnTrueForValidObject() {
            assertTrue(JsonSanitizer.isValidJson("{\"key\":\"value\"}"));
        }

        @Test
        @DisplayName("Should return true for valid JSON array")
        void shouldReturnTrueForValidArray() {
            assertTrue(JsonSanitizer.isValidJson("[1,2,3]"));
        }

        @Test
        @DisplayName("Should return false for invalid JSON")
        void shouldReturnFalseForInvalidJson() {
            assertFalse(JsonSanitizer.isValidJson("{invalid}"));
        }

        @Test
        @DisplayName("Should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(JsonSanitizer.isValidJson(null));
        }

        @Test
        @DisplayName("Should return false for empty string")
        void shouldReturnFalseForEmptyString() {
            assertFalse(JsonSanitizer.isValidJson(""));
        }
    }

    @Nested
    @DisplayName("extractJsonBestEffort")
    class ExtractJsonBestEffort {

        @Test
        @DisplayName("Should return sanitized JSON on success")
        void shouldReturnSanitizedJsonOnSuccess() {
            String input = "```json\n{\"word\":\"Hallo\"}\n```";
            String result = JsonSanitizer.extractJsonBestEffort(input);
            assertEquals("{\"word\":\"Hallo\"}", result);
        }

        @Test
        @DisplayName("Should return original on failure")
        void shouldReturnOriginalOnFailure() {
            String input = "This is not JSON at all";
            String result = JsonSanitizer.extractJsonBestEffort(input);
            assertEquals(input, result);
        }
    }

    @Nested
    @DisplayName("parseJson")
    class ParseJson {

        @Test
        @DisplayName("Should parse valid JSON to JsonNode")
        void shouldParseValidJson() {
            JsonNode node = JsonSanitizer.parseJson("{\"key\":\"value\"}");
            assertNotNull(node);
            assertEquals("value", node.get("key").asText());
        }

        @Test
        @DisplayName("Should return null for invalid JSON")
        void shouldReturnNullForInvalidJson() {
            JsonNode node = JsonSanitizer.parseJson("{invalid}");
            assertNull(node);
        }
    }
}