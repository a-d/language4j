package dev.languagelearning.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PromptTemplate}.
 */
class PromptTemplateTest {

    @Nested
    @DisplayName("of(template)")
    class FactoryMethod {

        @Test
        @DisplayName("should create template with given string")
        void shouldCreateTemplateWithGivenString() {
            // Given
            String templateString = "Hello {name}";

            // When
            PromptTemplate template = PromptTemplate.of(templateString);

            // Then
            assertThat(template.getTemplate()).isEqualTo(templateString);
        }
    }

    @Nested
    @DisplayName("render(variables)")
    class Render {

        @Test
        @DisplayName("should replace single placeholder")
        void shouldReplaceSinglePlaceholder() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}!");
            Map<String, Object> variables = Map.of("name", "World");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("should replace multiple placeholders")
        void shouldReplaceMultiplePlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("Translate '{word}' from {source} to {target}");
            Map<String, Object> variables = Map.of(
                    "word", "Hello",
                    "source", "English",
                    "target", "French"
            );

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Translate 'Hello' from English to French");
        }

        @Test
        @DisplayName("should replace same placeholder multiple times")
        void shouldReplaceSamePlaceholderMultipleTimes() {
            // Given
            PromptTemplate template = PromptTemplate.of("{lang} to {lang} translation");
            Map<String, Object> variables = Map.of("lang", "French");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("French to French translation");
        }

        @Test
        @DisplayName("should handle integer variables")
        void shouldHandleIntegerVariables() {
            // Given
            PromptTemplate template = PromptTemplate.of("Generate {count} words");
            Map<String, Object> variables = Map.of("count", 10);

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Generate 10 words");
        }

        @Test
        @DisplayName("should handle boolean variables")
        void shouldHandleBooleanVariables() {
            // Given
            PromptTemplate template = PromptTemplate.of("Include examples: {include}");
            Map<String, Object> variables = Map.of("include", true);

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Include examples: true");
        }

        @Test
        @DisplayName("should return template unchanged when no placeholders")
        void shouldReturnTemplateUnchangedWhenNoPlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("No placeholders here");
            Map<String, Object> variables = Map.of("unused", "value");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("No placeholders here");
        }

        @Test
        @DisplayName("should throw exception for missing placeholder value")
        void shouldThrowExceptionForMissingPlaceholderValue() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}, welcome to {place}");
            Map<String, Object> variables = Map.of("name", "Alice");

            // When/Then
            assertThatThrownBy(() -> template.render(variables))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing value for placeholder")
                    .hasMessageContaining("{place}");
        }

        @Test
        @DisplayName("should throw exception when all placeholders missing")
        void shouldThrowExceptionWhenAllPlaceholdersMissing() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}");
            Map<String, Object> variables = Collections.emptyMap();

            // When/Then
            assertThatThrownBy(() -> template.render(variables))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing value for placeholder")
                    .hasMessageContaining("{name}");
        }

        @Test
        @DisplayName("should handle special regex characters in replacement")
        void shouldHandleSpecialRegexCharactersInReplacement() {
            // Given
            PromptTemplate template = PromptTemplate.of("Pattern: {pattern}");
            Map<String, Object> variables = Map.of("pattern", "$100.00");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Pattern: $100.00");
        }

        @Test
        @DisplayName("should handle backslashes in replacement")
        void shouldHandleBackslashesInReplacement() {
            // Given
            PromptTemplate template = PromptTemplate.of("Path: {path}");
            Map<String, Object> variables = Map.of("path", "C:\\Users\\Test");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Path: C:\\Users\\Test");
        }

        @Test
        @DisplayName("should preserve multiline template structure")
        void shouldPreserveMultilineTemplateStructure() {
            // Given
            String templateString = """
                    Line 1: {first}
                    Line 2: {second}
                    Line 3: {third}
                    """;
            PromptTemplate template = PromptTemplate.of(templateString);
            Map<String, Object> variables = Map.of(
                    "first", "A",
                    "second", "B",
                    "third", "C"
            );

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).contains("Line 1: A");
            assertThat(result).contains("Line 2: B");
            assertThat(result).contains("Line 3: C");
        }

        @Test
        @DisplayName("should handle underscore in variable names")
        void shouldHandleUnderscoreInVariableNames() {
            // Given
            PromptTemplate template = PromptTemplate.of("User: {user_name}, Level: {skill_level}");
            Map<String, Object> variables = Map.of(
                    "user_name", "Alice",
                    "skill_level", "B1"
            );

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("User: Alice, Level: B1");
        }

        @Test
        @DisplayName("should handle numbers in variable names")
        void shouldHandleNumbersInVariableNames() {
            // Given
            PromptTemplate template = PromptTemplate.of("Option1: {opt1}, Option2: {opt2}");
            Map<String, Object> variables = Map.of(
                    "opt1", "First",
                    "opt2", "Second"
            );

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("Option1: First, Option2: Second");
        }
    }

    @Nested
    @DisplayName("renderSafe(variables)")
    class RenderSafe {

        @Test
        @DisplayName("should replace placeholders with values")
        void shouldReplacePlaceholdersWithValues() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}!");
            Map<String, Object> variables = Map.of("name", "World");

            // When
            String result = template.renderSafe(variables);

            // Then
            assertThat(result).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("should use empty string for missing placeholders")
        void shouldUseEmptyStringForMissingPlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}, welcome to {place}!");
            Map<String, Object> variables = Map.of("name", "Alice");

            // When
            String result = template.renderSafe(variables);

            // Then
            assertThat(result).isEqualTo("Hello Alice, welcome to !");
        }

        @Test
        @DisplayName("should handle all placeholders missing")
        void shouldHandleAllPlaceholdersMissing() {
            // Given
            PromptTemplate template = PromptTemplate.of("Hello {name}!");
            Map<String, Object> variables = Collections.emptyMap();

            // When
            String result = template.renderSafe(variables);

            // Then
            assertThat(result).isEqualTo("Hello !");
        }

        @Test
        @DisplayName("should not throw exception for missing values")
        void shouldNotThrowExceptionForMissingValues() {
            // Given
            PromptTemplate template = PromptTemplate.of("{a} {b} {c}");
            Map<String, Object> variables = Map.of("b", "middle");

            // When
            String result = template.renderSafe(variables);

            // Then
            assertThat(result).isEqualTo(" middle ");
        }
    }

    @Nested
    @DisplayName("withSystem(systemMessage, userMessage)")
    class WithSystem {

        @Test
        @DisplayName("should combine system and user messages")
        void shouldCombineSystemAndUserMessages() {
            // Given
            String systemMessage = "You are a helpful assistant";
            String userMessage = "What is 2+2?";

            // When
            PromptTemplate template = PromptTemplate.withSystem(systemMessage, userMessage);

            // Then
            assertThat(template.getTemplate()).contains("System: You are a helpful assistant");
            assertThat(template.getTemplate()).contains("User: What is 2+2?");
        }

        @Test
        @DisplayName("should support placeholders in combined template")
        void shouldSupportPlaceholdersInCombinedTemplate() {
            // Given
            String systemMessage = "You are a {language} tutor";
            String userMessage = "Teach me {topic}";

            // When
            PromptTemplate template = PromptTemplate.withSystem(systemMessage, userMessage);
            String result = template.render(Map.of("language", "French", "topic", "greetings"));

            // Then
            assertThat(result).contains("System: You are a French tutor");
            assertThat(result).contains("User: Teach me greetings");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty template")
        void shouldHandleEmptyTemplate() {
            // Given
            PromptTemplate template = PromptTemplate.of("");

            // When
            String result = template.render(Collections.emptyMap());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle template with only placeholder")
        void shouldHandleTemplateWithOnlyPlaceholder() {
            // Given
            PromptTemplate template = PromptTemplate.of("{value}");
            Map<String, Object> variables = Map.of("value", "test");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("should not match malformed placeholders")
        void shouldNotMatchMalformedPlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("{ name } and {name}");
            Map<String, Object> variables = Map.of("name", "Alice");

            // When
            String result = template.render(variables);

            // Then
            // Only {name} should be replaced, not { name } (with spaces)
            assertThat(result).isEqualTo("{ name } and Alice");
        }

        @Test
        @DisplayName("should handle consecutive placeholders")
        void shouldHandleConsecutivePlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("{first}{second}{third}");
            Map<String, Object> variables = Map.of(
                    "first", "A",
                    "second", "B",
                    "third", "C"
            );

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("ABC");
        }

        @Test
        @DisplayName("should handle curly braces that are not placeholders")
        void shouldHandleCurlyBracesThatAreNotPlaceholders() {
            // Given
            PromptTemplate template = PromptTemplate.of("JSON: {\"key\": \"{value}\"}");
            Map<String, Object> variables = Map.of("value", "test");

            // When
            String result = template.render(variables);

            // Then
            assertThat(result).isEqualTo("JSON: {\"key\": \"test\"}");
        }
    }
}