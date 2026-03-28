package dev.languagelearning.api.context;

import dev.languagelearning.core.context.UserContextHolder;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Request-scoped holder for the current user context.
 * <p>
 * The user ID is extracted from the X-User-Id header by {@link UserContextFilter}
 * and made available to services throughout the request lifecycle.
 * <p>
 * Implements {@link UserContextHolder} to provide the user ID to services
 * in other modules (like learning-service) that don't depend on api-module.
 */
@Component
@RequestScope
public class UserContext implements UserContextHolder {

    private UUID userId;

    /**
     * Gets the current user ID from the request context.
     *
     * @return the user ID, or null if not set
     */
    @Override
    @Nullable
    public UUID getUserId() {
        return userId;
    }

    /**
     * Sets the current user ID for this request.
     *
     * @param userId the user ID
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /**
     * Checks if a user ID has been set for this request.
     *
     * @return true if a user ID is present
     */
    @Override
    public boolean hasUserId() {
        return userId != null;
    }
}
