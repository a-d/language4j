# Architecture Guidelines

## Module Dependencies

```
                    ┌─────────────────┐
                    │   api-module    │ (Spring Boot Application)
                    └────────┬────────┘
                             │
     ┌───────────────────────┼───────────────────────┐
     │                       │                       │
     ▼                       ▼                       ▼
┌────────────┐   ┌───────────────────┐   ┌──────────────────┐
│chat-service│   │  content-service  │   │ learning-service │
└─────┬──────┘   └─────────┬─────────┘   └───────┬──────────┘
      │                    │                     │
      │          ┌─────────┴─────────┐           │
      │          │                   │           │
      │          ▼                   ▼           │
      │   ┌─────────────┐     ┌─────────────┐    │
      │   │speech-service│    │image-service│    │
      │   └──────┬──────┘     └──────┬──────┘    │
      │          │                   │           │
      └──────────┴─────────┬─────────┴───────────┘
                           │
                           ▼
                   ┌─────────────┐
                   │  llm-module │
                   └──────┬──────┘
                          │
                          ▼
                   ┌───────────────┐
                   │  core-module  │
                   └───────┬───────┘
                           │
                           ▼
                   ┌───────────────┐
                   │ config-module │
                   └───────────────┘
```

## Module Responsibilities

### config-module
- Application configuration properties
- AI provider configuration (API keys, endpoints)
- User preferences and language settings
- Environment-specific configurations

### core-module
- Domain entities (User, LearningGoal, Lesson, Card, etc.)
- Repository interfaces
- Common utilities and helpers
- Shared DTOs and value objects

### llm-module
- Spring AI client abstraction
- Multi-provider support (OpenAI, Anthropic, Ollama)
- Prompt templates and management
- Response parsing utilities

### api-module
- REST API controllers
- Request/response DTOs
- Security configuration
- CORS and web configuration

### content-service
- Learning content generation
- Markdown content management
- Vocabulary and phrase management
- Scenario and roleplay creation

### learning-service
- Learning plan creation and management
- Progress tracking
- Skill assessment
- Goal management (daily/weekly/monthly/yearly)

### speech-service
- Text-to-speech integration
- Speech-to-text transcription
- Audio file management
- Pronunciation assessment

### image-service
- Learning card image generation
- Image storage and retrieval
- Visual content management

### chat-service
- AI chat coach for interactive learning
- Session management and conversation history
- Embedded learning activities within chat messages
- Streaming response support (Server-Sent Events)
- Activity completion tracking and summaries

## Design Patterns

### Service Layer Pattern
All business logic resides in service classes. Controllers are thin and delegate to services.

### Repository Pattern
Data access is abstracted through repository interfaces. Implementations handle PostgreSQL for structured data and file system for Markdown content.

### Strategy Pattern
AI provider integration uses strategy pattern for multi-provider support.

### Factory Pattern
Content generation uses factories to create different types of learning materials.

## API Design

### REST Endpoints Structure (Current Implementation)
```
/api/v1/
├── chat/             # AI Chat Coach
│   ├── session      # Get/create active session (GET)
│   └── session/{id}/
│       ├── messages      # Get/send messages (GET, POST)
│       └── messages/stream # Stream responses (POST, SSE)
├── users/            # User management
│   └── me           # Current user profile (GET, PUT)
├── goals/            # Learning goals
│   ├── /            # List/create goals (GET, POST)
│   ├── daily/active # Active daily goals (GET)
│   └── {goalId}/    # Goal operations (PATCH progress, POST complete, DELETE)
├── content/          # Content generation
│   ├── lessons/generate      # Generate lessons (POST)
│   ├── vocabulary/generate   # Generate vocabulary (POST)
│   ├── flashcards/generate   # Generate flashcards (POST)
│   ├── scenarios/generate    # Generate roleplay scenarios (POST)
│   └── learning-plan/generate # Generate learning plan (POST)
└── exercises/        # Exercise endpoints
    ├── text-completion  # Fill-in-blank exercises (POST)
    ├── drag-drop        # Word-order exercises (POST)
    ├── translation      # Translation exercises (POST)
    └── evaluate         # Evaluate responses (POST)
```

### Planned Endpoints (Not Yet Implemented)
```
/api/v1/
├── speech/           # Speech endpoints
│   ├── transcribe   # Speech-to-text
│   └── synthesize   # Text-to-speech
└── images/           # Image generation
    └── generate     # Generate learning images
```

## Data Storage Strategy

### PostgreSQL (Structured Data)
- User accounts and preferences
- Learning progress and statistics
- Goals and milestones
- Exercise results and scores
- Audit logs

### Markdown Files (Unstructured/LLM Content)
- Generated lessons and tutorials
- Vocabulary lists with context
- Roleplay scenarios and dialogues
- Learning cards content
- Personalized feedback

### File Naming Convention
```
content/
├── {user-id}/
│   ├── lessons/
│   │   └── {language-pair}/{lesson-id}.md
│   ├── vocabulary/
│   │   └── {language-pair}/vocabulary.md
│   ├── scenarios/
│   │   └── {language-pair}/{scenario-id}.md
│   └── cards/
│       └── {language-pair}/{card-id}.md
└── templates/
    ├── lesson-template.md
    ├── vocabulary-template.md
    └── scenario-template.md
```

## Error Handling

### Exception Hierarchy
```
LanguageLearningException (base)
├── ConfigurationException
├── ContentGenerationException
├── LlmProviderException
├── SpeechProcessingException
├── ImageGenerationException
└── ValidationException
```

### Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Invalid language code",
  "path": "/api/v1/config/user",
  "details": {
    "field": "targetLanguage",
    "rejectedValue": "xyz",
    "message": "Language code must be ISO 639-1 format"
  }
}
