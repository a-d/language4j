package dev.languagelearning.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    private final Instant timestamp;

    /**
     * HTTP status code.
     */
    private final int status;

    /**
     * Error type/category.
     */
    private final String error;

    /**
     * Human-readable error message.
     */
    private final String message;

    /**
     * Request path that caused the error.
     */
    private final String path;

    /**
     * Additional error details (e.g., field validation errors).
     */
    private final Map<String, String> details;
}