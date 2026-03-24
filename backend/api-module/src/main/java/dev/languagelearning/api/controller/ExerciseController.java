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
 * REST controller for exercise operations.
 */
@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Interactive exercises and evaluation")
public class ExerciseController {

    private final ContentGenerationService contentService;

    /**
     * Generates text completion (fill-in-the-blank) exercises.
     */
    @PostMapping("/text-completion")
    @Operation(
            summary = "Generate text completion exercises",
            description = "Generates fill-in-the-blank exercises for the specified topic. Each exercise includes a sentence with a blank and the expected answer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Exercises generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Exercise generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> getTextCompletionExercises(
            @RequestBody GenerateExerciseRequest request
    ) {
        String content = contentService.generateTextCompletionExercises(
                request.topic(),
                request.questionCount()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "text-completion"));
    }

    /**
     * Generates drag-and-drop (word order) exercises.
     */
    @PostMapping("/drag-drop")
    @Operation(
            summary = "Generate drag-and-drop exercises",
            description = "Generates word-ordering exercises where users arrange words to form sentences. Includes shuffled words and the correct sentence order."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Exercises generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Exercise generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> getDragDropExercises(
            @RequestBody GenerateExerciseRequest request
    ) {
        String content = contentService.generateDragDropExercises(
                request.topic(),
                request.questionCount()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "drag-drop"));
    }

    /**
     * Generates translation exercises.
     */
    @PostMapping("/translation")
    @Operation(
            summary = "Generate translation exercises",
            description = "Generates translation exercises from native language to target language. Includes the source sentence, expected translation, and key vocabulary."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Exercises generated successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Exercise generation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> getTranslationExercises(
            @RequestBody GenerateExerciseRequest request
    ) {
        String content = contentService.generateTranslationExercises(
                request.topic(),
                request.questionCount()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "translation"));
    }

    /**
     * Evaluates a user's exercise response.
     */
    @PostMapping("/evaluate")
    @Operation(
            summary = "Evaluate user response",
            description = "Evaluates a user's answer to an exercise using AI. Provides feedback on correctness, grammar, and suggestions for improvement."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Evaluation completed successfully",
                    content = @Content(schema = @Schema(implementation = GeneratedContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Evaluation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GeneratedContentResponse> evaluateResponse(
            @RequestBody EvaluateResponseRequest request
    ) {
        String content = contentService.evaluateResponse(
                request.exercise(),
                request.userResponse(),
                request.expectedAnswer()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "evaluation"));
    }
}