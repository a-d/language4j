package dev.languagelearning.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a chat conversation session between user and AI tutor.
 * <p>
 * A session maintains the conversation history and context for the
 * learning moderation chat interface.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class ChatSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Creates a new chat session for the given user.
     *
     * @param user the user who owns this session
     * @return the new chat session
     */
    public static ChatSession forUser(User user) {
        return new ChatSession().setUser(user);
    }

    /**
     * Adds a message to this session.
     *
     * @param message the message to add
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
        messageCount++;
    }

    /**
     * Gets the user ID for this session.
     *
     * @return the user ID
     */
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}