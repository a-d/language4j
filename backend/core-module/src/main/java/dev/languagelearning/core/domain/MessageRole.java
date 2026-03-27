package dev.languagelearning.core.domain;

/**
 * Represents the role of a message sender in a chat conversation.
 */
public enum MessageRole {
    /**
     * Message from the user (learner).
     */
    USER,

    /**
     * Message from the AI assistant (tutor).
     */
    ASSISTANT,

    /**
     * System message providing context or instructions.
     */
    SYSTEM
}