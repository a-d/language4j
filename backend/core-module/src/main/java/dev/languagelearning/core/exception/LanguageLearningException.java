package dev.languagelearning.core.exception;

/**
 * Base exception for all language learning platform exceptions.
 * <p>
 * All domain-specific exceptions should extend this class.
 */
public class LanguageLearningException extends RuntimeException {

    public LanguageLearningException(String message) {
        super(message);
    }

    public LanguageLearningException(String message, Throwable cause) {
        super(message, cause);
    }
}