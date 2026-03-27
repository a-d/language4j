package dev.languagelearning.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents a single message in a chat conversation.
 * <p>
 * Messages can contain plain text and/or embedded learning activities.
 * When an activity is completed, the embeddedActivityContent can be
 * replaced with a summary.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedded_activity_type", length = 30)
    private EmbeddedActivityType embeddedActivityType;

    @Column(name = "embedded_activity_content", columnDefinition = "TEXT")
    private String embeddedActivityContent;

    @Column(name = "activity_completed", nullable = false)
    private boolean activityCompleted = false;

    @Column(name = "activity_summary", columnDefinition = "TEXT")
    private String activitySummary;

    /**
     * Creates a user message with the given content.
     *
     * @param content the message content
     * @return the new message
     */
    public static ChatMessage userMessage(String content) {
        return new ChatMessage()
                .setRole(MessageRole.USER)
                .setContent(content);
    }

    /**
     * Creates an assistant message with the given content.
     *
     * @param content the message content
     * @return the new message
     */
    public static ChatMessage assistantMessage(String content) {
        return new ChatMessage()
                .setRole(MessageRole.ASSISTANT)
                .setContent(content);
    }

    /**
     * Creates an assistant message with an embedded activity.
     *
     * @param content         the message content
     * @param activityType    the type of embedded activity
     * @param activityContent the activity content (JSON)
     * @return the new message
     */
    public static ChatMessage assistantMessageWithActivity(
            String content,
            EmbeddedActivityType activityType,
            String activityContent) {
        return new ChatMessage()
                .setRole(MessageRole.ASSISTANT)
                .setContent(content)
                .setEmbeddedActivityType(activityType)
                .setEmbeddedActivityContent(activityContent);
    }

    /**
     * Creates a system message with the given content.
     *
     * @param content the message content
     * @return the new message
     */
    public static ChatMessage systemMessage(String content) {
        return new ChatMessage()
                .setRole(MessageRole.SYSTEM)
                .setContent(content);
    }

    /**
     * Checks if this message has an embedded activity.
     *
     * @return true if there is an embedded activity
     */
    public boolean hasEmbeddedActivity() {
        return embeddedActivityType != null && embeddedActivityContent != null;
    }

    /**
     * Marks the embedded activity as completed with a summary.
     *
     * @param summary the completion summary
     */
    public void completeActivity(String summary) {
        this.activityCompleted = true;
        this.activitySummary = summary;
    }
}