package dev.languagelearning.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.*;
import dev.languagelearning.config.LanguageConfig;
import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for content generation operations.
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Content", description = "AI-generated learning content")
public class ContentController {

    private final ContentGenerationService contentService;
    private final ObjectProvider<ImageService> imageServiceProvider;
    private final LanguageConfig languageConfig;
    private final ObjectMapper objectMapper;

    /**
     * Generates a new lesson.
     */
    @PostMapping("/lessons/generate")
    @Operation(
            summary = "Generate a lesson",
            description = "Generates a new lesson on the specified topic using AI. The lesson is tailored to the user's skill level and language pair. Response time may take 5-30 seconds depending on AI provider."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lesson generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> generateLesson(@RequestBody GenerateContentRequest request) {
        String content = contentService.generateLesson(request.topic());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "lesson"));
    }

    /**
     * Generates vocabulary for a topic.
     */
    @PostMapping("/vocabulary/generate")
    @Operation(
            summary = "Generate vocabulary",
            description = "Generates a vocabulary list for the specified topic. Includes translations, example sentences, and usage notes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Vocabulary generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> generateVocabulary(@RequestBody GenerateVocabularyRequest request) {
        String content = contentService.generateVocabulary(request.topic(), request.wordCount());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "vocabulary"));
    }

    /**
     * Generates flashcards for a topic.
     */
    @PostMapping("/flashcards/generate")
    @Operation(
            summary = "Generate flashcards",
            description = "Generates flashcard data for the specified topic. Each flashcard includes front (target language) and back (native language) content."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Flashcards generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> generateFlashcards(@RequestBody GenerateFlashcardsRequest request) {
        String content = contentService.generateFlashcards(request.topic(), request.cardCount());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "flashcards"));
    }

    /**
     * Generates a roleplay scenario.
     */
    @PostMapping("/scenarios/generate")
    @Operation(
            summary = "Generate roleplay scenario",
            description = "Generates a roleplay dialogue scenario for conversational practice. Includes character descriptions, dialogue lines, and cultural notes."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Scenario generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> generateScenario(@RequestBody GenerateScenarioRequest request) {
        String content = contentService.generateRoleplayScenario(request.scenario());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "scenario"));
    }

    /**
     * Generates a personalized learning plan.
     */
    @PostMapping("/learning-plan/generate")
    @Operation(
            summary = "Generate learning plan",
            description = "Generates a personalized learning plan based on user goals. Creates a structured roadmap with daily, weekly, and monthly milestones."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Learning plan generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> generateLearningPlan(@RequestBody GenerateLearningPlanRequest request) {
        String content = contentService.generateLearningPlan(
                request.dailyGoal(),
                request.weeklyGoal(),
                request.monthlyGoal()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "learning-plan"));
    }

    /**
     * Generates visual learning cards with AI-generated images.
     * <p>
     * This endpoint combines vocabulary generation and image generation to create
     * bilingual visual flashcards. Each card shows the native language word (front)
     * and target language word (back) along with an AI-generated illustration.
     */
    @PostMapping("/visual-cards/generate")
    @Operation(
            summary = "Generate visual learning cards",
            description = "Generates visual vocabulary cards from a topic. Uses LLM to derive " +
                    "appropriate vocabulary words, then generates AI images for each word. " +
                    "Cards display native language (front) and target language (back) with " +
                    "supporting images. Response time: 30-120 seconds depending on card count."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Visual cards generated successfully",
                    content = @Content(schema = @Schema(implementation = VisualCardsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Image service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Content generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<VisualCardsResponse> generateVisualCards(
            @Valid @RequestBody GenerateVisualCardsRequest request) {
        
        log.info("Generating {} visual cards for topic: {}", request.getCardCountOrDefault(), request.topic());
        
        // Get image service (optional dependency)
        ImageService imageService = imageServiceProvider.getIfAvailable();
        if (imageService == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Image generation service is not available. Please configure an image model."
            );
        }
        
        // Step 1: Generate vocabulary using LLM
        String vocabularyJson = contentService.generateVisualVocabulary(
                request.topic(), 
                request.getCardCountOrDefault()
        );
        
        // Step 2: Parse vocabulary JSON
        List<VocabularyItem> vocabularyItems = parseVisualVocabulary(vocabularyJson);
        
        // Step 3: Generate images for each word and build response
        List<VisualCardDto> cards = new ArrayList<>();
        int failedCount = 0;
        
        for (VocabularyItem item : vocabularyItems) {
            try {
                log.debug("Generating image for word: {}", item.targetWord());
                
                // Generate image using the image description from LLM
                var generatedImage = imageService.generate(
                        item.imageDescription(),
                        ImageService.ImageGenerationOptions.forFlashcard()
                );
                
                // Create visual card DTO
                VisualCardDto card = new VisualCardDto(
                        item.nativeWord(),
                        item.targetWord(),
                        generatedImage.url(),
                        item.exampleSentence(),
                        item.pronunciation()
                );
                cards.add(card);
                
            } catch (Exception e) {
                log.warn("Failed to generate image for word '{}': {}", item.targetWord(), e.getMessage());
                failedCount++;
                
                // Still add the card without an image
                VisualCardDto card = new VisualCardDto(
                        item.nativeWord(),
                        item.targetWord(),
                        null,  // No image
                        item.exampleSentence(),
                        item.pronunciation()
                );
                cards.add(card);
            }
        }
        
        log.info("Generated {} visual cards ({} with images, {} failed)",
                cards.size(), cards.size() - failedCount, failedCount);
        
        return ResponseEntity.ok(new VisualCardsResponse(
                request.topic(),
                languageConfig.getNativeCode(),
                languageConfig.getTargetCode(),
                cards,
                cards.size(),
                failedCount
        ));
    }

    /**
     * Parses the visual vocabulary JSON from LLM response.
     */
    private List<VocabularyItem> parseVisualVocabulary(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode vocabularyArray = root.path("vocabulary");
            
            if (!vocabularyArray.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid vocabulary response format"
                );
            }
            
            List<VocabularyItem> items = new ArrayList<>();
            for (JsonNode node : vocabularyArray) {
                items.add(new VocabularyItem(
                        node.path("nativeWord").asText(),
                        node.path("targetWord").asText(),
                        node.path("pronunciation").asText(null),
                        node.path("exampleSentence").asText(null),
                        node.path("imageDescription").asText("Simple educational illustration")
                ));
            }
            return items;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse vocabulary JSON: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse vocabulary response"
            );
        }
    }

    /**
     * Internal record for holding parsed vocabulary items.
     */
    private record VocabularyItem(
            String nativeWord,
            String targetWord,
            String pronunciation,
            String exampleSentence,
            String imageDescription
    ) {}
}
