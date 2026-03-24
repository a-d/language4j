package dev.languagelearning.content.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonExtractor utility.
 */
class JsonExtractorTest {

    @Nested
    @DisplayName("extractJson")
    class ExtractJson {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(JsonExtractor.extractJson(null));
        }

        @Test
        @DisplayName("should return blank for blank input")
        void shouldReturnBlankForBlankInput() {
            assertEquals("", JsonExtractor.extractJson(""));
            assertEquals("   ", JsonExtractor.extractJson("   "));
        }

        @Test
        @DisplayName("should return clean JSON object as-is")
        void shouldReturnCleanJsonObjectAsIs() {
            String json = "{\"name\": \"test\", \"value\": 123}";
            assertEquals(json, JsonExtractor.extractJson(json));
        }

        @Test
        @DisplayName("should return clean JSON array as-is")
        void shouldReturnCleanJsonArrayAsIs() {
            String json = "[{\"name\": \"test\"}, {\"name\": \"test2\"}]";
            assertEquals(json, JsonExtractor.extractJson(json));
        }

        @Test
        @DisplayName("should extract JSON from markdown code block with json tag")
        void shouldExtractJsonFromMarkdownCodeBlockWithJsonTag() {
            String input = """
                    Here is the JSON:
                    ```json
                    {"exercises": [{"sentence": "test"}]}
                    ```
                    Hope this helps!
                    """;
            String expected = "{\"exercises\": [{\"sentence\": \"test\"}]}";
            assertEquals(expected, JsonExtractor.extractJson(input));
        }

        @Test
        @DisplayName("should extract JSON from markdown code block without json tag")
        void shouldExtractJsonFromMarkdownCodeBlockWithoutJsonTag() {
            String input = """
                    ```
                    {"exercises": [{"sentence": "test"}]}
                    ```
                    """;
            String expected = "{\"exercises\": [{\"sentence\": \"test\"}]}";
            assertEquals(expected, JsonExtractor.extractJson(input));
        }

        @Test
        @DisplayName("should extract JSON from multiline code block")
        void shouldExtractJsonFromMultilineCodeBlock() {
            String input = """
                    Here are the exercises:
                    ```json
                    {
                      "exercises": [
                        {
                          "sentence": "Ich ___ Deutsch.",
                          "answer": "spreche"
                        }
                      ]
                    }
                    ```
                    Let me know if you need more!
                    """;
            String result = JsonExtractor.extractJson(input);
            assertTrue(result.startsWith("{"));
            assertTrue(result.endsWith("}"));
            assertTrue(result.contains("\"exercises\""));
        }

        @Test
        @DisplayName("should extract JSON object from text without code block")
        void shouldExtractJsonObjectFromTextWithoutCodeBlock() {
            String input = """
                    Here is your response:
                    {"score": 85, "correct": true}
                    That's a great score!
                    """;
            String result = JsonExtractor.extractJson(input);
            assertEquals("{\"score\": 85, \"correct\": true}", result);
        }

        @Test
        @DisplayName("should extract JSON array from text without code block")
        void shouldExtractJsonArrayFromTextWithoutCodeBlock() {
            String input = """
                    The flashcards are:
                    [{"front": "Hallo", "back": "Hello"}]
                    """;
            String result = JsonExtractor.extractJson(input);
            assertEquals("[{\"front\": \"Hallo\", \"back\": \"Hello\"}]", result);
        }

        @Test
        @DisplayName("should handle nested JSON structures")
        void shouldHandleNestedJsonStructures() {
            String input = """
                    ```json
                    {
                      "flashcards": [
                        {
                          "front": "Guten Tag",
                          "back": {
                            "translation": "Good day",
                            "pronunciation": "goo-ten tahk"
                          }
                        }
                      ]
                    }
                    ```
                    """;
            String result = JsonExtractor.extractJson(input);
            assertTrue(result.contains("\"flashcards\""));
            assertTrue(result.contains("\"translation\""));
        }

        @Test
        @DisplayName("should handle JSON with special characters in strings")
        void shouldHandleJsonWithSpecialCharacters() {
            String input = """
                    ```json
                    {"text": "Er sagte: \\"Hallo!\\"", "value": 123}
                    ```
                    """;
            String result = JsonExtractor.extractJson(input);
            assertTrue(result.contains("\\\"Hallo!\\\""));
        }

        @Test
        @DisplayName("should return original when no JSON found")
        void shouldReturnOriginalWhenNoJsonFound() {
            String input = "This is just plain text without any JSON.";
            assertEquals(input, JsonExtractor.extractJson(input));
        }

        @Test
        @DisplayName("should handle real-world LLM response for exercises")
        void shouldHandleRealWorldLlmResponseForExercises() {
            String input = """
                    I'll create some fill-in-the-blank exercises for you about food vocabulary:
                    
                    ```json
                    {
                      "exercises": [
                        {
                          "sentence": "Ich möchte einen ___ kaufen.",
                          "wordBank": ["Apfel", "Buch", "Auto"],
                          "correctAnswer": "Apfel",
                          "explanation": "We're talking about food, so 'Apfel' (apple) is correct."
                        },
                        {
                          "sentence": "Das ___ schmeckt sehr gut.",
                          "wordBank": ["Brot", "Tisch", "Stuhl"],
                          "correctAnswer": "Brot",
                          "explanation": "'Brot' (bread) is a food item that can taste good."
                        }
                      ]
                    }
                    ```
                    
                    These exercises focus on A1-level food vocabulary. Let me know if you'd like more!
                    """;
            
            String result = JsonExtractor.extractJson(input);
            assertTrue(result.startsWith("{"));
            assertTrue(result.endsWith("}"));
            assertTrue(result.contains("\"exercises\""));
            assertTrue(result.contains("\"Apfel\""));
            assertFalse(result.contains("I'll create"));
            assertFalse(result.contains("Let me know"));
        }

        @Test
        @DisplayName("should prefer code block extraction over text extraction")
        void shouldPreferCodeBlockOverTextExtraction() {
            String input = """
                    Here is some JSON: {"wrong": true}
                    ```json
                    {"correct": true}
                    ```
                    """;
            String result = JsonExtractor.extractJson(input);
            assertEquals("{\"correct\": true}", result);
        }
    }
}