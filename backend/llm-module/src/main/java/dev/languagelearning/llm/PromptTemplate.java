package dev.languagelearning.llm;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template for constructing prompts with variable substitution.
 * <p>
 * Supports placeholders in the format {variableName} which can be
 * replaced with actual values at runtime.
 * <p>
 * Example:
 * <pre>{@code
 * PromptTemplate template = PromptTemplate.of(
 *     "Translate the following {sourceLanguage} text to {targetLanguage}: {text}"
 * );
 * String prompt = template.render(Map.of(
 *     "sourceLanguage", "German",
 *     "targetLanguage", "French",
 *     "text", "Guten Morgen"
 * ));
 * }</pre>
 */
@Getter
@RequiredArgsConstructor(staticName = "of")
public class PromptTemplate {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    /**
     * The template string with placeholders.
     */
    @Nonnull
    private final String template;

    /**
     * Renders the template by replacing placeholders with provided values.
     *
     * @param variables map of variable names to values
     * @return the rendered prompt string
     * @throws IllegalArgumentException if a required placeholder has no value
     */
    @Nonnull
    public String render(@Nonnull Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            
            if (value == null) {
                throw new IllegalArgumentException(
                        "Missing value for placeholder: {" + variableName + "}"
                );
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Renders the template, using empty string for missing placeholders.
     *
     * @param variables map of variable names to values
     * @return the rendered prompt string
     */
    @Nonnull
    public String renderSafe(@Nonnull Map<String, Object> variables) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.getOrDefault(variableName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Creates a template with a system message and user message.
     *
     * @param systemMessage the system message template
     * @param userMessage   the user message template
     * @return combined template
     */
    public static PromptTemplate withSystem(@Nonnull String systemMessage, @Nonnull String userMessage) {
        return PromptTemplate.of(
                "System: " + systemMessage + "\n\nUser: " + userMessage
        );
    }
}