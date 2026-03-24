package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.image.service.ImageService;
import dev.languagelearning.image.service.ImageService.GeneratedImage;
import dev.languagelearning.image.service.ImageService.ImageGenerationOptions;
import dev.languagelearning.image.service.ImageService.ImageSize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * REST controller for image generation operations.
 * <p>
 * Provides endpoints for generating educational images for flashcards,
 * vocabulary learning, and other visual learning materials.
 * <p>
 * This controller requires an ImageModel bean to be configured
 * (requires OpenAI API key with DALL-E access). If image generation
 * is not configured, endpoints will return HTTP 503 (Service Unavailable).
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Images", description = "AI-generated images for learning content")
public class ImageController {

    private final ObjectProvider<ImageService> imageServiceProvider;
    private final LanguageConfig languageConfig;

    private ImageService getImageService() {
        ImageService service = imageServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Image generation service is not available. " +
                            "Please configure an image model (e.g., OpenAI DALL-E) to use this feature."
            );
        }
        return service;
    }

    /**
     * Generates an image from a text prompt.
     */
    @PostMapping("/generate")
    @Operation(
            summary = "Generate an image",
            description = "Generates an image from a text prompt using AI (DALL-E). " +
                    "Response time may take 10-30 seconds depending on size and quality."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Image generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedImageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Image generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedImageResponse> generateImage(@RequestBody GenerateImageRequest request) {
        log.debug("Generating image for prompt: {}", truncate(request.prompt()));

        ImageGenerationOptions options = buildOptions(request.size(), request.quality(), request.style());
        GeneratedImage result = getImageService().generate(request.prompt(), options);

        return ResponseEntity.ok(toResponse(result));
    }

    /**
     * Generates an image for a vocabulary flashcard.
     */
    @PostMapping("/flashcard")
    @Operation(
            summary = "Generate a flashcard image",
            description = "Generates an educational illustration for a vocabulary word. " +
                    "The image is optimized for flashcard display (512x512) with a clean, " +
                    "minimalist style suitable for language learning."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flashcard image generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedImageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Image generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedImageResponse> generateFlashcardImage(
            @RequestBody GenerateFlashcardImageRequest request) {
        log.debug("Generating flashcard image for word: {}", request.word());

        String targetLang = languageConfig.getTargetCode();
        GeneratedImage result = getImageService().generateFlashcardImage(
                request.word(),
                targetLang,
                request.context()
        );

        return ResponseEntity.ok(toResponse(result));
    }

    /**
     * Generates images for multiple vocabulary words (batch).
     */
    @PostMapping("/flashcard/batch")
    @Operation(
            summary = "Generate multiple flashcard images",
            description = "Generates educational illustrations for multiple vocabulary words. " +
                    "Returns an array of generated images. Limited to 5 words per request."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flashcard images generated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (too many words)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Image generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedImageResponse[]> generateFlashcardImagesBatch(
            @RequestBody GenerateFlashcardImageRequest[] requests) {
        if (requests.length > 5) {
            throw new IllegalArgumentException("Maximum 5 images per batch request");
        }

        log.debug("Generating batch of {} flashcard images", requests.length);

        String targetLang = languageConfig.getTargetCode();
        GeneratedImageResponse[] responses = new GeneratedImageResponse[requests.length];

        ImageService imageService = getImageService();
        for (int i = 0; i < requests.length; i++) {
            GenerateFlashcardImageRequest req = requests[i];
            GeneratedImage result = imageService.generateFlashcardImage(
                    req.word(),
                    targetLang,
                    req.context()
            );
            responses[i] = toResponse(result);
        }

        return ResponseEntity.ok(responses);
    }

    private ImageGenerationOptions buildOptions(String size, String quality, String style) {
        ImageSize imageSize = parseSize(size).orElse(ImageSize.LARGE);
        String imageQuality = quality != null ? quality : "standard";
        String imageStyle = style != null ? style : "natural";

        return new ImageGenerationOptions(imageSize, imageQuality, imageStyle);
    }

    private Optional<ImageSize> parseSize(String size) {
        if (size == null || size.isBlank()) {
            return Optional.empty();
        }

        return switch (size.toLowerCase()) {
            case "small", "256x256" -> Optional.of(ImageSize.SMALL);
            case "medium", "512x512" -> Optional.of(ImageSize.MEDIUM);
            case "large", "1024x1024" -> Optional.of(ImageSize.LARGE);
            case "wide", "1792x1024" -> Optional.of(ImageSize.WIDE);
            case "tall", "1024x1792" -> Optional.of(ImageSize.TALL);
            default -> Optional.empty();
        };
    }

    private GeneratedImageResponse toResponse(GeneratedImage image) {
        return new GeneratedImageResponse(
                image.url(),
                image.revisedPrompt(),
                image.size().getDimensions()
        );
    }

    private String truncate(String text) {
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}