package dev.languagelearning.chat.service;

import dev.languagelearning.core.domain.TopicHistory.ActivityCategory;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Service for generating and managing topic suggestions.
 * <p>
 * Provides LLM-powered topic suggestions that are personalized based on:
 * <ul>
 *   <li>User's skill level (CEFR)</li>
 *   <li>Target language</li>
 *   <li>Previously used topics (to avoid repetition)</li>
 *   <li>User's learning goals</li>
 * </ul>
 */
public interface TopicSuggestionService {

    /**
     * Generates topic suggestions for an activity category.
     *
     * @param category the activity category
     * @param count    number of suggestions to generate (max 10)
     * @return list of topic suggestions
     */
    @Nonnull
    List<TopicSuggestion> generateSuggestions(@Nonnull ActivityCategory category, int count);

    /**
     * Selects a random appropriate topic for an activity category.
     * <p>
     * Takes into account user's daily goals, skill level, and recent topics
     * to pick a suitable topic automatically.
     *
     * @param category the activity category
     * @return a randomly selected appropriate topic
     */
    @Nonnull
    String selectRandomTopic(@Nonnull ActivityCategory category);

    /**
     * Records that a topic was used for an activity.
     *
     * @param topic    the topic that was used
     * @param category the activity category
     */
    void recordTopicUsage(@Nonnull String topic, @Nonnull ActivityCategory category);

    /**
     * Gets recent topics used by the current user.
     *
     * @param category the activity category
     * @param limit    maximum number of topics to return
     * @return list of recently used topics
     */
    @Nonnull
    List<String> getRecentTopics(@Nonnull ActivityCategory category, int limit);

    /**
     * Represents a topic suggestion with metadata.
     */
    record TopicSuggestion(
            /**
             * The topic name/title.
             */
            String topic,
            
            /**
             * Brief description of what the topic covers.
             */
            String description,
            
            /**
             * Emoji icon for visual appeal.
             */
            String emoji,
            
            /**
             * Whether this topic aligns with user's daily goals.
             */
            boolean alignsWithGoals
    ) {
        /**
         * Creates a simple topic suggestion without metadata.
         */
        public static TopicSuggestion simple(String topic) {
            return new TopicSuggestion(topic, null, null, false);
        }
        
        /**
         * Creates a topic suggestion with an emoji.
         */
        public static TopicSuggestion withEmoji(String topic, String emoji) {
            return new TopicSuggestion(topic, null, emoji, false);
        }
    }
}