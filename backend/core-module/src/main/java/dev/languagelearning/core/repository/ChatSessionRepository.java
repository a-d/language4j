package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing chat sessions.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Finds all sessions for a user, ordered by creation date descending.
     *
     * @param userId the user ID
     * @return list of sessions
     */
    List<ChatSession> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds the most recent active session for a user.
     *
     * @param userId the user ID
     * @return the active session if exists
     */
    Optional<ChatSession> findFirstByUser_IdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds a session by ID with messages eagerly loaded.
     *
     * @param sessionId the session ID
     * @return the session with messages
     */
    @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.messages WHERE s.id = :sessionId")
    Optional<ChatSession> findByIdWithMessages(@Param("sessionId") UUID sessionId);

    /**
     * Counts the number of sessions for a user.
     *
     * @param userId the user ID
     * @return the count
     */
    long countByUser_Id(UUID userId);

    /**
     * Deactivates all sessions for a user.
     *
     * @param userId the user ID
     */
    @Query("UPDATE ChatSession s SET s.active = false WHERE s.user.id = :userId")
    void deactivateAllForUser(@Param("userId") UUID userId);
}