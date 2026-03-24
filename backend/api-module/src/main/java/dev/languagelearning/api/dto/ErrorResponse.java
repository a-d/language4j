package dev.languagelearning.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response format for API errors.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response format")
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    private final Instant timestamp;

    /**
     * HTTP status code.
     */
    @Schema(description = "HTTP status code", example = "400")
    private final int status;

    /**
     * Error type/category.
     */
    @Schema(description = "Error type/category", example = "Validation Error")
    private final String error;

    /**
     * Human-readable error message.
     */
    @Schema(description = "Human-readable error message", example = "Invalid language code")
    private final String message;

    /**
     * Request path that caused the error.
     */
    @Schema(description = "Request path that caused the error", example = "/api/v1/users/me")
    private final String path;

    /**
     * Additional error details (e.g., field validation errors).
     */
    @Schema(description = "Additional error details (e.g., field validation errors)", nullable = true)
    private final Map<String, String> details;
}