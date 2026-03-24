package dev.languagelearning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to evaluate pronunciation by comparing expected text with transcription.
 */
@Schema(description = "Request to evaluate pronunciation")
public record EvaluatePronunciationRequest(
        @Schema(
                description = "The text the user was supposed to say",
                example = "Bonjour, comment allez-vous?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Expected text is required")
        String expectedText,

        @Schema(
                description = "The transcribed text from the user's speech",
                example = "Bonjour, comment allez vous",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Transcription is required")
        String transcription
) {}