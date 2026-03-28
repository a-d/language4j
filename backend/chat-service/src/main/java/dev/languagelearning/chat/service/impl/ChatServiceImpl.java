package dev.languagelearning.chat.service.impl;

import dev.languagelearning.chat.model.ChatContext;
import dev.languagelearning.chat.prompts.ChatPrompts;
import dev.languagelearning.chat.service.ChatContextService;
import dev.languagelearning.chat.service.ChatService;
import dev.languagelearning.chat.service.ChatService.ChatResponse;
import dev.languagelearning.chat.service.ChatService.ChatResponseChunk;
import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.core.domain.ChatMessage;
import dev.languagelearning.core.domain.ChatSession;
import dev.languagelearning.core.domain.EmbeddedActivityType;
import dev.languagelearning.core.domain.ExerciseGenerationType;
import dev.languagelearning.core.domain.MessageRole;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.core.repository.ChatMessageRepository;
import dev.languagelearning.core.repository.ChatSessionRepository;
import dev.languagelearning.learning.service.UserService;
import dev.languagelearning.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the chat service with streaming support and activity generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Pattern ACTIVITY_PATTERN = Pattern.compile(
            "\\[ACTIVITY:(\\w+):([^\\]]+)\\]",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_CONTEXT_MESSAGES = 10;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;
    private final ChatContextService contextService;
    private final LlmService llmService;
    private final ContentGenerationService contentGenerationService;
    
    /**
     * Suggestion translations by language code.
     * Key structure: language code -> suggestion key -> translated text
     */
    private static final Map<String, Map<String, String>> SUGGESTION_TRANSLATIONS = Map.of(
            "en", Map.of(
                    "PRACTICE_VOCAB", "Practice vocabulary",
                    "DO_EXERCISES", "Do some exercises",
                    "START_LESSON", "Start a lesson",
                    "REVIEW", "Review what I learned"
            ),
            "de", Map.of(
                    "PRACTICE_VOCAB", "Vokabeln üben",
                    "DO_EXERCISES", "Übungen machen",
                    "START_LESSON", "Lektion starten",
                    "REVIEW", "Gelerntes wiederholen"
            ),
            "fr", Map.of(
                    "PRACTICE_VOCAB", "Pratiquer le vocabulaire",
                    "DO_EXERCISES", "Faire des exercices",
                    "START_LESSON", "Commencer une leçon",
                    "REVIEW", "Réviser ce que j'ai appris"
            ),
            "es", Map.of(
                    "PRACTICE_VOCAB", "Practicar vocabulario",
                    "DO_EXERCISES", "Hacer ejercicios",
                    "START_LESSON", "Comenzar una lección",
                    "REVIEW", "Revisar lo que aprendí"
            )
    );

    @Override
    @Transactional
    public ChatSession getOrCreateSession() {
        User user = userService.getCurrentUser();
        
        return sessionRepository.findFirstByUser_IdAndActiveTrueOrderByCreatedAtDesc(user.getId())
                .orElseGet(() -> createNewSession(user));
    }

    private ChatSession createNewSession(User user) {
        log.info("Creating new chat session for user: {}", user.getDisplayName());
        
        ChatSession session = ChatSession.forUser(user);
        // Don't generate greeting here - frontend will request it via __START_SESSION__
        // This prevents duplicate greetings caused by race conditions between
        // session creation and message fetching
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSession getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ChatSession", sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(UUID sessionId) {
        return messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
    }

    @Override
    @Transactional
    public Flux<ChatResponseChunk> sendMessageStream(UUID sessionId, String content) {
        ChatSession session = getSession(sessionId);
        ChatContext context = contextService.buildContext();
        
        // Save user message
        ChatMessage userMessage = ChatMessage.userMessage(content);
        session.addMessage(userMessage);
        messageRepository.save(userMessage);
        
        // Build conversation context for LLM
        String conversationContext = buildConversationContext(sessionId);
        String prompt = String.format(
                ChatPrompts.RESPONSE_PROMPT,
                content,
                conversationContext,
                context.toSystemPromptContext()
        );
        
        // Stream response from LLM
        StringBuilder fullResponse = new StringBuilder();
        
        return llmService.generateStream(ChatPrompts.LEARNING_COACH_SYSTEM, prompt)
                .map(chunk -> {
                    fullResponse.append(chunk);
                    return new ChatResponseChunk(ChatResponseChunk.ChunkType.TEXT, chunk);
                })
                .concatWith(Flux.defer(() -> {
                    // Process complete response
                    String response = fullResponse.toString();
                    return processAndSaveResponse(session, response, context);
                }))
                .onErrorResume(e -> {
                    log.error("Error streaming response", e);
                    // Fallback to non-streaming
                    return Mono.fromCallable(() -> {
                        ChatResponse nonStreamResponse = sendMessage(sessionId, content);
                        return new ChatResponseChunk(
                                ChatResponseChunk.ChunkType.DONE,
                                nonStreamResponse.message().getContent()
                        );
                    }).flux();
                });
    }

    private Flux<ChatResponseChunk> processAndSaveResponse(
            ChatSession session,
            String response,
            ChatContext context) {
        
        List<ChatResponseChunk> chunks = new ArrayList<>();
        
        // Check for activity tags
        Matcher matcher = ACTIVITY_PATTERN.matcher(response);
        if (matcher.find()) {
            String activityType = matcher.group(1).toUpperCase();
            String topic = matcher.group(2).trim();
            String textContent = response.substring(0, matcher.start()).trim();
            
            // Emit activity start
            chunks.add(new ChatResponseChunk(ChatResponseChunk.ChunkType.ACTIVITY_START, activityType));
            
            try {
                // Generate activity content
                String activityContent = generateActivityContent(activityType, topic);
                EmbeddedActivityType type = parseActivityType(activityType);
                
                // Save message with embedded activity
                ChatMessage assistantMessage = ChatMessage.assistantMessageWithActivity(
                        textContent,
                        type,
                        activityContent
                );
                session.addMessage(assistantMessage);
                messageRepository.save(assistantMessage);
                
                // Emit activity data
                chunks.add(new ChatResponseChunk(ChatResponseChunk.ChunkType.ACTIVITY_DATA, activityContent));
                chunks.add(new ChatResponseChunk(ChatResponseChunk.ChunkType.ACTIVITY_END, null));
                
            } catch (Exception e) {
                log.error("Failed to generate activity: {}", activityType, e);
                // Save message without activity
                ChatMessage assistantMessage = ChatMessage.assistantMessage(response);
                session.addMessage(assistantMessage);
                messageRepository.save(assistantMessage);
            }
        } else {
            // No activity, just text
            ChatMessage assistantMessage = ChatMessage.assistantMessage(response);
            session.addMessage(assistantMessage);
            messageRepository.save(assistantMessage);
        }
        
        chunks.add(new ChatResponseChunk(ChatResponseChunk.ChunkType.DONE, null));
        
        return Flux.fromIterable(chunks);
    }

    private static final String START_SESSION_MARKER = "__START_SESSION__";

    @Override
    @Transactional
    public ChatResponse sendMessage(UUID sessionId, String content) {
        ChatSession session = getSession(sessionId);
        ChatContext context = contextService.buildContext();
        
        // Handle session start request - generate greeting without saving the marker as a user message
        if (START_SESSION_MARKER.equals(content)) {
            return generateGreeting(session, context);
        }
        
        // Save user message
        ChatMessage userMessage = ChatMessage.userMessage(content);
        session.addMessage(userMessage);
        messageRepository.save(userMessage);
        
        // Build conversation context for LLM
        String conversationContext = buildConversationContext(sessionId);
        String prompt = String.format(
                ChatPrompts.RESPONSE_PROMPT,
                content,
                conversationContext,
                context.toSystemPromptContext()
        );
        
        // Generate response
        String response = llmService.generate(ChatPrompts.LEARNING_COACH_SYSTEM, prompt);
        
        // Process response for activities
        ChatMessage assistantMessage = processResponse(session, response);
        
        // Generate suggestions
        List<String> suggestions = generateSuggestions(context);
        
        return new ChatResponse(assistantMessage, suggestions);
    }

    private ChatMessage processResponse(ChatSession session, String response) {
        Matcher matcher = ACTIVITY_PATTERN.matcher(response);
        
        if (matcher.find()) {
            String activityType = matcher.group(1).toUpperCase();
            String topic = matcher.group(2).trim();
            String textContent = response.substring(0, matcher.start()).trim();
            
            try {
                String activityContent = generateActivityContent(activityType, topic);
                EmbeddedActivityType type = parseActivityType(activityType);
                
                ChatMessage message = ChatMessage.assistantMessageWithActivity(
                        textContent,
                        type,
                        activityContent
                );
                session.addMessage(message);
                return messageRepository.save(message);
                
            } catch (Exception e) {
                log.error("Failed to generate activity: {}", activityType, e);
            }
        }
        
        // No activity or failed - just text
        ChatMessage message = ChatMessage.assistantMessage(response);
        session.addMessage(message);
        return messageRepository.save(message);
    }

    @Override
    @Transactional
    public ChatResponse generateGreeting(ChatSession session, ChatContext context) {
        String prompt = String.format(ChatPrompts.GREETING_PROMPT, context.toSystemPromptContext());
        
        String response = llmService.generate(ChatPrompts.LEARNING_COACH_SYSTEM, prompt);
        
        ChatMessage message = processResponse(session, response);
        List<String> suggestions = generateSuggestions(context);
        
        return new ChatResponse(message, suggestions);
    }

    @Override
    @Transactional
    public void completeActivity(UUID messageId, int score, String feedback) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("ChatMessage", messageId));
        
        if (!message.hasEmbeddedActivity()) {
            throw new IllegalStateException("Message does not have an embedded activity");
        }
        
        // Generate summary
        String summary = String.format(
                "%s completed with %d%% score.%s",
                message.getEmbeddedActivityType().name(),
                score,
                feedback != null ? " " + feedback : ""
        );
        
        message.completeActivity(summary);
        messageRepository.save(message);
        
        log.info("Activity completed: {} with score {}%", message.getEmbeddedActivityType(), score);
    }

    @Override
    @Transactional
    public void clearSession(UUID sessionId) {
        ChatSession session = getSession(sessionId);
        messageRepository.deleteBySession_Id(sessionId);
        session.setMessageCount(0);
        session.getMessages().clear();
        sessionRepository.save(session);
        
        log.info("Cleared session: {}", sessionId);
    }

    private String buildConversationContext(UUID sessionId) {
        List<ChatMessage> recentMessages = messageRepository.findLastNMessages(sessionId, MAX_CONTEXT_MESSAGES);
        
        if (recentMessages.isEmpty()) {
            return "No previous conversation.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : recentMessages) {
            String role = msg.getRole() == MessageRole.USER ? "User" : "Assistant";
            String content = msg.getContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append(role).append(": ").append(content).append("\n");
        }
        
        return sb.toString();
    }

    private String generateActivityContent(String activityType, String topic) {
        log.debug("Generating activity: type={}, topic={}", activityType, topic);
        
        return switch (activityType.toUpperCase()) {
            case "VOCABULARY" -> contentGenerationService.generateVocabulary(topic, 8);
            case "FLASHCARDS" -> contentGenerationService.generateFlashcards(topic, 8);
            case "VISUAL_CARDS" -> contentGenerationService.generateVisualVocabulary(topic, 6);
            case "TEXT_COMPLETION" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.TEXT_COMPLETION, topic, 5, null);
            case "DRAG_DROP" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.DRAG_DROP, topic, 5, null);
            case "TRANSLATION" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.TRANSLATION, topic, 5, null);
            case "LISTENING" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.LISTENING, topic, 5, null);
            case "LISTENING_COMPREHENSION" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.LISTENING_COMPREHENSION, topic, 1, 
                    Map.of("wordCount", 100, "statementCount", 5));
            case "SPEAKING" -> contentGenerationService.generateExercises(
                    ExerciseGenerationType.SPEAKING, topic, 5, null);
            case "LESSON" -> contentGenerationService.generateLesson(topic);
            case "SCENARIO" -> contentGenerationService.generateRoleplayScenario(topic);
            case "PAIR_MATCHING", "MEMORY_GAME" -> contentGenerationService.generateVocabulary(topic, 8);
            default -> throw new IllegalArgumentException("Unknown activity type: " + activityType);
        };
    }

    private EmbeddedActivityType parseActivityType(String type) {
        return switch (type.toUpperCase()) {
            case "VOCABULARY" -> EmbeddedActivityType.VOCABULARY;
            case "FLASHCARDS" -> EmbeddedActivityType.FLASHCARDS;
            case "VISUAL_CARDS" -> EmbeddedActivityType.VISUAL_CARDS;
            case "TEXT_COMPLETION" -> EmbeddedActivityType.TEXT_COMPLETION;
            case "DRAG_DROP" -> EmbeddedActivityType.DRAG_DROP;
            case "TRANSLATION" -> EmbeddedActivityType.TRANSLATION;
            case "LISTENING" -> EmbeddedActivityType.LISTENING;
            case "LISTENING_COMPREHENSION" -> EmbeddedActivityType.LISTENING_COMPREHENSION;
            case "SPEAKING" -> EmbeddedActivityType.SPEAKING;
            case "LESSON" -> EmbeddedActivityType.LESSON;
            case "SCENARIO" -> EmbeddedActivityType.SCENARIO;
            case "PAIR_MATCHING" -> EmbeddedActivityType.PAIR_MATCHING;
            case "MEMORY_GAME" -> EmbeddedActivityType.MEMORY_GAME;
            default -> throw new IllegalArgumentException("Unknown activity type: " + type);
        };
    }

    private List<String> generateSuggestions(ChatContext context) {
        List<String> suggestions = new ArrayList<>();
        
        // Get translations for user's native language
        User user = userService.getCurrentUser();
        String nativeLang = user.getNativeLanguage();
        Map<String, String> translations = SUGGESTION_TRANSLATIONS.getOrDefault(
                nativeLang, 
                SUGGESTION_TRANSLATIONS.get("en") // Fallback to English
        );
        
        // Based on context, suggest relevant actions
        if (context.getGoals() != null && context.getGoals().getDailyGoals() != null) {
            boolean hasIncompleteVocabGoal = context.getGoals().getDailyGoals().stream()
                    .anyMatch(g -> !g.isCompleted() && g.getTitle().toLowerCase().contains("vocabulary"));
            
            if (hasIncompleteVocabGoal) {
                suggestions.add(translations.get("PRACTICE_VOCAB"));
            }
        }
        
        suggestions.add(translations.get("DO_EXERCISES"));
        suggestions.add(translations.get("START_LESSON"));
        
        if (context.getRecentActivity() != null && context.getRecentActivity().getExercisesToday() > 0) {
            suggestions.add(translations.get("REVIEW"));
        }
        
        return suggestions.subList(0, Math.min(suggestions.size(), 4));
    }
}