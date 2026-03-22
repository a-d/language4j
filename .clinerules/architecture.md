# Architecture Guidelines

## Module Dependencies

```
                    ┌─────────────────┐
                    │   api-module    │ (Spring Boot Application)
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│content-service│   │learning-service│  │ speech-service│
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        └─────────┬─────────┴─────────┬─────────┘
                  │                   │
                  ▼                   ▼
          ┌─────────────┐     ┌─────────────┐
          │  llm-module │     │image-service│
          └──────┬──────┘     └──────┬──────┘
                 │                   │
                 └─────────┬─────────┘
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

### REST Endpoints Structure
```
/api/v1/
├── config/           # Configuration endpoints
│   ├── user         # User settings
│   └── ai           # AI provider settings
├── learning/         # Learning management
│   ├── plan         # Learning plans
│   ├── goals        # Learning goals
│   └── progress     # Progress tracking
├── content/          # Content management
│   ├── lessons      # Lessons
│   ├── cards        # Learning cards
│   ├── vocabulary   # Vocabulary lists
│   └── scenarios    # Roleplay scenarios
├── exercises/        # Exercise endpoints
│   ├── completion   # Text completion
│   ├── dragdrop     # Drag-and-drop
│   └── speaking     # Speaking exercises
└── speech/           # Speech endpoints
    ├── transcribe   # Speech-to-text
    └── synthesize   # Text-to-speech
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