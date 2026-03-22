package dev.languagelearning.api.dto;

/**
 * Response DTO for generated content.
 */
public record GeneratedContentResponse(
        String content,
        String type
) {}