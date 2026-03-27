package dev.languagelearning.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.languagelearning.llm.LlmService;
import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LlmJsonGenerator}.
 * Tests retry mechanism and JSON validation with mocked LLM service.
 */
@ExtendWith(MockitoExtension.class)
class LlmJsonGeneratorTest {

    @Mock
    private LlmService llmService;

    private LlmJsonGenerator generator;

    @BeforeEach
    void setUp() {
        // Create generator with 3 retries and no delay for fast tests
        generator = new LlmJsonGenerator(llmService, 3, 0);
    }

    @Nested
    @DisplayName("generateJson - Success scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return valid JSON on first attempt")
        void shouldReturnValidJsonOnFirstAttempt() {
            String validJson = "{\"word\":\"Hallo\",\"translation\":\"Hello\"}";
            when(llmService.generate(anyString())).thenReturn(validJson);

            String result = generator.generateJson("Generate vocabulary");

            assertEquals(validJson, result);
            verify(llmService, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("Should extract and return JSON from markdown code block")
        void shouldExtractFromCodeBlock() {
            String llmResponse = "Here's the JSON:\n```json\n{\"word\":\"Hallo\"}\n```";
            when(llmService.generate(anyString())).thenReturn(llmResponse);

            String result = generator.generateJson("Generate vocabulary");

            assertEquals("{\"word\":\"Hallo\"}", result);
            verify(llmService, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("Should sanitize control characters in JSON")
        void shouldSanitizeControlCharacters() {
            // JSON with literal newline in string value
            String llmResponse = "{\"example\":\"Line1\nLine2\"}";
            when(llmService.generate(anyString())).thenReturn(llmResponse);

            String result = generator.generateJson("Generate example");

            assertTrue(result.contains("\\n"), "Should contain escaped newline");
            assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(result));
        }

        @Test
        @DisplayName("Should work with system and user prompts")
        void shouldWorkWithSystemAndUserPrompts() {
            String validJson = "{\"data\":\"test\"}";
            when(llmService.generate(anyString(), anyString())).thenReturn(validJson);

            String result = generator.generateJson("System prompt", "User prompt");

            assertEquals(validJson, result);
            verify(llmService, times(1)).generate("System prompt", "User prompt");
        }

        @Test
        @DisplayName("Should work with prompt template")
        void shouldWorkWithPromptTemplate() {
            String validJson = "{\"topic\":\"food\"}";
            PromptTemplate template = mock(PromptTemplate.class);
            Map<String, Object> variables = Map.of("topic", "food");
            
            when(llmService.generate(any(PromptTemplate.class), anyMap())).thenReturn(validJson);

            String result = generator.generateJson(template, variables);

            assertEquals(validJson, result);
            verify(llmService, times(1)).generate(template, variables);
        }
    }

    @Nested
    @DisplayName("generateJson - Retry scenarios")
    class RetryScenarios {

        @Test
        @DisplayName("Should retry and succeed on second attempt")
        void shouldRetryAndSucceedOnSecondAttempt() {
            String invalidJson = "This is not JSON";
            String validJson = "{\"word\":\"Hallo\"}";
            
            when(llmService.generate(anyString()))
                    .thenReturn(invalidJson)  // First attempt fails
                    .thenReturn(validJson);   // Second attempt succeeds

            String result = generator.generateJson("Generate vocabulary");

            assertEquals(validJson, result);
            verify(llmService, times(2)).generate(anyString());
        }

        @Test
        @DisplayName("Should retry and succeed on third attempt")
        void shouldRetryAndSucceedOnThirdAttempt() {
            String invalidJson1 = "Not JSON at all";
            String invalidJson2 = "{incomplete";
            String validJson = "{\"word\":\"Hallo\"}";
            
            when(llmService.generate(anyString()))
                    .thenReturn(invalidJson1)  // First attempt fails
                    .thenReturn(invalidJson2)  // Second attempt fails
                    .thenReturn(validJson);    // Third attempt succeeds

            String result = generator.generateJson("Generate vocabulary");

            assertEquals(validJson, result);
            verify(llmService, times(3)).generate(anyString());
        }

        @Test
        @DisplayName("Should throw after max retries exhausted")
        void shouldThrowAfterMaxRetriesExhausted() {
            String invalidJson = "Not valid JSON";
            
            when(llmService.generate(anyString()))
                    .thenReturn(invalidJson);  // Always returns invalid JSON

            LlmException exception = assertThrows(LlmException.class, () ->
                    generator.generateJson("Generate vocabulary"));

            assertTrue(exception.getMessage().contains("Failed to generate valid JSON after 3 attempts"));
            verify(llmService, times(3)).generate(anyString());
        }

        @Test
        @DisplayName("Should not retry on LLM service error")
        void shouldNotRetryOnLlmServiceError() {
            when(llmService.generate(anyString()))
                    .thenThrow(new LlmException("API error"));

            assertThrows(LlmException.class, () ->
                    generator.generateJson("Generate vocabulary"));

            // Should not retry - LlmException is propagated immediately
            verify(llmService, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("Should retry when JSON is malformed with control characters")
        void shouldRetryWhenJsonMalformed() {
            // First response: JSON that's truly broken
            String brokenJson = "{\"word\":";
            // Second response: Valid JSON with control char (which will be sanitized)
            String goodJson = "{\"word\":\"Hallo\"}";
            
            when(llmService.generate(anyString()))
                    .thenReturn(brokenJson)
                    .thenReturn(goodJson);

            String result = generator.generateJson("Generate vocabulary");

            assertEquals(goodJson, result);
            verify(llmService, times(2)).generate(anyString());
        }
    }

    @Nested
    @DisplayName("generateTyped")
    class GenerateTyped {

        @Test
        @DisplayName("Should deserialize JSON to target type")
        void shouldDeserializeToTargetType() {
            String json = "{\"word\":\"Hallo\",\"translation\":\"Hello\"}";
            PromptTemplate template = mock(PromptTemplate.class);
            Map<String, Object> variables = Map.of();
            
            when(llmService.generate(any(PromptTemplate.class), anyMap())).thenReturn(json);

            VocabularyItem result = generator.generateTyped(template, variables, VocabularyItem.class);

            assertNotNull(result);
            assertEquals("Hallo", result.word);
            assertEquals("Hello", result.translation);
        }

        @Test
        @DisplayName("Should throw on deserialization failure")
        void shouldThrowOnDeserializationFailure() {
            // JSON doesn't match expected structure at all - wrong type
            String json = "[\"not\", \"an\", \"object\"]";
            PromptTemplate template = mock(PromptTemplate.class);
            Map<String, Object> variables = Map.of();
            
            when(llmService.generate(any(PromptTemplate.class), anyMap())).thenReturn(json);

            // Should throw because array cannot be deserialized to VocabularyItem
            assertThrows(LlmException.class, () -> 
                generator.generateTyped(template, variables, VocabularyItem.class));
        }

        // Test record for deserialization
        record VocabularyItem(String word, String translation) {}
    }

    @Nested
    @DisplayName("generateJsonNode")
    class GenerateJsonNode {

        @Test
        @DisplayName("Should return JsonNode for valid JSON")
        void shouldReturnJsonNode() {
            String json = "{\"vocabulary\":[{\"word\":\"Hallo\"}]}";
            PromptTemplate template = mock(PromptTemplate.class);
            Map<String, Object> variables = Map.of();
            
            when(llmService.generate(any(PromptTemplate.class), anyMap())).thenReturn(json);

            JsonNode result = generator.generateJsonNode(template, variables);

            assertNotNull(result);
            assertTrue(result.has("vocabulary"));
            assertTrue(result.get("vocabulary").isArray());
            assertEquals("Hallo", result.get("vocabulary").get(0).get("word").asText());
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("Should respect custom max retries setting")
        void shouldRespectCustomMaxRetries() {
            // Create generator with only 1 retry
            LlmJsonGenerator singleRetryGenerator = new LlmJsonGenerator(llmService, 1, 0);
            
            when(llmService.generate(anyString()))
                    .thenReturn("invalid json");

            assertThrows(LlmException.class, () ->
                    singleRetryGenerator.generateJson("test"));

            verify(llmService, times(1)).generate(anyString());
        }

        @Test
        @DisplayName("Should respect retry delay setting")
        void shouldRespectRetryDelay() {
            // Create generator with 100ms delay
            LlmJsonGenerator delayedGenerator = new LlmJsonGenerator(llmService, 2, 100);
            
            when(llmService.generate(anyString()))
                    .thenReturn("invalid")
                    .thenReturn("{\"valid\":true}");

            long startTime = System.currentTimeMillis();
            delayedGenerator.generateJson("test");
            long elapsed = System.currentTimeMillis() - startTime;

            // Should have at least 100ms delay between retries
            assertTrue(elapsed >= 100, "Should have delay between retries");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle JSON array response")
        void shouldHandleJsonArray() {
            String jsonArray = "[{\"word\":\"Hallo\"},{\"word\":\"Welt\"}]";
            when(llmService.generate(anyString())).thenReturn(jsonArray);

            String result = generator.generateJson("Generate vocabulary");

            assertEquals(jsonArray, result);
        }

        @Test
        @DisplayName("Should handle deeply nested JSON")
        void shouldHandleDeeplyNestedJson() {
            String nestedJson = "{\"level1\":{\"level2\":{\"level3\":{\"value\":\"deep\"}}}}";
            when(llmService.generate(anyString())).thenReturn(nestedJson);

            String result = generator.generateJson("Generate nested");

            assertEquals(nestedJson, result);
        }

        @Test
        @DisplayName("Should handle JSON with special characters in values")
        void shouldHandleSpecialCharacters() {
            String json = "{\"example\":\"Quotes: \\\"hello\\\", Backslash: \\\\\"}";
            when(llmService.generate(anyString())).thenReturn(json);

            String result = generator.generateJson("Generate example");

            assertEquals(json, result);
        }

        @Test
        @DisplayName("Should handle empty JSON object")
        void shouldHandleEmptyObject() {
            String emptyJson = "{}";
            when(llmService.generate(anyString())).thenReturn(emptyJson);

            String result = generator.generateJson("Generate empty");

            assertEquals(emptyJson, result);
        }

        @Test
        @DisplayName("Should handle empty JSON array")
        void shouldHandleEmptyArray() {
            String emptyArray = "[]";
            when(llmService.generate(anyString())).thenReturn(emptyArray);

            String result = generator.generateJson("Generate empty array");

            assertEquals(emptyArray, result);
        }
    }
}