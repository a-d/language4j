package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.core.domain.ExerciseResult;
import dev.languagelearning.core.domain.ExerciseType;
import dev.languagelearning.learning.service.ExerciseResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for exercise operations.
 */
@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Interactive exercises and evaluation")
public class ExerciseController {

    private final ContentGenerationService contentService;
    private final ExerciseResultService exerciseResultService;

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

    /**
     * Generates listening comprehension exercises.
     */
    @PostMapping("/listening")
    @Operation(
            summary = "Generate listening exercises",
            description = "Generates listening comprehension exercises where users listen to audio and type what they hear. Each exercise includes text for TTS, translation, and hints."
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
    public ResponseEntity<GeneratedContentResponse> getListeningExercises(
            @RequestBody GenerateExerciseRequest request
    ) {
        String content = contentService.generateListeningExercises(
                request.topic(),
                request.questionCount()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "listening"));
    }

    /**
     * Generates speaking/pronunciation exercises.
     */
    @PostMapping("/speaking")
    @Operation(
            summary = "Generate speaking exercises",
            description = "Generates speaking and pronunciation exercises where users speak phrases and get feedback. Each exercise includes text to speak, pronunciation tips, and common mistakes to avoid."
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
    public ResponseEntity<GeneratedContentResponse> getSpeakingExercises(
            @RequestBody GenerateExerciseRequest request
    ) {
        String content = contentService.generateSpeakingExercises(
                request.topic(),
                request.questionCount()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "speaking"));
    }

    /**
     * Evaluates pronunciation by comparing expected text with transcription.
     */
    @PostMapping("/evaluate-pronunciation")
    @Operation(
            summary = "Evaluate pronunciation",
            description = "Evaluates a user's pronunciation by comparing the expected text with the transcribed speech. Provides accuracy score and specific feedback."
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
    public ResponseEntity<GeneratedContentResponse> evaluatePronunciation(
            @RequestBody EvaluatePronunciationRequest request
    ) {
        String content = contentService.evaluatePronunciation(
                request.expectedText(),
                request.transcription()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "pronunciation-evaluation"));
    }

    // ==================== Exercise Results ====================

    /**
     * Saves an exercise result.
     */
    @PostMapping("/results")
    @Operation(
            summary = "Save exercise result",
            description = "Saves the result of a completed exercise including score, time spent, and answers."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Result saved successfully",
                    content = @Content(schema = @Schema(implementation = ExerciseResultDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ExerciseResultDto> saveResult(
            @Valid @RequestBody SaveExerciseResultRequest request
    ) {
        ExerciseType exerciseType = ExerciseType.valueOf(request.exerciseType());

        ExerciseResult result = exerciseResultService.saveResult(
                exerciseType,
                request.exerciseReference(),
                request.score(),
                request.correctAnswers(),
                request.totalQuestions(),
                request.timeSpentSeconds(),
                request.userResponse(),
                request.correctResponse(),
                request.feedback()
        );

        return ResponseEntity.ok(ExerciseResultDto.from(result));
    }

    /**
     * Gets exercise history for the current user.
     */
    @GetMapping("/results")
    @Operation(
            summary = "Get exercise history",
            description = "Returns paginated exercise results for the current user, optionally filtered by type."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "History retrieved successfully"
            )
    })
    public ResponseEntity<Page<ExerciseResultDto>> getHistory(
            @Parameter(description = "Filter by exercise type")
            @RequestParam(required = false) String type,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ExerciseResult> results;

        if (type != null && !type.isBlank()) {
            ExerciseType exerciseType = ExerciseType.valueOf(type);
            results = exerciseResultService.getHistoryByType(exerciseType, pageable);
        } else {
            results = exerciseResultService.getHistory(pageable);
        }

        Page<ExerciseResultDto> dtos = results.map(ExerciseResultDto::from);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Gets recent exercise results.
     */
    @GetMapping("/results/recent")
    @Operation(
            summary = "Get recent exercise results",
            description = "Returns exercise results from the last N days."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recent results retrieved successfully"
            )
    })
    public ResponseEntity<List<ExerciseResultDto>> getRecentResults(
            @Parameter(description = "Number of days to look back")
            @RequestParam(defaultValue = "7") int days
    ) {
        List<ExerciseResult> results = exerciseResultService.getRecentResults(days);
        List<ExerciseResultDto> dtos = results.stream()
                .map(ExerciseResultDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Gets exercise statistics for the current user.
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "Get exercise statistics",
            description = "Returns summary statistics including total exercises, average score, time spent, and pass rate."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExerciseStatisticsDto.class))
            )
    })
    public ResponseEntity<ExerciseStatisticsDto> getStatistics() {
        ExerciseResultService.ExerciseStatistics stats = exerciseResultService.getStatistics();
        return ResponseEntity.ok(ExerciseStatisticsDto.from(stats));
    }
}
