# Demo Mode for Language Learning Platform

Demo mode allows the frontend to function without a backend, using pre-collected sample data. This is useful for:
- Demonstrating the application without running the full backend
- Offline usage when the backend is unavailable
- Quick testing and development

## Overview

Demo mode uses pre-generated content stored as JSON files in `frontend/demo-data/`. When the backend is unavailable (or demo mode is manually enabled), the frontend automatically falls back to this offline data.

## Features in Demo Mode

| Feature | Available | Notes |
|---------|-----------|-------|
| Lessons | ✅ | Pre-generated for 15 topics |
| Vocabulary | ✅ | Pre-generated word lists |
| Flashcards | ✅ | Pre-generated flashcard sets |
| Text Completion Exercises | ✅ | Pre-generated exercises |
| Drag-Drop Exercises | ✅ | Pre-generated exercises |
| Translation Exercises | ✅ | Pre-generated exercises |
| Chat Coach | ✅ | Pre-scripted exchanges |
| Topic Suggestions | ✅ | Pre-loaded suggestions |
| Goals | ✅ | Sample goals (read-only) |
| Listening Exercises | ❌ | Requires TTS service |
| Speaking Exercises | ❌ | Requires TTS/STT services |
| Visual Cards | ⚠️ | Available if collected with images |
| Roleplay Scenarios | ✅ | Pre-generated scenarios |
| Progress Tracking | ❌ | Requires database |
| User Management | ⚠️ | Single demo user only |

## Collecting Demo Data

### Prerequisites
- A running backend instance with configured AI services
- `curl` installed
- Environment variables configured (see below)

### Configuration

Set environment variables before running the collection script:

```bash
# Required
export BACKEND_URL="http://localhost:8080"  # Your running backend

# Optional (defaults shown)
export NATIVE_LANG="en"     # Native language code
export TARGET_LANG="de"     # Target language code
export OUTPUT_DIR="frontend/demo-data"
export DELAY_MS="1000"      # Delay between API calls (rate limiting)
```

### Running the Collection Script

```bash
# From project root - basic collection (no visual cards)
./demo-mode/collect-demo-data.sh

# Or with inline environment variables
BACKEND_URL=http://localhost:8080 NATIVE_LANG=en TARGET_LANG=de ./demo-mode/collect-demo-data.sh

# To include visual cards with base64-embedded images (adds ~15-20MB)
BACKEND_URL=http://localhost:8080 COLLECT_VISUAL_CARDS=true ./demo-mode/collect-demo-data.sh
```

> **Note**: Collecting visual cards requires the image generation service (DALL-E) to be configured. Each topic generates 3 visual cards, and image generation takes longer than other content. Total collection time with visual cards is approximately 15-30 minutes.

### What Gets Collected

The script collects data for 15 topics:
1. Greetings & Introductions
2. Food & Dining
3. Travel & Directions
4. Family & Relationships
5. Shopping & Money
6. Weather & Seasons
7. Work & Professions
8. Hobbies & Free Time
9. Health & Body
10. Home & Furniture
11. Time & Calendar
12. Colors & Numbers
13. Animals
14. Clothing
15. Technology

For each topic:
- 1 Lesson (markdown content)
- 15 Vocabulary words
- 10 Flashcards
- 5 Text Completion exercises
- 5 Drag-Drop exercises
- 5 Translation exercises
- 1 Roleplay scenario (for applicable topics)

Additionally:
- I18n translations (en, de)
- Chat topic suggestions per category
- Sample chat greeting
- Sample goals

## Demo Data Structure

```
frontend/demo-data/
├── index.json                 # Manifest of available data
├── config.json                # Demo configuration
├── i18n/
│   ├── en.json               # English translations
│   └── de.json               # German translations
├── user.json                  # Demo user profile
├── goals.json                 # Sample goals
├── content/
│   ├── lessons/
│   │   ├── greetings.json
│   │   └── ...
│   ├── vocabulary/
│   │   └── ...
│   ├── flashcards/
│   │   └── ...
│   ├── visual-cards/        # Optional - if COLLECT_VISUAL_CARDS=true
│   │   └── ...              # Contains base64-embedded images
│   └── scenarios/
│       └── ...
├── exercises/
│   ├── text-completion/
│   │   └── ...
│   ├── drag-drop/
│   │   └── ...
│   └── translation/
│       └── ...
└── chat/
    ├── greeting.json
    └── topic-suggestions.json
```

## Frontend Integration

### Automatic Fallback

The frontend automatically enables demo mode when:
1. The backend is unreachable (network error)
2. The backend returns a connection error

### Manual Toggle

Demo mode can be manually enabled/disabled in Settings:
- Settings → Demo Mode → Toggle

Or via browser console:
```javascript
// Enable demo mode
localStorage.setItem('llp_demo_mode', 'true');
location.reload();

// Disable demo mode
localStorage.removeItem('llp_demo_mode');
location.reload();
```

### Demo Mode Indicator

When demo mode is active:
- A banner appears at the top of the page
- Unsupported features are hidden from the UI
- A badge appears in the navigation

## Limitations

1. **No data persistence**: Progress, goals, and exercise results are not saved
2. **No audio features**: Listening and speaking exercises are disabled
3. **Visual cards**: Available only if `COLLECT_VISUAL_CARDS=true` was used during collection
4. **Single user**: Only the demo user profile is available
5. **Fixed content**: Content is pre-generated and cannot be customized
6. **No real-time chat**: Chat responses are pre-scripted

## Extending Demo Data

To add new topics or update existing content:

1. Ensure the backend is running with AI services configured
2. Run the collection script with the new topic:
   ```bash
   TOPICS="new-topic" ./demo-mode/collect-demo-data.sh
   ```
3. Or modify `TOPICS` array in the script and re-run

## Troubleshooting

### Demo mode not activating
- Check if `frontend/demo-data/index.json` exists
- Verify the JSON files are valid
- Check browser console for errors

### Missing content for a topic
- Re-run the collection script for that topic
- Check if the backend returned an error during collection

### Translations not loading
- Ensure `frontend/demo-data/i18n/en.json` and `de.json` exist
- Check that the files are valid JSON