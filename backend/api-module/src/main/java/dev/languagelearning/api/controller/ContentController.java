package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.content.service.ContentGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for content generation operations.
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "AI-generated learning content")
public class ContentController {

    private final ContentGenerationService contentService;

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
}