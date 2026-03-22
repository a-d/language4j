package dev.languagelearning.core.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested entity cannot be found.
 */
public class EntityNotFoundException extends LanguageLearningException {

    public EntityNotFoundException(String entityType, UUID id) {
        super(String.format("%s not found with id: %s", entityType, id));
    }

    public EntityNotFoundException(String entityType, String identifier) {
        super(String.format("%s not found: %s", entityType, identifier));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Factory method for user not found.
     */
    public static EntityNotFoundException userNotFound(UUID id) {
        return new EntityNotFoundException("User", id);
    }

    /**
     * Factory method for learning goal not found.
     */
    public static EntityNotFoundException goalNotFound(UUID id) {
        return new EntityNotFoundException("Learning goal", id);
    }

    /**
     * Factory method for content not found.
     */
    public static EntityNotFoundException contentNotFound(String reference) {
        return new EntityNotFoundException("Content", reference);
    }
}