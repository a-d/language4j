package dev.languagelearning.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks topics that have been used in learning activities.
 * <p>
 * Used to avoid suggesting repetitive topics and to provide
 * personalized, diverse learning experiences.
 */
@Entity
@Table(name = "topic_history", indexes = {
        @Index(name = "idx_topic_history_user_activity", columnList = "user_id, activity_type"),
        @Index(name = "idx_topic_history_created", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class TopicHistory extends BaseEntity {

    /**
     * The user who used this topic.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The topic that was used.
     */
    @Column(name = "topic", nullable = false)
    private String topic;

    /**
     * The type of activity the topic was used for.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityCategory activityCategory;

    /**
     * How many times this topic has been used for this activity type.
     */
    @Column(name = "use_count", nullable = false)
    private int useCount = 1;

    /**
     * Factory method to create a new topic history entry.
     *
     * @param user     the user
     * @param topic    the topic
     * @param category the activity category
     * @return a new TopicHistory instance
     */
    public static TopicHistory of(User user, String topic, ActivityCategory category) {
        TopicHistory history = new TopicHistory();
        history.setUser(user);
        history.setTopic(topic);
        history.setActivityCategory(category);
        return history;
    }

    /**
     * Increments the usage count.
     */
    public void incrementUseCount() {
        this.useCount++;
    }

    /**
     * Categories of learning activities for topic organization.
     */
    public enum ActivityCategory {
        /**
         * Vocabulary practice and flashcards.
         */
        VOCABULARY,
        
        /**
         * Exercises (text completion, drag-drop, translation, etc.).
         */
        EXERCISE,
        
        /**
         * Lessons and tutorials.
         */
        LESSON,
        
        /**
         * Roleplay scenarios.
         */
        SCENARIO,
        
        /**
         * Listening and speaking practice.
         */
        AUDIO
    }
}