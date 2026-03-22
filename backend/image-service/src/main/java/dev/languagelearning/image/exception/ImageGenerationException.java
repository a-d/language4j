package dev.languagelearning.image.exception;

import dev.languagelearning.core.exception.LanguageLearningException;

/**
 * Exception thrown when image generation fails.
 */
public class ImageGenerationException extends LanguageLearningException {

    public ImageGenerationException(String message) {
        super(message);
    }

    public ImageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ImageGenerationException providerError(String provider, String details) {
        return new ImageGenerationException(
                String.format("Image generation failed with %s: %s", provider, details)
        );
    }

    public static ImageGenerationException timeout() {
        return new ImageGenerationException("Image generation request timed out");
    }

    public static ImageGenerationException invalidPrompt(String reason) {
        return new ImageGenerationException("Invalid image prompt: " + reason);
    }
}