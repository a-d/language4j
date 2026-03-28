# Language Learning Platform - Implementation Roadmap

> **Document Version**: 2.0  
> **Last Updated**: March 2026  
> **Status**: Active Development

## Executive Summary

This document compares the original project vision against the current implementation and provides a detailed roadmap for completing the remaining features. The project is approximately **90% complete** with core functionality working.

---

## Original Project Vision

From the initial query, the project was conceived as:

> A deployable language learning platform where users specify their native language, target language, and AI provider credentials to receive a personalized learning experience with LLM-planned lessons, tutorials, scenarios, vocabulary, and roleplay.

### Key Requirements from Original Query

1. **Deployment Configuration**
   - Docker-based deployment
   - User-configurable language settings (native/target)
   - AI API configuration (LLM, TTS, STT, image generation)

2. **LLM-Powered Content**
   - Learning plan generation
   - Tutorials and lessons
   - Vocabulary lists
   - Roleplay scenarios

3. **Goal-Based Learning**
   - Daily, weekly, monthly, yearly goals
   - Skill level tracking and adaptation

4. **Best Practice Learning Methods**
   - Learning cards (flashcards)
   - Text completion exercises
   - Listen-and-repeat
   - Speaking aloud practice

5. **Interactive Website**
   - Text input
   - Drag-and-drop text construction
   - Microphone access for training

---

## Current Implementation Status

### ✅ Fully Implemented

| Category | Feature | Implementation |
|----------|---------|----------------|
| **Chat Moderation** | AI Learning Coach | `chat-service` module |
| | Session management | `ChatSession`, `ChatMessage` entities |
| | Embedded activities | Activities within chat messages |
| | Learning context | User progress, goals, and history |
| **Infrastructure** | Docker deployment | `docker/docker-compose.yml` |
| | Multi-provider AI support | OpenAI, Anthropic, Ollama |
| | Configuration management | `config-module` with env vars |
| | PostgreSQL persistence | Flyway migrations |
| **Content Generation** | Lesson generation | `POST /api/v1/content/lessons/generate` |
| | Vocabulary generation | `POST /api/v1/content/vocabulary/generate` |
| | Flashcard generation | `POST /api/v1/content/flashcards/generate` |
| | Roleplay scenarios | `POST /api/v1/content/scenarios/generate` |
| | Learning plan generation | `POST /api/v1/content/learning-plan/generate` |
| **Exercises** | Fill-in-the-blank | `POST /api/v1/exercises/generate` |
| | Word ordering (drag-drop) | `POST /api/v1/exercises/generate` |
| | Translation | `POST /api/v1/exercises/generate` |
| | Listening comprehension | `POST /api/v1/exercises/generate` |
| | Speaking/pronunciation | `POST /api/v1/exercises/generate` |
| | Response evaluation | `POST /api/v1/exercises/evaluate` |
| | Pronunciation evaluation | `POST /api/v1/exercises/evaluate-pronunciation` |
| **Speech** | Text-to-speech | `POST /api/v1/speech/synthesize` |
| | Speech-to-text | `POST /api/v1/speech/transcribe` |
| | Slow speech for learning | `slow` parameter |
| **Images** | AI image generation | `POST /api/v1/images/generate` |
| | Flashcard images | `POST /api/v1/images/flashcard` |
| | Batch image generation | `POST /api/v1/images/flashcard/batch` |
| **Goals** | CRUD operations | All goal endpoints |
| | Progress tracking | Update/increment progress |
| | Auto-completion | Goals auto-complete at target |
| | Time-based types | Daily/weekly/monthly/yearly |
| **User** | Multi-user support | User selector, switching, deletion |
| | Profile management | `GET/PUT /api/v1/users/me` |
| | Skill level (CEFR) | A1-C2 levels |
| | Assessment tracking | `assessmentCompleted` flag |
| **Frontend** | Dashboard | Goal display, quick actions |
| | Lessons page | Generate and view lessons |
| | Vocabulary page | Generate vocabulary lists |
| | Cards page | Visual flashcards with images |
| | Exercises page | All 5 exercise types |
| | Progress page | Statistics display |
| | Settings page | Profile management |
| | Dark mode | Theme toggle with persistence |
| | Internationalization | Dynamic i18n via LLM |
| **Local AI** | Ollama integration | Full LLM support |
| | Whisper (local STT) | `local-deployment/` |
| | Piper (local TTS) | `local-deployment/` |
| | Stable Diffusion | `local-deployment/` |

### ⚠️ Partially Implemented

| Feature | Current State | Missing Parts |
|---------|--------------|---------------|
| **Progress Analytics** | Basic statistics exist | Charts, trends, insights |
| **Day Streak** | Displayed in UI | Not calculated from data |
| **Content Persistence** | DB for structured data | Markdown file storage not used |
| **Gamification** | Basic scoring | Points, badges, achievements |

### ❌ Not Yet Implemented

| Feature | Priority | Complexity |
|---------|----------|------------|
| **Listen-and-Repeat Mode** | 🟡 Medium | Low |
| **Interactive Roleplay Chat** | 🟡 Medium | Medium |

---

## Detailed Feature Specifications

### 1. Listen-and-Repeat Mode

**Priority**: 🟡 Medium  
**Effort**: 2-3 days  
**Dependencies**: TTS and STT services (already implemented)

#### Description
A unified exercise mode that:
1. Plays audio of a phrase (TTS)
2. User listens and repeats
3. Records user's speech (STT)
4. Evaluates pronunciation
5. Shows feedback and plays again if needed

#### Implementation Plan

This feature mostly requires frontend work since the backend APIs already exist.

**Frontend Changes:**
```
frontend/js/
├── pages/
│   └── exercises.js                     # Add listen-repeat mode
├── components/
│   └── listen-repeat-exercise.js        # New component
```

**Exercise Flow:**
```javascript
class ListenRepeatExercise {
    async start(phrases) {
        for (const phrase of phrases) {
            // 1. Play audio (slow)
            await this.playAudio(phrase.text, { slow: true });
            
            // 2. Show "Your turn" indicator
            this.showPrompt("Now repeat what you heard");
            
            // 3. Start recording
            const recording = await this.recordAudio();
            
            // 4. Transcribe
            const transcription = await api.speech.transcribe(recording);
            
            // 5. Evaluate
            const evaluation = await api.exercises.evaluatePronunciation(
                phrase.text,
                transcription
            );
            
            // 6. Show feedback
            this.showFeedback(evaluation);
            
            // 7. Allow replay
            await this.waitForContinue();
        }
    }
}
```

**UI Design:**
```
┌─────────────────────────────────────┐
│  Listen and Repeat                  │
├─────────────────────────────────────┤
│                                     │
│     🔊 [Play Audio]                 │
│                                     │
│     "Bonjour, comment allez-vous?"  │
│                                     │
│     🎤 [Record] (pulsing when on)   │
│                                     │
│     Your attempt: "Bonjour..."      │
│                                     │
│     ✅ 85% accuracy - Good job!     │
│                                     │
│     [Next] [Replay] [Skip]          │
│                                     │
└─────────────────────────────────────┘
```

---

### 2. Interactive Roleplay Chat

**Priority**: 🟡 Medium  
**Effort**: 4-5 days  
**Dependencies**: Content generation (scenarios)

#### Description
Transform static roleplay scenarios into interactive conversations where the user plays one role and the AI plays the other.

#### Implementation Plan

**Backend Changes:**
```
content-service/
├── service/
│   └── RoleplayService.java             # New service for chat
api-module/
├── controller/
│   └── RoleplayController.java          # New controller
├── dto/
│   ├── RoleplaySessionDto.java
│   ├── RoleplayMessageRequest.java
│   └── RoleplayMessageResponse.java
```

**API Endpoints:**
```
POST /api/v1/roleplay/start              # Start session with scenario
POST /api/v1/roleplay/message            # Send message, get AI response
GET  /api/v1/roleplay/hint               # Get hint for user's next line
POST /api/v1/roleplay/evaluate           # Evaluate conversation
```

**Session Management:**
```java
public class RoleplayService {
    // In-memory or Redis session storage
    private Map<UUID, RoleplaySession> sessions;
    
    public RoleplaySession startSession(String scenarioType) {
        // Generate scenario with LLM
        var scenario = contentService.generateRoleplayScenario(scenarioType);
        
        // Parse into characters and setting
        var session = new RoleplaySession()
            .setId(UUID.randomUUID())
            .setScenario(scenario)
            .setUserRole("Customer")  // or dynamic
            .setAiRole("Waiter")
            .setMessages(new ArrayList<>());
        
        // AI opens conversation
        String opening = generateAiResponse(session, null);
        session.addMessage(new Message(session.getAiRole(), opening));
        
        return session;
    }
    
    public RoleplayMessageResponse sendMessage(UUID sessionId, String userMessage) {
        var session = sessions.get(sessionId);
        session.addMessage(new Message(session.getUserRole(), userMessage));
        
        // Get AI response considering context
        String aiResponse = generateAiResponse(session, userMessage);
        session.addMessage(new Message(session.getAiRole(), aiResponse));
        
        // Check if conversation goal achieved
        boolean completed = checkGoalAchieved(session);
        
        return new RoleplayMessageResponse(aiResponse, completed);
    }
}
```

**Frontend:**
- Chat-style interface
- Voice input option (STT)
- Voice output option (TTS)
- Translation hints on hover
- Conversation history

---

## Implementation Timeline

### Phase 1: Interactive Features (1-2 Weeks)

| Week | Tasks |
|------|-------|
| Week 1 | Listen-and-Repeat Mode |
| Week 2 | Interactive Roleplay Chat |

### Phase 2: Polish & Enhancement (Optional)

| Task | Effort |
|------|--------|
| Progress charts & trends | 2-3 days |
| Day streak calculation | 1 day |
| Gamification (badges) | 3-4 days |

---

## Technical Debt & Improvements

### Code Quality
- [ ] Increase test coverage to 80%+
- [ ] Add integration tests for all endpoints
- [ ] Implement proper error handling throughout

### Performance
- [ ] Add caching layer (Redis)
- [ ] Implement request rate limiting
- [ ] Optimize LLM prompts for faster responses

### Security
- [ ] Implement API rate limiting
- [ ] Add input validation on all endpoints

### Documentation
- [ ] API documentation improvements
- [ ] User guide/onboarding
- [ ] Developer contribution guide

---

## Appendix: Feature Request Template

When adding new features, use this template in issues:

```markdown
## Feature: [Name]

### Description
[Brief description]

### User Story
As a [user type], I want [goal] so that [benefit].

### Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2

### Technical Notes
- Backend changes: [...]
- Frontend changes: [...]
- Database changes: [...]

### Dependencies
- [List of dependencies]

### Estimated Effort
[X days/weeks]
```

---

## Conclusion

The Language Learning Platform has a solid foundation with nearly all core features implemented. The remaining features focus on:

1. **Engagement** (interactive roleplay, listen-and-repeat exercises)
2. **Polish** (progress charts, gamification)

Following this roadmap will complete the vision of a comprehensive, deployable, AI-powered language learning platform.