package dev.languagelearning.core.context;

import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Interface for accessing the current user context.
 * <p>
 * Implementations provide access to the user ID associated with
 * the current request/execution context.
 */
public interface UserContextHolder {

    /**
     * Gets the current user ID from the context.
     *
     * @return the user ID, or null if not set
     */
    @Nullable
    UUID getUserId();

    /**
     * Checks if a user ID has been set in the context.
     *
     * @return true if a user ID is present
     */
    boolean hasUserId();
}