package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.TopicHistory;
import dev.languagelearning.core.domain.TopicHistory.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing topic history records.
 */
@Repository
public interface TopicHistoryRepository extends JpaRepository<TopicHistory, UUID> {

    /**
     * Finds an existing topic history entry for a user, topic, and category.
     *
     * @param userId   the user ID
     * @param topic    the topic (case-insensitive match)
     * @param category the activity category
     * @return the existing entry if found
     */
    @Query("SELECT th FROM TopicHistory th WHERE th.user.id = :userId " +
            "AND LOWER(th.topic) = LOWER(:topic) AND th.activityCategory = :category")
    Optional<TopicHistory> findByUserAndTopicAndCategory(
            @Param("userId") UUID userId,
            @Param("topic") String topic,
            @Param("category") ActivityCategory category
    );

    /**
     * Gets recent topics used by a user for a specific activity category.
     *
     * @param userId   the user ID
     * @param category the activity category
     * @param limit    maximum number of topics to return
     * @return list of recently used topic strings
     */
    @Query("SELECT th.topic FROM TopicHistory th WHERE th.user.id = :userId " +
            "AND th.activityCategory = :category ORDER BY th.updatedAt DESC LIMIT :limit")
    List<String> findRecentTopics(
            @Param("userId") UUID userId,
            @Param("category") ActivityCategory category,
            @Param("limit") int limit
    );

    /**
     * Gets all topics used by a user for a specific category since a given time.
     *
     * @param userId   the user ID
     * @param category the activity category
     * @param since    the start time
     * @return list of topics used since the given time
     */
    @Query("SELECT th.topic FROM TopicHistory th WHERE th.user.id = :userId " +
            "AND th.activityCategory = :category AND th.updatedAt >= :since")
    List<String> findTopicsUsedSince(
            @Param("userId") UUID userId,
            @Param("category") ActivityCategory category,
            @Param("since") Instant since
    );

    /**
     * Gets the most frequently used topics for a user and category.
     *
     * @param userId   the user ID
     * @param category the activity category
     * @param limit    maximum number of topics to return
     * @return list of most-used topics
     */
    @Query("SELECT th.topic FROM TopicHistory th WHERE th.user.id = :userId " +
            "AND th.activityCategory = :category ORDER BY th.useCount DESC LIMIT :limit")
    List<String> findMostUsedTopics(
            @Param("userId") UUID userId,
            @Param("category") ActivityCategory category,
            @Param("limit") int limit
    );

    /**
     * Gets all topic history for a user.
     *
     * @param userId the user ID
     * @return all topic history entries
     */
    List<TopicHistory> findByUser_IdOrderByUpdatedAtDesc(UUID userId);

    /**
     * Deletes all topic history for a user.
     *
     * @param userId the user ID
     */
    void deleteByUser_Id(UUID userId);
}