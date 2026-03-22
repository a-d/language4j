package dev.languagelearning.llm.exception;

import dev.languagelearning.core.exception.LanguageLearningException;

/**
 * Exception thrown when LLM operations fail.
 */
public class LlmException extends LanguageLearningException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception for provider unavailability.
     */
    public static LlmException providerUnavailable(String provider) {
        return new LlmException("LLM provider unavailable: " + provider);
    }

    /**
     * Creates an exception for rate limiting.
     */
    public static LlmException rateLimited() {
        return new LlmException("LLM request rate limited. Please try again later.");
    }

    /**
     * Creates an exception for timeout.
     */
    public static LlmException timeout() {
        return new LlmException("LLM request timed out");
    }

    /**
     * Creates an exception for invalid response.
     */
    public static LlmException invalidResponse(String details) {
        return new LlmException("Invalid LLM response: " + details);
    }
}