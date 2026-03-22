package dev.languagelearning.image.service;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * Service for generating and managing images for learning content.
 * <p>
 * Provides AI-powered image generation for flashcards, vocabulary cards,
 * and other visual learning materials.
 */
public interface ImageService {

    /**
     * Generates an image based on the given prompt.
     *
     * @param prompt the description of the image to generate
     * @return generated image data containing URL or base64 encoded image
     */
    @Nonnull
    GeneratedImage generate(@Nonnull String prompt);

    /**
     * Generates an image asynchronously.
     *
     * @param prompt the description of the image to generate
     * @return CompletableFuture with the generated image
     */
    @Nonnull
    CompletableFuture<GeneratedImage> generateAsync(@Nonnull String prompt);

    /**
     * Generates an image for a flashcard with the given word and context.
     *
     * @param word        the vocabulary word
     * @param targetLang  the target language code
     * @param context     optional context or sentence
     * @return generated image suitable for a flashcard
     */
    @Nonnull
    GeneratedImage generateFlashcardImage(@Nonnull String word, @Nonnull String targetLang, String context);

    /**
     * Generates an image with custom options.
     *
     * @param prompt  the description of the image to generate
     * @param options the image generation options
     * @return generated image data
     */
    @Nonnull
    GeneratedImage generate(@Nonnull String prompt, @Nonnull ImageGenerationOptions options);

    /**
     * Record representing a generated image.
     */
    record GeneratedImage(
            @Nonnull String url,
            @Nonnull String revisedPrompt,
            @Nonnull ImageSize size
    ) {}

    /**
     * Image size options.
     */
    enum ImageSize {
        SMALL("256x256"),
        MEDIUM("512x512"),
        LARGE("1024x1024"),
        WIDE("1792x1024"),
        TALL("1024x1792");

        private final String dimensions;

        ImageSize(String dimensions) {
            this.dimensions = dimensions;
        }

        public String getDimensions() {
            return dimensions;
        }
    }

    /**
     * Options for image generation.
     */
    record ImageGenerationOptions(
            @Nonnull ImageSize size,
            @Nonnull String quality,
            @Nonnull String style
    ) {
        public static ImageGenerationOptions defaults() {
            return new ImageGenerationOptions(ImageSize.LARGE, "standard", "natural");
        }

        public static ImageGenerationOptions highQuality() {
            return new ImageGenerationOptions(ImageSize.LARGE, "hd", "vivid");
        }

        public static ImageGenerationOptions forFlashcard() {
            return new ImageGenerationOptions(ImageSize.MEDIUM, "standard", "natural");
        }
    }
}