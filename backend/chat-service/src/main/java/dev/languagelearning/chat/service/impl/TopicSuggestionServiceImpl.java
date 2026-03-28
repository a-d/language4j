package dev.languagelearning.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.chat.model.ChatContext;
import dev.languagelearning.chat.prompts.ChatPrompts;
import dev.languagelearning.chat.service.ChatContextService;
import dev.languagelearning.chat.service.TopicSuggestionService;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.TopicHistory;
import dev.languagelearning.core.domain.TopicHistory.ActivityCategory;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.repository.TopicHistoryRepository;
import dev.languagelearning.learning.service.GoalService;
import dev.languagelearning.learning.service.UserService;
import dev.languagelearning.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of topic suggestion service with LLM-powered personalization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicSuggestionServiceImpl implements TopicSuggestionService {

    private static final int MAX_RECENT_TOPICS = 20;
    private static final int DEFAULT_SUGGESTION_COUNT = 5;

    private final TopicHistoryRepository topicHistoryRepository;
    private final UserService userService;
    private final GoalService goalService;
    private final ChatContextService contextService;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TopicSuggestion> generateSuggestions(ActivityCategory category, int count) {
        count = Math.min(Math.max(count, 1), 10);
        
        User user = userService.getCurrentUser();
        ChatContext context = contextService.buildContext(user);
        
        // Get recently used topics to avoid
        List<String> recentTopics = getRecentTopics(category, MAX_RECENT_TOPICS);
        String avoidTopics = recentTopics.isEmpty() ? "None" : String.join(", ", recentTopics);
        
        // Get daily goals for context
        String goalsContext = formatGoalsForPrompt(context);
        
        // Build the prompt using user's language settings
        String activityName = formatActivityCategory(category);
        String prompt = String.format(
                ChatPrompts.TOPIC_SUGGESTIONS_PROMPT,
                count,
                activityName,
                user.getTargetLanguageName(),
                user.getSkillLevel().name(),
                user.getTargetLanguage(),
                user.getNativeLanguage(),
                avoidTopics,
                goalsContext,
                user.getNativeLanguage(),
                count,
                user.getNativeLanguage(),
                user.getSkillLevel().name(),
                avoidTopics
        );
        
        log.debug("Generating {} topic suggestions for {} activity", count, category);
        
        try {
            String response = llmService.generate(
                    "You are a language learning content curator. Respond only with valid JSON.",
                    prompt
            );
            
            return parseSuggestions(response, context);
            
        } catch (Exception e) {
            log.error("Failed to generate topic suggestions", e);
            return generateFallbackSuggestions(category, user.getSkillLevel().name(), count);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String selectRandomTopic(ActivityCategory category) {
        User user = userService.getCurrentUser();
        ChatContext context = contextService.buildContext(user);
        
        // Get recently used topics to avoid
        List<String> recentTopics = getRecentTopics(category, MAX_RECENT_TOPICS);
        String avoidTopics = recentTopics.isEmpty() ? "None" : String.join(", ", recentTopics);
        
        // Get daily goals for context
        String goalsContext = formatGoalsForPrompt(context);
        
        // Build the prompt using user's language settings
        String activityName = formatActivityCategory(category);
        String prompt = String.format(
                ChatPrompts.RANDOM_TOPIC_PROMPT,
                activityName,
                user.getSkillLevel().name(),
                user.getTargetLanguage(),
                user.getNativeLanguage(),
                avoidTopics,
                goalsContext,
                user.getSkillLevel().name(),
                user.getNativeLanguage()
        );
        
        log.debug("Selecting random topic for {} activity", category);
        
        try {
            String response = llmService.generate(
                    "You are a language learning content curator. Respond with only the topic name.",
                    prompt
            );
            
            // Clean up the response
            String topic = response.trim()
                    .replaceAll("^[\"']|[\"']$", "") // Remove quotes
                    .replaceAll("\\.$", ""); // Remove trailing period
            
            return topic.isEmpty() ? getDefaultTopic(category, user.getSkillLevel().name()) : topic;
            
        } catch (Exception e) {
            log.error("Failed to select random topic", e);
            return getDefaultTopic(category, user.getSkillLevel().name());
        }
    }

    @Override
    @Transactional
    public void recordTopicUsage(String topic, ActivityCategory category) {
        User user = userService.getCurrentUser();
        
        // Check if topic already exists
        var existing = topicHistoryRepository.findByUserAndTopicAndCategory(
                user.getId(), topic, category
        );
        
        if (existing.isPresent()) {
            // Increment usage count
            TopicHistory history = existing.get();
            history.incrementUseCount();
            topicHistoryRepository.save(history);
            log.debug("Updated topic usage: {} for {} (count: {})", topic, category, history.getUseCount());
        } else {
            // Create new entry
            TopicHistory history = TopicHistory.of(user, topic, category);
            topicHistoryRepository.save(history);
            log.debug("Recorded new topic usage: {} for {}", topic, category);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRecentTopics(ActivityCategory category, int limit) {
        User user = userService.getCurrentUser();
        return topicHistoryRepository.findRecentTopics(user.getId(), category, limit);
    }

    private List<TopicSuggestion> parseSuggestions(String response, ChatContext context) {
        List<TopicSuggestion> suggestions = new ArrayList<>();
        
        try {
            // Extract JSON from response (handle markdown code blocks)
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode suggestionsNode = root.path("suggestions");
            
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    String topic = node.path("topic").asText("");
                    String emoji = node.path("emoji").asText("📚");
                    String description = node.path("description").asText(null);
                    
                    if (!topic.isEmpty()) {
                        boolean alignsWithGoals = checkGoalAlignment(topic, context);
                        suggestions.add(new TopicSuggestion(topic, description, emoji, alignsWithGoals));
                    }
                }
            }
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse topic suggestions JSON: {}", e.getMessage());
        }
        
        return suggestions;
    }

    private String extractJson(String response) {
        // Handle markdown code blocks
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }

    private boolean checkGoalAlignment(String topic, ChatContext context) {
        if (context.getGoals() == null || context.getGoals().getDailyGoals() == null) {
            return false;
        }
        
        String topicLower = topic.toLowerCase();
        for (var goal : context.getGoals().getDailyGoals()) {
            if (!goal.isCompleted()) {
                String goalTitle = goal.getTitle().toLowerCase();
                // Simple keyword matching
                if (goalTitle.contains("vocabulary") && 
                        (topicLower.contains("word") || topicLower.contains("vocab"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String formatGoalsForPrompt(ChatContext context) {
        if (context.getGoals() == null || context.getGoals().getDailyGoals() == null) {
            return "No daily goals set";
        }
        
        return context.getGoals().getDailyGoals().stream()
                .filter(g -> !g.isCompleted())
                .map(g -> String.format("- %s (%d/%d %s)", 
                        g.getTitle(), g.getCurrentValue(), g.getTargetValue(), g.getUnit()))
                .collect(Collectors.joining("\n"));
    }

    private String formatActivityCategory(ActivityCategory category) {
        return switch (category) {
            case VOCABULARY -> "vocabulary learning";
            case EXERCISE -> "grammar and practice exercises";
            case LESSON -> "structured lesson";
            case SCENARIO -> "roleplay scenario";
            case AUDIO -> "listening and speaking practice";
        };
    }

    private List<TopicSuggestion> generateFallbackSuggestions(
            ActivityCategory category, String level, int count) {
        
        // Fallback topics based on level
        List<TopicSuggestion> fallbacks = switch (level) {
            case "A1" -> List.of(
                    TopicSuggestion.withEmoji("Greetings", "👋"),
                    TopicSuggestion.withEmoji("Numbers 1-20", "🔢"),
                    TopicSuggestion.withEmoji("Colors", "🎨"),
                    TopicSuggestion.withEmoji("Family members", "👨‍👩‍👧‍👦"),
                    TopicSuggestion.withEmoji("Basic food", "🍎"),
                    TopicSuggestion.withEmoji("Animals", "🐕"),
                    TopicSuggestion.withEmoji("Days of the week", "📅")
            );
            case "A2" -> List.of(
                    TopicSuggestion.withEmoji("Daily routine", "⏰"),
                    TopicSuggestion.withEmoji("Weather", "🌤️"),
                    TopicSuggestion.withEmoji("Shopping", "🛒"),
                    TopicSuggestion.withEmoji("Rooms in a house", "🏠"),
                    TopicSuggestion.withEmoji("Clothing", "👕"),
                    TopicSuggestion.withEmoji("Transportation", "🚌"),
                    TopicSuggestion.withEmoji("Time expressions", "🕐")
            );
            case "B1" -> List.of(
                    TopicSuggestion.withEmoji("Travel and vacations", "✈️"),
                    TopicSuggestion.withEmoji("Work and professions", "💼"),
                    TopicSuggestion.withEmoji("Hobbies and interests", "🎸"),
                    TopicSuggestion.withEmoji("Health and fitness", "💪"),
                    TopicSuggestion.withEmoji("Opinions and feelings", "💭"),
                    TopicSuggestion.withEmoji("Making plans", "📋"),
                    TopicSuggestion.withEmoji("Education", "📚")
            );
            default -> List.of(
                    TopicSuggestion.withEmoji("Current events", "📰"),
                    TopicSuggestion.withEmoji("Culture and traditions", "🎭"),
                    TopicSuggestion.withEmoji("Technology", "💻"),
                    TopicSuggestion.withEmoji("Environment", "🌍"),
                    TopicSuggestion.withEmoji("Business and economics", "📈"),
                    TopicSuggestion.withEmoji("Art and literature", "🖼️"),
                    TopicSuggestion.withEmoji("Science", "🔬")
            );
        };
        
        return fallbacks.subList(0, Math.min(count, fallbacks.size()));
    }

    private String getDefaultTopic(ActivityCategory category, String level) {
        return switch (level) {
            case "A1" -> switch (category) {
                case VOCABULARY -> "basic greetings";
                case EXERCISE -> "simple sentences";
                case LESSON -> "introducing yourself";
                case SCENARIO -> "meeting someone new";
                case AUDIO -> "numbers and counting";
            };
            case "A2" -> switch (category) {
                case VOCABULARY -> "daily activities";
                case EXERCISE -> "past tense practice";
                case LESSON -> "describing your day";
                case SCENARIO -> "shopping at a store";
                case AUDIO -> "weather descriptions";
            };
            default -> switch (category) {
                case VOCABULARY -> "everyday conversation";
                case EXERCISE -> "grammar practice";
                case LESSON -> "communication skills";
                case SCENARIO -> "social situations";
                case AUDIO -> "listening comprehension";
            };
        };
    }
}