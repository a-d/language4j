# Frontend - Language Learning Platform

A vanilla JavaScript single-page application for the language learning platform.

## 🏗️ Architecture

### No Framework Philosophy

This frontend deliberately avoids frameworks like React, Vue, or Angular. Benefits:
- **Zero build step** - Just open `index.html` in a browser
- **No dependencies** - No `node_modules`, no npm install
- **Lightweight** - Fast load times, small footprint
- **Educational** - Clear, understandable code

### Directory Structure

```
frontend/
├── index.html              # Main HTML file (single page)
├── README.md               # This file
├── css/
│   ├── main.css           # Primary styles and CSS variables
│   └── components/         # Component-specific styles
│       ├── navbar.css     # Navigation bar styles
│       ├── cards.css      # Card component styles
│       └── exercises.css  # Exercise UI styles
├── js/
│   ├── app.js             # Main application logic
│   ├── config.js          # Configuration (API URL, etc.)
│   ├── api/
│   │   └── client.js      # API client for backend communication
│   └── services/
│       ├── router.js      # Hash-based routing
│       └── toast.js       # Toast notification service
└── assets/                # Static assets (images, audio)
```

### Application Flow

```
┌─────────────────────────────────────────────────────────┐
│                    index.html                            │
│  ┌────────────┐  ┌────────────────────────────────────┐ │
│  │   Navbar   │  │          Main Content               │ │
│  │            │  │  ┌────────────────────────────────┐ │ │
│  │ • Dashboard│  │  │         Page Sections          │ │ │
│  │ • Lessons  │  │  │  (dashboard, lessons, vocab,   │ │ │
│  │ • Vocab    │  │  │   exercises, progress)         │ │ │
│  │ • Exercises│  │  └────────────────────────────────┘ │ │
│  │ • Progress │  │                                      │ │
│  └────────────┘  └────────────────────────────────────┘ │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   Modal     │  │   Loading   │  │  Toast Container│  │
│  │  Container  │  │   Overlay   │  │                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Module Responsibilities

| Module | Purpose |
|--------|---------|
| `app.js` | Application initialization, state management, page loading, event handlers |
| `config.js` | Environment configuration (API URLs, feature flags) |
| `api/client.js` | HTTP client for all backend API calls |
| `services/router.js` | Hash-based navigation between pages |
| `services/toast.js` | User notifications (success, error, info) |

### State Management

Simple in-memory state object in `app.js`:

```javascript
const state = {
    user: null,           // Current user data
    currentPage: 'dashboard',
    loading: false,
    currentExercise: null,
    exerciseQuestions: [],
    exerciseIndex: 0
};
```

### Routing

Hash-based routing (`#dashboard`, `#lessons`, etc.):
- No server configuration needed
- Works with file:// protocol
- Browser back/forward supported

```javascript
// Navigate programmatically
navigateTo('lessons');

// Listen for route changes
window.addEventListener('hashchange', handleRoute);
```

---

## 🚀 Development

### Running Locally

**Option 1: Direct file**
```bash
# Just open in browser
open frontend/index.html
```

**Option 2: Local server (recommended for CORS)**
```bash
# Python
cd frontend && python -m http.server 3000

# Node.js
cd frontend && npx serve .
```

### Connecting to Backend

1. Start the backend:
   ```bash
   cd backend && mvn -pl api-module spring-boot:run
   ```

2. The frontend expects the API at `http://localhost:8080/api`

3. Configure in `config.js`:
   ```javascript
   window.APP_CONFIG = {
       API_URL: 'http://localhost:8080/api'
   };
   ```

### Adding New Pages

1. Add HTML section in `index.html`:
   ```html
   <section id="new-page-page" class="page hidden">
       <div class="page-header">
           <h1>New Page</h1>
       </div>
       <!-- Page content -->
   </section>
   ```

2. Add nav link:
   ```html
   <a href="#new-page" class="nav-link" data-page="new-page">New Page</a>
   ```

3. Add data loading in `app.js`:
   ```javascript
   async function loadPageData(pageName) {
       switch (pageName) {
           case 'new-page':
               await loadNewPageData();
               break;
           // ...
       }
   }
   ```

### Adding API Calls

Add methods to `api/client.js`:

```javascript
export const api = {
    // ...existing...
    
    newFeature: {
        list: () => request('/v1/new-feature'),
        create: (data) => request('/v1/new-feature', { method: 'POST', body: data }),
        delete: (id) => request(`/v1/new-feature/${id}`, { method: 'DELETE' })
    }
};
```

### CSS Architecture

CSS uses custom properties (variables) for theming:

```css
:root {
    --primary-color: #4f46e5;
    --bg-color: #f8fafc;
    --spacing-md: 1rem;
    /* ... */
}
```

Component classes follow BEM-like naming:
- `.goal-card` - Block
- `.goal-card-header` - Element
- `.goal-card.completed` - Modifier

---

## 📋 Feature Checklist

### ✅ Implemented Features

#### Core
- [x] Hash-based SPA routing
- [x] API client with error handling
- [x] Toast notifications
- [x] Loading states
- [x] Responsive design
- [x] Offline mode fallback (shows error, continues working)

#### User
- [x] Load current user from API
- [x] Display user info in navbar
- [x] Show language pair (native → target)
- [x] Show skill level (A1-C2)

#### Goals
- [x] Display daily goals on dashboard
- [x] Display all goals on progress page
- [x] Create new goals (via modal)
- [x] Create default goals (quick action)
- [x] Increment goal progress (+1 button)
- [x] Mark goal as complete
- [x] Delete goals
- [x] Progress bars with percentage

#### Content Generation
- [x] Generate lessons by topic
- [x] Generate vocabulary lists
- [x] Generate flashcards
- [x] Generate roleplay scenarios
- [x] Markdown rendering for generated content
- [x] Listen button (TTS) for lessons

#### Exercises
- [x] Text completion (fill-in-the-blank) generation
- [x] Drag-and-drop (word order) generation
- [x] Translation exercise generation
- [x] Exercise display with markdown

#### Speech
- [x] Text-to-speech playback (via backend API)
- [x] Slow speech mode for learning

### ❌ Missing Features (TODO)

#### User Profile
- [ ] Edit user profile modal
- [ ] Change display name
- [ ] Update skill level
- [ ] Settings page

#### Goals
- [ ] Edit existing goals
- [ ] Goal history/archive
- [ ] Weekly/monthly goal views
- [ ] Goal reminders

#### Content
- [ ] Save/bookmark generated lessons
- [ ] Lesson history
- [ ] Favorite vocabulary words
- [ ] Spaced repetition for flashcards
- [ ] Interactive flashcard flip UI

#### Exercises
- [ ] Interactive exercise UI (not just markdown display)
- [ ] Answer submission and validation
- [ ] Score tracking per exercise
- [ ] Exercise history
- [ ] Listening exercises (audio playback)
- [ ] Speaking exercises (audio recording)
- [ ] Drag-and-drop UI implementation

#### Progress
- [ ] Activity timeline/history
- [ ] Learning streaks
- [ ] Statistics charts
- [ ] Export progress data

#### Speech
- [ ] Speech-to-text (recording)
- [ ] Pronunciation feedback
- [ ] Voice selection UI

#### UI/UX
- [ ] Dark mode
- [ ] Language selector (change target language)
- [ ] Keyboard shortcuts
- [ ] Tutorial/onboarding flow
- [ ] Offline support (service worker)

---

## 💡 Future Ideas

### Learning Features
- **Spaced Repetition System (SRS)** - Track word familiarity, schedule reviews
- **Grammar Explanations** - AI-generated grammar tips based on mistakes
- **Conversation Practice** - Chat-like interface with AI tutor
- **Cultural Notes** - Add context about target language culture
- **Idiom of the Day** - Daily idiom with explanation
- **Mini-games** - Word matching, speed vocabulary quiz

### Social Features
- **Progress Sharing** - Share achievements on social media
- **Leaderboards** - Compare with other learners (optional)
- **Study Groups** - Connect learners of same language pair

### Content
- **Pre-built Lesson Packs** - Themed lesson collections (travel, business, etc.)
- **News Reader** - Simplified news in target language
- **Story Mode** - Progressive narrative with vocabulary
- **Podcast Integration** - Listen to content with transcripts

### Technical
- **PWA Support** - Install as app, offline capability
- **WebRTC Speech** - Client-side speech recognition
- **Local Storage** - Cache lessons for offline access
- **Sync Across Devices** - User accounts with cloud sync
- **Analytics Dashboard** - Learning patterns, optimal study times

### Accessibility
- **Screen Reader Support** - ARIA labels throughout
- **High Contrast Mode** - For visual impairment
- **Font Size Controls** - Adjustable text size
- **Keyboard Navigation** - Full app navigation via keyboard

---

## 🔧 Troubleshooting

### CORS Errors
If you see CORS errors in console:
1. Make sure backend is running on `localhost:8080`
2. Check that backend has CORS configured for your origin
3. Use a local server instead of `file://` protocol

### API Errors
If API calls fail:
1. Check browser dev tools Network tab
2. Verify backend is running: `curl http://localhost:8080/api/v1/users/me`
3. Check `config.js` has correct API_URL

### Blank Page
If page doesn't load:
1. Check browser console for JavaScript errors
2. Verify all JS files are loading (no 404s)
3. Check that ES6 modules are supported (modern browser)

---

## 📚 Resources

- [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [ES6 Modules](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules)
- [CSS Custom Properties](https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties)
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API) (for future speech features)