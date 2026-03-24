# Language Learning Platform - Implementation Roadmap

> **Document Version**: 1.0  
> **Last Updated**: March 2026  
> **Status**: Active Development

## Executive Summary

This document compares the original project vision against the current implementation and provides a detailed roadmap for completing the remaining features. The project is approximately **75-80% complete** with core functionality working.

---

## Original Project Vision

From the initial query, the project was conceived as:

> A meta project - a collection of docker images, scripts, configurations, MCP and tool-call assisted applications. A deployable language learning platform where users specify their native language, target language, and AI provider credentials to receive a personalized learning experience with LLM-planned lessons, tutorials, scenarios, vocabulary, and roleplay.

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
   - Skill level assessment and adaptation

4. **Best Practice Learning Methods**
   - Learning cards (flashcards)
   - Text completion exercises
   - Listen-and-repeat
   - Speaking aloud practice

5. **Interactive Website**
   - Text input
   - Drag-and-drop text construction
   - Microphone access for training

6. **MCP Integration**
   - Tool-assisted learning via Model Context Protocol

---

## Current Implementation Status

### ✅ Fully Implemented

| Category | Feature | Implementation |
|----------|---------|----------------|
| **Infrastructure** | Docker deployment | `docker/docker-compose.yml` |
| | Multi-provider AI support | OpenAI, Anthropic, Ollama |
| | Configuration management | `config-module` with env vars |
| | PostgreSQL persistence | Flyway migrations |
| **Content Generation** | Lesson generation | `POST /api/v1/content/lessons/generate` |
| | Vocabulary generation | `POST /api/v1/content/vocabulary/generate` |
| | Flashcard generation | `POST /api/v1/content/flashcards/generate` |
| | Roleplay scenarios | `POST /api/v1/content/scenarios/generate` |
| | Learning plan generation | `POST /api/v1/content/learning-plan/generate` |
| **Exercises** | Fill-in-the-blank | `POST /api/v1/exercises/text-completion` |
| | Word ordering (drag-drop) | `POST /api/v1/exercises/drag-drop` |
| | Translation | `POST /api/v1/exercises/translation` |
| | Listening comprehension | `POST /api/v1/exercises/listening` |
| | Speaking/pronunciation | `POST /api/v1/exercises/speaking` |
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
| **User** | Profile management | `GET/PUT /api/v1/users/me` |
| | Skill level (CEFR) | A1-C2 levels |
| | Assessment tracking | `assessmentCompleted` flag |
| **Frontend** | Dashboard | Goal display, quick actions |
| | Lessons page | Generate and view lessons |
| | Vocabulary page | Generate vocabulary lists |
| | Cards page | Visual flashcards with images |
| | Exercises page | All 5 exercise types |
| | Progress page | Statistics display |
| | Settings page | Profile management |
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
| **Skill Assessment Wizard** | 🔴 High | Medium |
| **Spaced Repetition System (SRS)** | 🔴 High | High |
| **Listen-and-Repeat Mode** | 🟡 Medium | Low |
| **Interactive Roleplay Chat** | 🟡 Medium | Medium |
| **MCP Server** | 🟡 Medium | High |
| **Intelligent Scheduling** | 🟡 Medium | Medium |
| **Multi-User Support** | 🟢 Low | High |
| **Dark Mode** | 🟢 Low | Low |
| **Notifications** | 🟢 Low | Medium |

---

## Detailed Feature Specifications

### 1. Skill Assessment Wizard

**Priority**: 🔴 High  
**Effort**: 3-5 days  
**Dependencies**: None

#### Description
An interactive assessment to determine the user's initial CEFR level (A1-C2) instead of requiring manual selection.

#### Implementation Plan

**Backend Changes:**
```
learning-service/
├── service/
│   └── AssessmentService.java           # New service
├── domain/
│   └── AssessmentQuestion.java          # Question entity
api-module/
├── controller/
│   └── AssessmentController.java        # New controller
├── dto/
│   ├── AssessmentQuestionDto.java
│   ├── AssessmentAnswerRequest.java
│   └── AssessmentResultDto.java
```

**API Endpoints:**
```
POST /api/v1/assessment/start          # Start new assessment
GET  /api/v1/assessment/question       # Get next question
POST /api/v1/assessment/answer         # Submit answer
GET  /api/v1/assessment/result         # Get final result
```

**Algorithm:**
1. Start with B1 level questions (middle ground)
2. If correct, increase difficulty; if wrong, decrease
3. Use adaptive testing with ~10-15 questions
4. Cover: vocabulary, grammar, reading comprehension
5. Calculate final CEFR level with confidence score

**Frontend:**
- New assessment page/modal
- Progress indicator
- Question display with multiple choice
- Result screen with level explanation

---

### 2. Spaced Repetition System (SRS)

**Priority**: 🔴 High  
**Effort**: 5-7 days  
**Dependencies**: Vocabulary system

#### Description
Implement SM-2 or similar algorithm for intelligent vocabulary review scheduling.

#### Implementation Plan

**Database Schema:**
```sql
CREATE TABLE vocabulary_cards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    word VARCHAR(255) NOT NULL,
    translation VARCHAR(255),
    context TEXT,
    image_url TEXT,
    -- SRS fields
    ease_factor DECIMAL(3,2) DEFAULT 2.5,
    interval_days INT DEFAULT 0,
    repetitions INT DEFAULT 0,
    next_review_date TIMESTAMP,
    last_review_date TIMESTAMP,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE review_history (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    user_id UUID NOT NULL,
    quality INT NOT NULL, -- 0-5 rating
    reviewed_at TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES vocabulary_cards(id)
);
```

**Backend Changes:**
```
learning-service/
├── service/
│   ├── VocabularyService.java
│   └── SrsService.java                  # SRS algorithm
├── domain/
│   ├── VocabularyCard.java
│   └── ReviewHistory.java
├── repository/
│   ├── VocabularyCardRepository.java
│   └── ReviewHistoryRepository.java
api-module/
├── controller/
│   └── VocabularyController.java        # Extended
├── dto/
│   ├── VocabularyCardDto.java
│   └── ReviewRequest.java
```

**API Endpoints:**
```
GET  /api/v1/vocabulary/cards            # List all cards
POST /api/v1/vocabulary/cards            # Add card
GET  /api/v1/vocabulary/due              # Get cards due for review
POST /api/v1/vocabulary/review           # Submit review result
GET  /api/v1/vocabulary/statistics       # SRS statistics
```

**SM-2 Algorithm Implementation:**
```java
public class SrsService {
    public void processReview(VocabularyCard card, int quality) {
        // quality: 0-5 (0=complete blackout, 5=perfect response)
        
        if (quality < 3) {
            // Failed - reset
            card.setRepetitions(0);
            card.setIntervalDays(1);
        } else {
            // Success - calculate new interval
            if (card.getRepetitions() == 0) {
                card.setIntervalDays(1);
            } else if (card.getRepetitions() == 1) {
                card.setIntervalDays(6);
            } else {
                card.setIntervalDays((int)(card.getIntervalDays() * card.getEaseFactor()));
            }
            card.setRepetitions(card.getRepetitions() + 1);
        }
        
        // Update ease factor
        double newEf = card.getEaseFactor() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        card.setEaseFactor(Math.max(1.3, newEf));
        
        // Set next review date
        card.setNextReviewDate(LocalDateTime.now().plusDays(card.getIntervalDays()));
    }
}
```

**Frontend:**
- Review session page
- Card flip animation
- Quality rating buttons (Again, Hard, Good, Easy)
- Statistics dashboard

---

### 3. Listen-and-Repeat Mode

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

### 4. Interactive Roleplay Chat

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

### 5. MCP Server Implementation

**Priority**: 🟡 Medium  
**Effort**: 7-10 days  
**Dependencies**: Core platform

#### Description
Implement a Model Context Protocol server to enable tool-assisted learning through AI assistants.

#### Implementation Plan

**Directory Structure:**
```
mcp-server/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts                         # MCP server entry
│   ├── tools/
│   │   ├── vocabulary.ts                # Vocabulary tools
│   │   ├── exercises.ts                 # Exercise tools
│   │   ├── progress.ts                  # Progress tools
│   │   └── speech.ts                    # Speech tools
│   └── resources/
│       ├── lessons.ts                   # Lesson resources
│       └── goals.ts                     # Goal resources
└── README.md
```

**MCP Tools:**
```typescript
// vocabulary.ts
export const vocabularyTools = {
    name: "vocabulary_lookup",
    description: "Look up a word and get translation, examples, and pronunciation",
    inputSchema: {
        type: "object",
        properties: {
            word: { type: "string" },
            targetLanguage: { type: "string" }
        }
    },
    handler: async (params) => {
        // Call backend API
        const response = await fetch(`${API_URL}/api/v1/content/vocabulary/generate`, {
            method: 'POST',
            body: JSON.stringify({ topic: params.word, wordCount: 1 })
        });
        return response.json();
    }
};

export const practiceTools = {
    name: "start_practice",
    description: "Start a practice session for a specific topic",
    inputSchema: {
        type: "object",
        properties: {
            exerciseType: { type: "string", enum: ["fill-blank", "translation", "listening"] },
            topic: { type: "string" }
        }
    },
    handler: async (params) => {
        // Generate exercises via API
    }
};
```

**MCP Resources:**
```typescript
// goals.ts
export const goalsResource = {
    uri: "language-learning://goals/daily",
    name: "Daily Goals",
    description: "Current daily learning goals",
    mimeType: "application/json",
    handler: async () => {
        const response = await fetch(`${API_URL}/api/v1/goals/daily/active`);
        return response.json();
    }
};
```

---

### 6. Intelligent Scheduling

**Priority**: 🟡 Medium  
**Effort**: 3-4 days  
**Dependencies**: Goals, SRS, progress tracking

#### Description
Use LLM to analyze user's progress and suggest personalized daily learning activities.

#### Implementation Plan

**Backend Changes:**
```
learning-service/
├── service/
│   └── SchedulingService.java           # New service
api-module/
├── controller/
│   └── ScheduleController.java          # New controller
├── dto/
│   └── DailyScheduleDto.java
```

**API Endpoints:**
```
GET  /api/v1/schedule/today              # Get today's recommended activities
POST /api/v1/schedule/generate           # Regenerate schedule
```

**Scheduling Logic:**
```java
public class SchedulingService {
    public DailySchedule generateDailySchedule(User user) {
        // Gather data
        var stats = exerciseService.getStatistics(user.getId());
        var dueCards = srsService.getDueCards(user.getId());
        var goals = goalService.getActiveDailyGoals(user.getId());
        var weakAreas = analyzeWeakAreas(stats);
        
        // Build prompt
        String prompt = buildSchedulingPrompt(user, stats, dueCards, goals, weakAreas);
        
        // Get LLM recommendation
        String recommendation = llmService.generate(prompt);
        
        // Parse into schedule
        return parseSchedule(recommendation);
    }
    
    private String buildSchedulingPrompt(/*...*/) {
        return """
            You are a language learning coach. Based on the following data, 
            suggest a 30-minute daily learning session:
            
            User Level: %s
            Words due for review: %d
            Goals: %s
            Weak areas: %s
            Last session: %s
            
            Suggest activities in this format:
            1. [Activity] - [Duration] - [Reason]
            ...
            """.formatted(/*...*/);
    }
}
```

---

## Implementation Timeline

### Phase 1: Core Learning Features (Weeks 1-2)

| Week | Tasks |
|------|-------|
| Week 1 | Skill Assessment Wizard |
| Week 2 | Spaced Repetition System |

### Phase 2: Interactive Features (Weeks 3-4)

| Week | Tasks |
|------|-------|
| Week 3 | Listen-and-Repeat Mode |
| Week 3 | Interactive Roleplay Chat |
| Week 4 | Intelligent Scheduling |

### Phase 3: Extended Features (Weeks 5-6)

| Week | Tasks |
|------|-------|
| Week 5 | MCP Server Implementation |
| Week 6 | UI Polish (Dark Mode, Progress Charts) |

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
- [ ] Add authentication (currently single-user)
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

The Language Learning Platform has a solid foundation with most core features implemented. The remaining features focus on:

1. **Learning effectiveness** (SRS, assessment)
2. **Engagement** (interactive roleplay, gamification)
3. **Accessibility** (MCP integration for tool-assisted learning)

Following this roadmap will complete the original vision of a comprehensive, deployable, AI-powered language learning meta-project.