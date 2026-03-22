package dev.languagelearning.speech.exception;

import dev.languagelearning.core.exception.LanguageLearningException;

/**
 * Exception thrown when speech processing fails.
 */
public class SpeechException extends LanguageLearningException {

    public SpeechException(String message) {
        super(message);
    }

    public SpeechException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SpeechException textToSpeechFailed(String reason) {
        return new SpeechException("Text-to-speech failed: " + reason);
    }

    public static SpeechException transcriptionFailed(String reason) {
        return new SpeechException("Speech transcription failed: " + reason);
    }

    public static SpeechException invalidAudioFormat(String format) {
        return new SpeechException("Invalid or unsupported audio format: " + format);
    }

    public static SpeechException audioTooLong(int maxSeconds) {
        return new SpeechException("Audio exceeds maximum length of " + maxSeconds + " seconds");
    }

    public static SpeechException providerNotConfigured(String provider) {
        return new SpeechException("Speech provider not configured: " + provider);
    }
}