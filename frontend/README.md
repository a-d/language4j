# Frontend Architecture

Vanilla JavaScript single-page application for the Language Learning Platform.

## Directory Structure

```
frontend/
├── index.html              # Main HTML file
├── css/
│   ├── main.css           # Core styles (layout, variables, base components)
│   └── components/        # Component-specific styles
│       ├── cards.css      # Card components
│       ├── exercises.css  # Exercise UI styles
│       └── navbar.css     # Navigation bar
├── js/
│   ├── app.js             # Entry point - coordinates modules
│   ├── config.js          # Configuration (API URL, etc.)
│   ├── api/
│   │   └── client.js      # Backend API client
│   ├── pages/             # Page-specific modules
│   │   ├── dashboard.js   # Daily goals, activity
│   │   ├── lessons.js     # Lesson generation
│   │   ├── vocabulary.js  # Vocabulary & flashcards
│   │   ├── exercises.js   # Interactive exercises
│   │   ├── progress.js    # Progress tracking
│   │   └── settings.js    # User settings
│   └── services/          # Shared services
│       ├── content-renderer.js  # Markdown/JSON to HTML
│       ├── goals.js       # Goal CRUD operations
│       ├── i18n.js        # Internationalization
│       ├── markdown.js    # Markdown parser
│       ├── toast.js       # Toast notifications
│       ├── router.js      # (Currently unused)
│       └── ui.js          # Modal, loading, speech
└── README.md              # This file
```

## Module Overview

### Entry Point: `app.js`
- Initializes application
- Manages global state (user, currentPage)
- Coordinates between page modules and services
- Registers global functions for inline event handlers

### Page Modules (`pages/`)
Each page module handles its specific functionality:

| Module | Purpose | Key Exports |
|--------|---------|-------------|
| `dashboard.js` | Daily goals display | `loadDashboardData()` |
| `lessons.js` | Lesson generation | `loadLessonsData()`, `generateLesson()` |
| `vocabulary.js` | Vocabulary & flashcards | `loadVocabularyData()`, `generateVocabulary()`, `generateFlashcards()` |
| `exercises.js` | Exercise handling | `startExercise()`, `closeExercise()` |
| `progress.js` | Stats & long-term goals | `loadProgressData()` |
| `settings.js` | Profile management | `loadSettingsData()`, `showEditProfileModal()` |

### Service Modules (`services/`)

| Module | Purpose | Key Exports |
|--------|---------|-------------|
| `content-renderer.js` | Renders Markdown/JSON | `renderContent()`, `ContentType` |
| `goals.js` | Goal CRUD operations | `incrementGoal()`, `completeGoal()`, `deleteGoal()`, etc. |
| `i18n.js` | Translations | `t()`, `setLanguage()`, `getLanguageName()` |
| `markdown.js` | Markdown to HTML | `renderMarkdown()` |
| `toast.js` | Notifications | `toast.success()`, `toast.error()`, `toast.info()` |
| `ui.js` | UI utilities | `showLoading()`, `hideLoading()`, `openModal()`, `closeModal()`, `speakText()` |

### API Client (`api/client.js`)
REST client for backend communication:

```javascript
api.users.getCurrent()
api.users.update(data)
api.goals.list(type?)
api.goals.create(data)
api.goals.incrementProgress(id, amount)
api.goals.complete(id)
api.goals.delete(id)
api.content.generateLesson(topic)
api.content.generateVocabulary(topic, wordCount)
api.content.generateFlashcards(topic, cardCount)
api.content.generateScenario(scenario)
api.exercises.generate(type, topic, count?, options?)  // Unified exercise generation
api.exercises.evaluate(exercise, userResponse, expected)
api.exercises.evaluatePronunciation(expectedText, transcription)
api.exercises.saveResult(result)
api.exercises.getHistory({ type?, page?, size? })
api.exercises.getRecentResults(days?)
api.exercises.getStatistics()
api.speech.synthesize(text, languageCode, slow?, voice?)
api.speech.transcribe(audioFile, languageHint?)
api.chat.getOrCreateSession()
api.chat.sendMessage(sessionId, content)
api.i18n.getLanguage(languageCode)
api.images.generate(prompt, options?)
api.images.generateFlashcard(word, context?)
```

## Content Type Handling

Backend returns `GeneratedContentResponse` with `content` (string) and `type` (string).

| Type | Format | Renderer |
|------|--------|----------|
| `lesson` | Markdown | `renderMarkdown()` |
| `vocabulary` | Markdown | `renderMarkdown()` |
| `scenario` | Markdown | `renderMarkdown()` |
| `learning-plan` | Markdown | `renderMarkdown()` |
| `flashcards` | JSON | Interactive flip cards |
| `text-completion` | JSON | Fill-in-the-blank exercises |
| `drag-drop` | JSON | Word ordering exercises |
| `translation` | JSON | Translation exercises |
| `evaluation` | JSON | Feedback display |

## Global Functions

Functions registered on `window` for inline HTML event handlers:

**UI Functions:**
- `closeModal()` - Close active modal
- `closeExercise()` - Close exercise area
- `speakText(text)` - Text-to-speech

**Content Generation:**
- `generateLesson()` - Generate new lesson
- `generateVocabulary()` - Generate vocabulary list
- `generateFlashcards()` - Generate flashcards

**Goal Management:**
- `incrementGoal(id)` - +1 to goal progress
- `completeGoal(id)` - Mark goal complete
- `deleteGoal(id)` - Delete goal
- `createDefaultGoals()` - Create starter goals
- `showCreateGoalModal()` - Show goal creation form
- `submitCreateGoal()` - Submit new goal

**Profile:**
- `showEditProfileModal()` - Show profile editor
- `submitProfileUpdate()` - Save profile changes

## CSS Architecture

- **Variables** in `:root` for consistent theming
- **Base styles** for typography, layout, forms
- **Component styles** separated by concern
- **Responsive** via media queries

Key CSS classes:
- `.page`, `.hidden` - Page visibility
- `.btn`, `.btn-primary`, `.btn-secondary` - Buttons
- `.form-input`, `.form-group` - Form elements
- `.goal-card`, `.progress-bar` - Goal display
- `.flashcard`, `.flashcard-inner` - Flashcard UI
- `.exercise-item`, `.exercise-input` - Exercise components
- `.markdown-content` - Rendered markdown styling

## Adding a New Feature

1. **New page**: Create `pages/newpage.js`, export functions, import in `app.js`
2. **New service**: Create `services/newservice.js`, export functions
3. **New API endpoint**: Add to `api/client.js` in appropriate category
4. **New styles**: Add to `css/main.css` or create `css/components/feature.css`

## Development Notes

- No build step required - uses ES modules directly
- Browser compatibility: Modern browsers with ES6+ module support
- API URL configured via `window.APP_CONFIG.API_URL` or defaults to `/api`