package dev.languagelearning.image.service.impl;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.image.exception.ImageGenerationException;
import dev.languagelearning.image.service.ImageService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link ImageService} using Spring AI's ImageModel.
 * <p>
 * Supports image generation via OpenAI DALL-E and other providers.
 * <p>
 * This class is registered as a bean by {@link dev.languagelearning.image.config.ImageServiceAutoConfiguration}
 * when an {@link ImageModel} is available.
 */
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageModel imageModel;
    private final AiProviderConfig aiProviderConfig;

    @Override
    @Nonnull
    public GeneratedImage generate(@Nonnull String prompt) {
        return generate(prompt, ImageGenerationOptions.defaults());
    }

    @Override
    @Nonnull
    public CompletableFuture<GeneratedImage> generateAsync(@Nonnull String prompt) {
        return CompletableFuture.supplyAsync(() -> generate(prompt));
    }

    @Override
    @Nonnull
    public GeneratedImage generateFlashcardImage(@Nonnull String word, @Nonnull String targetLang, String context) {
        String flashcardPrompt = buildFlashcardPrompt(word, targetLang, context);
        return generate(flashcardPrompt, ImageGenerationOptions.forFlashcard());
    }

    @Override
    @Nonnull
    public GeneratedImage generate(@Nonnull String prompt, @Nonnull ImageGenerationOptions options) {
        log.debug("Generating image with prompt: {} and options: {}", prompt, options);

        try {
            ImagePrompt imagePrompt = createImagePrompt(prompt, options);
            ImageResponse response = imageModel.call(imagePrompt);

            if (response == null || response.getResult() == null) {
                throw new ImageGenerationException("Empty response from image model");
            }

            var result = response.getResult();
            var output = result.getOutput();

            String url = output.getUrl();
            if (url == null || url.isBlank()) {
                // If URL is not available, try to get base64
                String b64 = output.getB64Json();
                if (b64 != null && !b64.isBlank()) {
                    url = "data:image/png;base64," + b64;
                } else {
                    throw new ImageGenerationException("No image URL or base64 data in response");
                }
            }

            // Use the original prompt as the revised prompt is not available in Spring AI 1.0.0-M5
            String revisedPrompt = prompt;

            log.info("Successfully generated image for prompt: {}", truncatePrompt(prompt));
            return new GeneratedImage(url, revisedPrompt, options.size());

        } catch (ImageGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate image: {}", e.getMessage(), e);
            throw new ImageGenerationException("Failed to generate image: " + e.getMessage(), e);
        }
    }

    private ImagePrompt createImagePrompt(String prompt, ImageGenerationOptions options) {
        var imageConfig = aiProviderConfig.getImage();

        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .withModel(imageConfig != null ? imageConfig.getModel() : "dall-e-3")
                .withQuality(options.quality())
                .withStyle(options.style())
                .withWidth(parseWidth(options.size()))
                .withHeight(parseHeight(options.size()))
                .build();

        return new ImagePrompt(prompt, imageOptions);
    }

    private String buildFlashcardPrompt(String word, String targetLang, String context) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Create a simple, clear educational illustration for the ");
        promptBuilder.append(getLanguageName(targetLang));
        promptBuilder.append(" word '").append(word).append("'. ");
        promptBuilder.append("The image should be suitable for a language learning flashcard. ");
        promptBuilder.append("Style: clean, minimalist, colorful, no text in the image. ");

        if (context != null && !context.isBlank()) {
            promptBuilder.append("Context: ").append(context);
        }

        return promptBuilder.toString();
    }

    private String getLanguageName(String langCode) {
        return switch (langCode.toLowerCase()) {
            case "en" -> "English";
            case "de" -> "German";
            case "fr" -> "French";
            case "es" -> "Spanish";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "nl" -> "Dutch";
            case "ru" -> "Russian";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            default -> langCode;
        };
    }

    private int parseWidth(ImageSize size) {
        String dimensions = size.getDimensions();
        return Integer.parseInt(dimensions.split("x")[0]);
    }

    private int parseHeight(ImageSize size) {
        String dimensions = size.getDimensions();
        return Integer.parseInt(dimensions.split("x")[1]);
    }

    private String truncatePrompt(String prompt) {
        return prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
    }
}