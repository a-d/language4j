package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.ChatMessage;
import dev.languagelearning.core.domain.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing chat messages.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Finds all messages for a session, ordered by creation date.
     *
     * @param sessionId the session ID
     * @return list of messages
     */
    List<ChatMessage> findBySession_IdOrderByCreatedAtAsc(UUID sessionId);

    /**
     * Finds recent messages for a session (limited).
     *
     * @param sessionId the session ID
     * @param pageable  pagination parameters
     * @return list of recent messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("sessionId") UUID sessionId, Pageable pageable);

    /**
     * Finds the last N messages for a session (for context building).
     *
     * @param sessionId the session ID
     * @param limit     maximum number of messages
     * @return list of messages, oldest first
     */
    @Query(value = """
            SELECT * FROM (
                SELECT * FROM chat_messages 
                WHERE session_id = :sessionId 
                ORDER BY created_at DESC 
                LIMIT :limit
            ) AS recent ORDER BY created_at ASC
            """, nativeQuery = true)
    List<ChatMessage> findLastNMessages(@Param("sessionId") UUID sessionId, @Param("limit") int limit);

    /**
     * Finds messages by role in a session.
     *
     * @param sessionId the session ID
     * @param role      the message role
     * @return list of messages
     */
    List<ChatMessage> findBySession_IdAndRole(UUID sessionId, MessageRole role);

    /**
     * Finds messages with incomplete embedded activities in a session.
     *
     * @param sessionId the session ID
     * @return list of messages with pending activities
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId " +
            "AND m.embeddedActivityType IS NOT NULL AND m.activityCompleted = false")
    List<ChatMessage> findPendingActivities(@Param("sessionId") UUID sessionId);

    /**
     * Finds a message with an incomplete activity by session.
     *
     * @param sessionId the session ID
     * @return the message if found
     */
    Optional<ChatMessage> findFirstBySession_IdAndEmbeddedActivityTypeIsNotNullAndActivityCompletedFalseOrderByCreatedAtDesc(UUID sessionId);

    /**
     * Counts messages in a session.
     *
     * @param sessionId the session ID
     * @return the count
     */
    long countBySession_Id(UUID sessionId);

    /**
     * Marks an activity as completed.
     *
     * @param messageId the message ID
     * @param summary   the completion summary
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.activityCompleted = true, m.activitySummary = :summary WHERE m.id = :messageId")
    void completeActivity(@Param("messageId") UUID messageId, @Param("summary") String summary);

    /**
     * Deletes all messages for a session.
     *
     * @param sessionId the session ID
     */
    void deleteBySession_Id(UUID sessionId);
}