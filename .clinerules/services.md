# Service Layer Documentation

## Overview

This document describes the service layer implementations in the Language Learning Platform.

## Service Modules

### learning-service

Located in `backend/learning-service/`

#### UserService
**Interface**: `dev.languagelearning.learning.service.UserService`
**Implementation**: `dev.languagelearning.learning.service.impl.UserServiceImpl`

**Responsibilities**:
- Manage user profiles and preferences
- Handle single-user mode for personal deployments
- Track skill level progression

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getCurrentUser()` | Gets or creates the current user based on language configuration |
| `findById(id)` | Finds a user by ID |
| `createUser(displayName, nativeLanguage, targetLanguage)` | Creates a new user |
| `updateSkillLevel(userId, skillLevel)` | Updates CEFR skill level (A1-C2) |
| `completeAssessment(userId)` | Marks initial assessment as complete |
| `updateDisplayName(userId, displayName)` | Updates user display name |

#### GoalService
**Interface**: `dev.languagelearning.learning.service.GoalService`
**Implementation**: `dev.languagelearning.learning.service.impl.GoalServiceImpl`

**Responsibilities**:
- Create and manage learning goals (daily, weekly, monthly, yearly)
- Track progress toward goals
- Auto-complete goals when targets are reached
- Create default goals for new users

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getCurrentUserGoals()` | Gets all goals for the current user |
| `getCurrentUserGoalsByType(type)` | Gets goals filtered by type |
| `getActiveDailyGoals()` | Gets active (incomplete) daily goals |
| `createGoal(title, description, type, targetValue, unit)` | Creates a new goal |
| `updateProgress(goalId, newValue)` | Updates goal progress |
| `incrementProgress(goalId, increment)` | Increments progress by amount |
| `createDefaultGoals(userId)` | Creates default goals for new users |

---

### content-service

Located in `backend/content-service/`

#### ContentGenerationService
**Interface**: `dev.languagelearning.content.service.ContentGenerationService`
**Implementation**: `dev.languagelearning.content.service.impl.ContentGenerationServiceImpl`

**Responsibilities**:
- Generate learning content using LLM
- Create lessons, vocabulary, exercises
- Generate roleplay scenarios
- Create personalized learning plans
- Evaluate user responses

**Key Methods**:
| Method | Description |
|--------|-------------|
| `generateLesson(topic)` | Generates a lesson on a topic |
| `generateVocabulary(topic, wordCount)` | Generates vocabulary list |
| `generateTextCompletionExercises(topic, count)` | Creates fill-in-the-blank exercises |
| `generateDragDropExercises(topic, count)` | Creates word-ordering exercises |
| `generateTranslationExercises(topic, count)` | Creates translation exercises |
| `generateFlashcards(topic, count)` | Creates flashcard data |
| `generateRoleplayScenario(scenario)` | Creates roleplay dialogue |
| `generateLearningPlan(daily, weekly, monthly)` | Creates personalized learning plan |
| `evaluateResponse(exercise, userResponse, expected)` | Evaluates user answers |

---

### speech-service

Located in `backend/speech-service/`

#### SpeechService
**Interface**: `dev.languagelearning.speech.service.SpeechService`
**Implementation**: `dev.languagelearning.speech.service.impl.SpeechServiceImpl`

**Responsibilities**:
- Text-to-speech audio generation
- Speech-to-text transcription
- Audio format management
- Voice selection and speed control

**Key Methods**:
| Method | Description |
|--------|-------------|
| `textToSpeech(text, languageCode)` | Convert text to speech audio |
| `textToSpeech(text, options)` | Convert text to speech with custom options |
| `speechToText(audioData, languageHint)` | Transcribe audio bytes to text |
| `speechToText(audioStream, languageHint)` | Transcribe audio stream to text |

**Speech Options**:
- `SpeechOptions.defaults(languageCode)` - Standard settings
- `SpeechOptions.slow(languageCode)` - Slower speed (0.75x) for learning
- `SpeechOptions.withVoice(languageCode, voice)` - Custom voice selection

**Available Voices**: ALLOY, ECHO, FABLE, ONYX, NOVA, SHIMMER

**Audio Formats**: MP3, OPUS, AAC, FLAC, WAV

---

### image-service

Located in `backend/image-service/`

#### ImageService
**Interface**: `dev.languagelearning.image.service.ImageService`
**Implementation**: `dev.languagelearning.image.service.impl.ImageServiceImpl`

**Responsibilities**:
- Generate images for flashcards and learning materials
- AI-powered image generation (DALL-E)
- Image size and quality management

**Key Methods**:
| Method | Description |
|--------|-------------|
| `generate(prompt)` | Generate image from text prompt |
| `generate(prompt, options)` | Generate with custom options |
| `generateAsync(prompt)` | Asynchronous image generation |
| `generateFlashcardImage(word, targetLang, context)` | Generate flashcard-optimized image |

**Image Options**:
- `ImageGenerationOptions.defaults()` - Standard quality (1024x1024)
- `ImageGenerationOptions.highQuality()` - HD quality with vivid style
- `ImageGenerationOptions.forFlashcard()` - Medium size for flashcards

**Image Sizes**: SMALL (256x256), MEDIUM (512x512), LARGE (1024x1024), WIDE (1792x1024), TALL (1024x1792)

---

## API Controllers

Located in `backend/api-module/src/main/java/dev/languagelearning/api/controller/`

### UserController
**Path**: `/api/v1/users`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/me` | GET | Get current user |
| `/me` | PUT | Update current user |

### GoalController
**Path**: `/api/v1/goals`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Get all goals (optional type filter) |
| `/` | POST | Create new goal |
| `/daily/active` | GET | Get active daily goals |
| `/{goalId}/progress` | PATCH | Update goal progress |
| `/{goalId}/increment` | PATCH | Increment goal progress |
| `/{goalId}/complete` | POST | Mark goal as complete |
| `/{goalId}` | DELETE | Delete a goal |

### ContentController
**Path**: `/api/v1/content`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/lessons/generate` | POST | Generate a lesson |
| `/vocabulary/generate` | POST | Generate vocabulary |
| `/flashcards/generate` | POST | Generate flashcards |
| `/scenarios/generate` | POST | Generate roleplay scenario |
| `/learning-plan/generate` | POST | Generate learning plan |

### ExerciseController
**Path**: `/api/v1/exercises`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/text-completion` | POST | Generate fill-in-blank exercises |
| `/drag-drop` | POST | Generate word-order exercises |
| `/translation` | POST | Generate translation exercises |
| `/evaluate` | POST | Evaluate user response |

---

## DTO Naming Conventions

### Request DTOs
- `Create*Request` - For creating new entities
- `Update*Request` - For updating existing entities
- `Generate*Request` - For generating content

### Response DTOs
- `*Dto` - Standard data transfer objects
- `Generated*Response` - For generated content responses

### Examples
```
UserDto
GoalDto
CreateGoalRequest
UpdateUserRequest
GenerateExerciseRequest
GeneratedContentResponse
```

---

## Service Dependencies

```
ContentGenerationService
    ├── LlmService (llm-module)
    ├── UserService (learning-service)
    └── LanguageConfig (config-module)

GoalService
    ├── LearningGoalRepository (core-module)
    └── UserService (learning-service)

UserService
    ├── UserRepository (core-module)
    └── LanguageConfig (config-module)
```

---

## Adding New Services

1. Create interface in `service/` package
2. Create implementation in `service/impl/` package
3. Add `@Service` annotation to implementation
4. Use constructor injection with `@RequiredArgsConstructor`
5. Add corresponding controller in api-module
6. Create request/response DTOs
7. Update this documentation