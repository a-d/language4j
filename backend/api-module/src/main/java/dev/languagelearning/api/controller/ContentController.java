package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.*;
import dev.languagelearning.content.service.ContentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for content generation operations.
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentGenerationService contentService;

    /**
     * Generates a new lesson.
     */
    @PostMapping("/lessons/generate")
    public ResponseEntity<GeneratedContentResponse> generateLesson(@RequestBody GenerateContentRequest request) {
        String content = contentService.generateLesson(request.topic());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "lesson"));
    }

    /**
     * Generates vocabulary for a topic.
     */
    @PostMapping("/vocabulary/generate")
    public ResponseEntity<GeneratedContentResponse> generateVocabulary(@RequestBody GenerateVocabularyRequest request) {
        String content = contentService.generateVocabulary(request.topic(), request.wordCount());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "vocabulary"));
    }

    /**
     * Generates flashcards for a topic.
     */
    @PostMapping("/flashcards/generate")
    public ResponseEntity<GeneratedContentResponse> generateFlashcards(@RequestBody GenerateFlashcardsRequest request) {
        String content = contentService.generateFlashcards(request.topic(), request.cardCount());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "flashcards"));
    }

    /**
     * Generates a roleplay scenario.
     */
    @PostMapping("/scenarios/generate")
    public ResponseEntity<GeneratedContentResponse> generateScenario(@RequestBody GenerateScenarioRequest request) {
        String content = contentService.generateRoleplayScenario(request.scenario());
        return ResponseEntity.ok(new GeneratedContentResponse(content, "scenario"));
    }

    /**
     * Generates a personalized learning plan.
     */
    @PostMapping("/learning-plan/generate")
    public ResponseEntity<GeneratedContentResponse> generateLearningPlan(@RequestBody GenerateLearningPlanRequest request) {
        String content = contentService.generateLearningPlan(
                request.dailyGoal(),
                request.weeklyGoal(),
                request.monthlyGoal()
        );
        return ResponseEntity.ok(new GeneratedContentResponse(content, "learning-plan"));
    }
}