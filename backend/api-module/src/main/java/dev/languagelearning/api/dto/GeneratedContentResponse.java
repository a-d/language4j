package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for generated content.
 */
@Schema(description = "Response containing generated content")
public record GeneratedContentResponse(
        @Schema(
                description = "Generated content (format depends on type: Markdown for lessons/vocabulary/scenarios, JSON for exercises/flashcards)",
                example = "# Lesson Title\n\n## Introduction\n..."
        )
        String content,

        @Schema(
                description = "Content type identifier",
                example = "lesson",
                allowableValues = {"lesson", "vocabulary", "flashcards", "scenario", "learning-plan", "text-completion", "drag-drop", "translation", "evaluation"}
        )
        String type
) {}