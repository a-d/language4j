# Demo Data Index

This document catalogs all available demo data for offline mode.

## Configuration

| File | Description |
|------|-------------|
| `config.json` | Demo mode settings (native/target language, disabled features) |
| `index.json` | Master index of available topics and categories |
| `user.json` | Demo user profile |
| `goals.json` | Sample learning goals |

## Available Topics (15 total)

All topics available for content generation:

1. **greetings** - Hello, goodbye, introductions
2. **food** - Dining, restaurants, cuisine
3. **travel** - Directions, transportation, tourism
4. **family** - Relatives, relationships
5. **shopping** - Money, stores, purchases
6. **weather** - Seasons, climate, forecasts
7. **work** - Jobs, professions, office
8. **hobbies** - Free time, leisure activities
9. **health** - Body, medical, wellness
10. **home** - Furniture, rooms, household
11. **time** - Calendar, dates, schedules
12. **colors** - Color names, descriptions
13. **animals** - Pets, wildlife
14. **clothing** - Fashion, apparel
15. **technology** - Computers, internet

## Content Categories

### Lessons (`content/lessons/`)
Full lessons with explanations and examples.

**Available:** All 15 topics

### Vocabulary (`content/vocabulary/`)
Word lists with translations and examples.

**Available:** All 15 topics

### Flashcards (`content/flashcards/`)
Study cards with words and definitions.

**Available:** All 15 topics

### Visual Cards (`content/visual-cards/`)
Image-based flashcards with AI-generated illustrations.

**Available:** All 15 topics

**⚠️ Note:** These files contain base64-encoded images and are large.

### Scenarios (`content/scenarios/`)
Roleplay conversation scripts.

**Available:** 7 topics
- greetings, food, travel, shopping, work, health, home

**Not available:** family, weather, hobbies, time, colors, animals, clothing, technology

## Exercises

### Text Completion (`exercises/text-completion/`)
Fill-in-the-blank exercises.

**Available:** All 15 topics

### Drag & Drop (`exercises/drag-drop/`)
Word ordering/sentence construction.

**Available:** All 15 topics

### Translation (`exercises/translation/`)
Translate sentences between languages.

**Available:** All 15 topics

### Listening ❌
**Not available** - Requires backend audio services

### Listening Comprehension ❌
**Not available** - Requires backend audio services

### Speaking ❌
**Not available** - Requires backend audio services

## Chat

| File | Description |
|------|-------------|
| `chat/greeting.json` | Initial AI coach greeting message |
| `chat/suggestions-vocabulary.json` | Topic suggestions for vocabulary activities |
| `chat/suggestions-exercise.json` | Topic suggestions for exercise activities |
| `chat/suggestions-lesson.json` | Topic suggestions for lesson activities |
| `chat/suggestions-scenario.json` | Topic suggestions for roleplay scenarios |

## I18n

| File | Description |
|------|-------------|
| `i18n/en.json` | English UI translations |
| `i18n/de.json` | German UI translations |

## Feature Availability Matrix

| Feature | Available | Notes |
|---------|-----------|-------|
| Lessons | ✅ Yes | All 15 topics |
| Vocabulary | ✅ Yes | All 15 topics |
| Flashcards | ✅ Yes | All 15 topics |
| Visual Cards | ✅ Yes | All 15 topics (large files) |
| Scenarios | ✅ Partial | 7 topics only |
| Text Completion | ✅ Yes | All 15 topics |
| Drag & Drop | ✅ Yes | All 15 topics |
| Translation | ✅ Yes | All 15 topics |
| Listening | ❌ No | Requires backend |
| Speaking | ❌ No | Requires backend |
| Listening Comprehension | ❌ No | Requires backend |
| Chat | ✅ Limited | Pre-defined responses only |
| Progress Tracking | ❌ No | No persistence |
| Audio/TTS | ❌ No | Requires backend |

## Usage in Code

The demo mode service (`frontend/js/services/demo-mode.js`) uses:

```javascript
// Get available topics
demoMode.getTopics() // Returns array of 15 topic strings

// Check feature availability
demoMode.isFeatureAvailable('listening') // Returns false

// Get categories with topics
demoMode.index.categories.exercises['text-completion'] // Returns topics array