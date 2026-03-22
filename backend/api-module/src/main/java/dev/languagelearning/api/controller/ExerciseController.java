package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.content.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for exercise operations.
 */
@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ContentGenerationService contentService;

    /**
     * Generates text completion (fill-in-the-blank) exercises.
     */
    @PostMapping("/text-completion")
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