# Learning Activities Reference

This document lists all learning activities supported by the Language Learning Platform, including how they can be accessed (via chat or direct navigation) and their data formats.

## Overview

The platform supports **14 activity types** across different learning modalities:
- **Vocabulary & Review**: Basic word learning and memorization
- **Interactive Exercises**: Fill-in-blank, translation, word order
- **Game-based Learning**: Pair matching, memory game
- **Audio/Speech**: Listening comprehension, pronunciation practice
- **Rich Content**: Lessons, scenarios, visual cards

---

## Activity Types

### 1. VOCABULARY
**Description**: Word list with translations, pronunciations, examples, and usage notes.

**Access Methods**:
- ✅ Chat: "Practice vocabulary about [topic]"
- ✅ Navigation: Vocabulary page

**Trigger Phrases** (Chat):
- "Practice vocabulary"
- "Learn words about..."
- "Vocabulary for..."

**Data Format**:
```json
{
  "vocabulary": [
    {
      "word": "Bonjour",
      "pronunciation": "/bɔ̃.ʒuʁ/",
      "translation": "Hello",
      "partOfSpeech": "interjection",
      "example": "Bonjour, comment allez-vous?",
      "exampleTranslation": "Hello, how are you?",
      "usageNote": "Used in formal greetings"
    }
  ]
}
```

---

### 2. FLASHCARDS
**Description**: Interactive flip cards for memorization and review.

**Access Methods**:
- ✅ Chat: "Review with flashcards"
- ✅ Navigation: Cards page

**Trigger Phrases** (Chat):
- "Review"
- "Flashcards"
- "Practice with cards"

**UI Features**:
- Flip animation to reveal translation
- Previous/Next navigation
- Card counter

---

### 3. VISUAL_CARDS
**Description**: Flashcards enhanced with AI-generated images for visual learners.

**Access Methods**:
- ✅ Chat: "Visual learning about [topic]"
- ✅ Navigation: Cards page (visual mode)

**Trigger Phrases** (Chat):
- "Visual learning"
- "With images"
- "Picture cards"

**Note**: Uses DALL-E for image generation. Requires OpenAI API key with image access.

---

### 4. TEXT_COMPLETION
**Description**: Fill-in-the-blank exercises for grammar and vocabulary practice.

**Access Methods**:
- ✅ Chat: "Do exercises about [topic]"
- ✅ Navigation: Exercises page

**Trigger Phrases** (Chat):
- "Exercises"
- "Fill in the blank"
- "Practice grammar"

**Data Format**:
```json
{
  "exercises": [
    {
      "sentence": "Je ___ au cinéma hier soir.",
      "correctAnswer": "suis allé",
      "wordBank": ["suis allé", "ai allé", "étais allé"],
      "hint": "Past tense - être + aller"
    }
  ]
}
```

**UI Features**:
- Clickable word bank
- Lenient answer checking (ignores case, accents)
- Show/hide solution toggle

---

### 5. DRAG_DROP
**Description**: Word ordering exercises for sentence structure practice.

**Access Methods**:
- ✅ Chat: "Word order exercises"
- ✅ Navigation: Exercises page

**Trigger Phrases** (Chat):
- "Word order"
- "Sentence building"
- "Arrange words"

**Data Format**:
```json
{
  "exercises": [
    {
      "words": ["Je", "suis", "allé", "au", "marché"],
      "translation": "I went to the market",
      "explanation": "Subject + verb + location"
    }
  ]
}
```

**UI Features**:
- Click-to-select word placement
- Clear button to reset
- Grammar hints after checking

---

### 6. TRANSLATION
**Description**: Translate sentences between native and target language.

**Access Methods**:
- ✅ Chat: "Translation exercises"
- ✅ Navigation: Exercises page

**Trigger Phrases** (Chat):
- "Translation"
- "Translate"

**Data Format**:
```json
{
  "exercises": [
    {
      "sourceText": "I would like a coffee, please.",
      "modelAnswer": "Je voudrais un café, s'il vous plaît.",
      "alternatives": ["J'aimerais un café, s'il vous plaît."],
      "keyPoints": ["polite form", "article usage"]
    }
  ]
}
```

**UI Features**:
- Textarea for free-form input
- Accepts alternative correct answers
- Similarity-based partial credit

---

### 7. LISTENING
**Description**: Listen to audio and transcribe what you hear.

**Access Methods**:
- ✅ Chat: "Listening practice"
- ✅ Navigation: Exercises page (listening tab)

**Trigger Phrases** (Chat):
- "Listening"
- "Comprehension"
- "Audio exercises"

**Data Format**:
```json
{
  "exercises": [
    {
      "sentence": "Bonjour, comment ça va?",
      "translation": "Hello, how are you?"
    }
  ]
}
```

**UI Features**:
- Play audio button (normal speed)
- Slow playback option
- Translation hint

---

### 8. SPEAKING
**Description**: Pronunciation practice with model audio and recording.

**Access Methods**:
- ✅ Chat: "Speaking practice"
- ✅ Navigation: Exercises page (speaking tab)

**Trigger Phrases** (Chat):
- "Speaking"
- "Pronunciation"
- "Practice speaking"

**Data Format**:
```json
{
  "exercises": [
    {
      "text": "Je voudrais un croissant, s'il vous plaît.",
      "translation": "I would like a croissant, please.",
      "pronunciationTips": "Silent 't' at the end of 'voudrais'",
      "commonMistakes": "Pronouncing the final consonants"
    }
  ]
}
```

**UI Features**:
- Listen to model pronunciation
- Record button (browser microphone)
- Pronunciation tips and common mistakes

---

### 9. PAIR_MATCHING ⭐ NEW
**Description**: Two-column matching exercise. Connect words with their translations.

**Access Methods**:
- ✅ Chat: "Matching game about [topic]"
- ✅ Navigation: Exercises page

**Trigger Phrases** (Chat):
- "Matching"
- "Pair matching"
- "Connect words"
- "Match the pairs"

**Data Format**: Uses same format as VOCABULARY

**UI Features**:
- Two columns: native language (left), target language (right)
- Click one word from each column to match
- Correct matches highlighted in green
- Incorrect matches shake briefly
- Progress counter

---

### 10. MEMORY_GAME ⭐ NEW
**Description**: Card-flipping memory game to find word-translation pairs.

**Access Methods**:
- ✅ Chat: "Memory game about [topic]"
- ✅ Navigation: Exercises page

**Trigger Phrases** (Chat):
- "Memory game"
- "Memory"
- "Card game"

**Data Format**: Uses same format as VOCABULARY

**UI Features**:
- Grid of face-down cards (4x4 for 8 pairs)
- Flip animation on click
- Match detection (same pair, different types)
- Attempt counter
- Efficiency score at completion

---

### 11. LESSON
**Description**: Full lesson content in Markdown format, including explanations, examples, and exercises.

**Access Methods**:
- ✅ Chat: "Start a lesson about [topic]"
- ✅ Navigation: Lessons page

**Trigger Phrases** (Chat):
- "Lesson"
- "Learn about"
- "Teach me"

**Content**: Markdown formatted lesson with:
- Introduction and learning objectives
- Grammar explanations
- Example sentences with translations
- Cultural notes
- Practice exercises

---

### 12. SCENARIO
**Description**: Roleplay conversation scenarios for real-world practice.

**Access Methods**:
- ✅ Chat: "Roleplay about [situation]"
- ✅ Navigation: Lessons page (scenarios)

**Trigger Phrases** (Chat):
- "Roleplay"
- "Conversation practice"
- "Dialogue"
- "Scenario"

**Content**: Markdown dialogue with:
- Setting description
- Character roles
- Conversation flow with translations
- Useful phrases
- Cultural tips

---

### 13. LEARNING_PLAN
**Description**: Personalized learning plan based on goals and level.

**Access Methods**:
- ⚠️ Chat: Not yet integrated
- ✅ Navigation: Dashboard / Learning Plan page

**Content**: Structured plan with:
- Daily/weekly/monthly goals
- Recommended activities
- Progress milestones

---

### 14. EVALUATION
**Description**: Feedback on user responses with corrections.

**Access Methods**:
- 🔄 Internal: Generated when user submits exercise answers
- ❌ Not directly triggerable

**Data Format**:
```json
{
  "correct": false,
  "score": 75,
  "feedback": "Almost correct! Watch the verb conjugation.",
  "corrections": ["Use 'suis' instead of 'ai' with aller"]
}
```

---

## Chat Integration

### How to Trigger Activities in Chat

The chat moderator recognizes natural language requests and automatically generates appropriate activities:

| Request | Activity Generated |
|---------|-------------------|
| "I want to practice vocabulary about food" | VOCABULARY |
| "Let's play a matching game" | PAIR_MATCHING |
| "Can we do a memory game with colors?" | MEMORY_GAME |
| "I want to practice speaking" | SPEAKING |
| "Let's try some translation exercises" | TRANSLATION |
| "Teach me about past tense" | LESSON |
| "Let's roleplay ordering at a restaurant" | SCENARIO |

### Activity Tag Format

The LLM uses this format to embed activities:
```
[ACTIVITY:TYPE:TOPIC]
```

Examples:
- `[ACTIVITY:VOCABULARY:kitchen items]`
- `[ACTIVITY:PAIR_MATCHING:colors]`
- `[ACTIVITY:MEMORY_GAME:animals]`
- `[ACTIVITY:SCENARIO:ordering food at a restaurant]`

---

## Navigation Pages

| Page | Activities Available |
|------|---------------------|
| **Chat** | All activities via natural language |
| **Dashboard** | Goals overview, quick actions |
| **Lessons** | LESSON, SCENARIO |
| **Vocabulary** | VOCABULARY |
| **Cards** | FLASHCARDS, VISUAL_CARDS |
| **Exercises** | TEXT_COMPLETION, DRAG_DROP, TRANSLATION, PAIR_MATCHING, MEMORY_GAME, LISTENING, SPEAKING |
| **Progress** | Statistics and history |

---

## Technical Details

### Content Types (Frontend)
```javascript
ContentType = {
    LESSON: 'lesson',
    VOCABULARY: 'vocabulary',
    FLASHCARDS: 'flashcards',
    SCENARIO: 'scenario',
    LEARNING_PLAN: 'learning-plan',
    TEXT_COMPLETION: 'text-completion',
    DRAG_DROP: 'drag-drop',
    TRANSLATION: 'translation',
    LISTENING: 'listening',
    SPEAKING: 'speaking',
    EVALUATION: 'evaluation',
    PRONUNCIATION_EVALUATION: 'pronunciation-evaluation',
    PAIR_MATCHING: 'pair-matching',
    MEMORY_GAME: 'memory-game'
}
```

### Activity Types (Backend)
```java
enum EmbeddedActivityType {
    VOCABULARY,
    FLASHCARDS,
    VISUAL_CARDS,
    TEXT_COMPLETION,
    DRAG_DROP,
    TRANSLATION,
    LISTENING,
    SPEAKING,
    LESSON,
    SCENARIO,
    LEARNING_PLAN,
    SUMMARY,
    PAIR_MATCHING,
    MEMORY_GAME
}
```

### Rendering Pipeline
1. Backend generates activity content (JSON or Markdown)
2. Content is stored with message in database
3. Frontend receives activity type + content
4. `content-renderer.js` maps type to appropriate renderer
5. Renderer outputs interactive HTML

---

## Proposed Activities (Not Yet Implemented)

The following activities are planned for future implementation. Each includes a feasibility rating based on:
- **Complexity**: Frontend + Backend implementation effort
- **LLM Dependency**: How much it relies on quality LLM output
- **Infrastructure**: Additional services required (audio, images, etc.)

### Feasibility Rating Scale
- ⭐⭐⭐⭐⭐ **Very Easy** - Simple UI, reuses existing data formats
- ⭐⭐⭐⭐ **Easy** - Moderate UI, straightforward LLM prompt
- ⭐⭐⭐ **Medium** - Custom UI or complex LLM interaction
- ⭐⭐ **Hard** - Requires new infrastructure or complex state
- ⭐ **Very Hard** - Significant new features or external services

---

### 15. WORD_SCRAMBLE 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐⭐ Very Easy

**Description**: Unscramble letters to form the correct word.

**Proposed Trigger Phrases**:
- "Scramble game"
- "Unscramble words"
- "Word puzzle"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "scrambled": "OJNOUBR",
      "answer": "BONJOUR",
      "hint": "A greeting"
    }
  ]
}
```

**Implementation Notes**:
- Simple text input validation
- Can reuse vocabulary data (just scramble on frontend or backend)
- Minimal UI: show scrambled letters, text input, check button

---

### 16. HANGMAN 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐ Easy

**Description**: Classic word guessing game with limited attempts.

**Proposed Trigger Phrases**:
- "Hangman"
- "Guess the word"
- "Word guessing game"

**Proposed Data Format**:
```json
{
  "word": "BONJOUR",
  "hint": "A common greeting",
  "maxAttempts": 6
}
```

**Implementation Notes**:
- Frontend tracks guessed letters and remaining attempts
- Simple state machine (correct letter, wrong letter, win, lose)
- Could show visual hangman or just lives counter
- Good for vocabulary reinforcement

---

### 17. CROSSWORD_MINI 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐ Hard

**Description**: Small crossword puzzle (5-6 words) with translations as clues.

**Proposed Trigger Phrases**:
- "Crossword"
- "Word puzzle"
- "Crossword puzzle"

**Proposed Data Format**:
```json
{
  "size": { "rows": 8, "cols": 8 },
  "words": [
    { "word": "BONJOUR", "row": 0, "col": 0, "direction": "across", "clue": "Hello" },
    { "word": "MERCI", "row": 0, "col": 0, "direction": "down", "clue": "Thank you" }
  ]
}
```

**Implementation Notes**:
- Complex: LLM must generate valid crossword layout
- Grid rendering and input handling is non-trivial
- Consider using a crossword generation library
- Alternatively: pre-compute layouts, LLM fills with vocabulary

---

### 18. WORD_CHAIN 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Each word must start with the last letter of the previous word.

**Proposed Trigger Phrases**:
- "Word chain"
- "Last letter game"
- "Chain words"

**Proposed Data Format**:
```json
{
  "startWord": "POMME",
  "validWords": ["ELEPHANT", "TABLE", "EAU", ...],
  "topic": "general vocabulary"
}
```

**Implementation Notes**:
- Interactive: user and coach take turns
- Requires vocabulary validation (is the word real? is it in topic?)
- Could use dictionary API or LLM validation
- State management for turn-based play

---

### 19. READING_COMPREHENSION 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Read a passage and answer comprehension questions.

**Proposed Trigger Phrases**:
- "Reading practice"
- "Comprehension exercise"
- "Read and answer"

**Proposed Data Format**:
```json
{
  "passage": "Jean va au marché...",
  "passageTranslation": "Jean goes to the market...",
  "questions": [
    {
      "question": "Où va Jean?",
      "type": "multiple_choice",
      "options": ["Au marché", "À l'école", "Au parc"],
      "correctAnswer": "Au marché"
    }
  ]
}
```

**Implementation Notes**:
- LLM generates passage + questions
- Multiple question types (MC, short answer, true/false)
- Audio playback option for the passage
- Good for intermediate+ learners

---

### 20. FILL_THE_GAP_AUDIO 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Listen to audio with a missing word (beep) and fill in the gap.

**Proposed Trigger Phrases**:
- "Audio gap fill"
- "Listen and complete"
- "Fill the blank audio"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "fullSentence": "Je voudrais un café, s'il vous plaît.",
      "gapWord": "café",
      "gapPosition": 3,
      "hint": "A hot drink"
    }
  ]
}
```

**Implementation Notes**:
- Requires audio generation with strategic pause/beep
- More complex than regular listening (need to splice audio)
- Could generate audio with placeholder word, then re-synthesize
- Frontend plays audio, user types missing word

---

### 21. DICTATION 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐ Easy

**Description**: Listen to full sentences/paragraphs and write them down.

**Proposed Trigger Phrases**:
- "Dictation"
- "Write what you hear"
- "Spelling practice"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "text": "Bonjour, je m'appelle Marie.",
      "translation": "Hello, my name is Marie."
    }
  ]
}
```

**Implementation Notes**:
- Similar to LISTENING but longer text
- Multiple playback speeds
- Lenient checking (ignore minor punctuation differences)
- Highlight differences in feedback

---

### 22. SENTENCE_CORRECTION 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐ Easy

**Description**: Find and fix grammatical or spelling errors in a sentence.

**Proposed Trigger Phrases**:
- "Correct the sentence"
- "Find the error"
- "Fix the mistake"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "incorrectSentence": "Je suis allé au cinéma hier.",
      "errorType": "verb agreement",
      "correctSentence": "Je suis allée au cinéma hier.",
      "explanation": "With 'être', past participle agrees with subject (feminine)"
    }
  ]
}
```

**Implementation Notes**:
- Highlight or underline the error
- User types correction
- LLM provides context-aware explanation
- Great for grammar focus

---

### 23. CONJUGATION_DRILL 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐⭐ Very Easy

**Description**: Rapid-fire verb conjugation practice.

**Proposed Trigger Phrases**:
- "Conjugation practice"
- "Verb drill"
- "Conjugate verbs"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "verb": "aller",
      "pronoun": "je",
      "tense": "présent",
      "correctAnswer": "vais"
    }
  ]
}
```

**Implementation Notes**:
- Fast-paced: show prompt, user types answer
- Optional timer for speed drills
- Track accuracy and speed
- Can focus on specific tenses or verbs

---

### 24. ARTICLE_PRACTICE 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐⭐ Very Easy

**Description**: Practice noun genders by selecting correct articles (der/die/das, le/la, etc.)

**Proposed Trigger Phrases**:
- "Article practice"
- "Gender practice"
- "Der, die, das"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "word": "Tisch",
      "translation": "table",
      "options": ["der", "die", "das"],
      "correctAnswer": "der"
    }
  ]
}
```

**Implementation Notes**:
- Multiple choice buttons
- Instant feedback
- Track common mistakes
- Language-specific (German, French, Spanish, etc.)

---

### 25. QUICK_RESPONSE 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Answer a simple question in target language naturally.

**Proposed Trigger Phrases**:
- "Quick response"
- "Answer questions"
- "Conversation practice"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "question": "Comment t'appelles-tu?",
      "questionTranslation": "What is your name?",
      "expectedType": "personal_info",
      "sampleAnswers": ["Je m'appelle...", "Mon nom est..."]
    }
  ]
}
```

**Implementation Notes**:
- Open-ended: LLM evaluates if response is appropriate
- Not about exact match, but communication
- Good for conversational readiness
- Could use speech input too

---

### 26. PICTURE_DESCRIPTION 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐ Hard

**Description**: Describe an image in target language.

**Proposed Trigger Phrases**:
- "Describe the picture"
- "Image description"
- "What do you see?"

**Proposed Data Format**:
```json
{
  "imageUrl": "https://...",
  "imagePrompt": "A family having dinner",
  "suggestedVocabulary": ["famille", "dîner", "table"],
  "minWords": 20
}
```

**Implementation Notes**:
- Requires image generation (DALL-E) or stock images
- LLM evaluates description quality
- Open-ended scoring based on vocabulary usage
- Good for advanced learners

---

### 27. STORY_CONTINUATION 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Continue a story with one sentence at a time, collaboratively with the coach.

**Proposed Trigger Phrases**:
- "Write a story"
- "Story time"
- "Continue the story"

**Proposed Data Format**:
```json
{
  "starter": "Il était une fois une petite fille qui...",
  "starterTranslation": "Once upon a time there was a little girl who...",
  "topic": "fairy tale",
  "level": "A2"
}
```

**Implementation Notes**:
- Turn-based: coach and user alternate sentences
- LLM validates grammar and coherence
- Creative writing practice
- Track story progression

---

### 28. TIMED_CHALLENGE 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐ Easy

**Description**: Complete as many exercises as possible within a time limit.

**Proposed Trigger Phrases**:
- "Timed challenge"
- "Speed drill"
- "Beat the clock"

**Proposed Data Format**:
```json
{
  "timeLimit": 60,
  "exerciseType": "vocabulary",
  "exercises": [...],
  "scoring": {
    "correct": 10,
    "streak_bonus": 5
  }
}
```

**Implementation Notes**:
- Wrapper around existing exercise types
- Timer countdown display
- Streak multiplier for consecutive correct answers
- Leaderboard potential

---

### 29. MULTIPLE_CHOICE 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐⭐⭐ Very Easy

**Description**: Classic multiple choice questions for vocabulary or grammar.

**Proposed Trigger Phrases**:
- "Quiz"
- "Multiple choice"
- "Test me"

**Proposed Data Format**:
```json
{
  "exercises": [
    {
      "question": "What does 'Bonjour' mean?",
      "options": ["Hello", "Goodbye", "Thank you", "Please"],
      "correctAnswer": "Hello",
      "explanation": "Bonjour is a common French greeting"
    }
  ]
}
```

**Implementation Notes**:
- Simple radio button selection
- Instant feedback with explanation
- Can test vocabulary, grammar, or comprehension
- Foundation for quizzes

---

### 30. DAILY_QUIZ 🚧
**Status**: Not Implemented  
**Feasibility**: ⭐⭐⭐ Medium

**Description**: Automatically generated quiz from recently learned vocabulary.

**Proposed Trigger Phrases**:
- "Daily quiz"
- "Review quiz"
- "Test my knowledge"

**Proposed Data Format**:
```json
{
  "date": "2024-01-15",
  "wordsReviewed": 10,
  "exercises": [...],
  "spaced_repetition": true
}
```

**Implementation Notes**:
- Auto-generated from user's vocabulary history
- Spaced repetition algorithm for word selection
- Mixed exercise types
- Tracks long-term retention

---

## Summary: Implementation Priority

Based on feasibility and learning value:

### Quick Wins (Implement First)
| Activity | Feasibility | Notes |
|----------|-------------|-------|
| WORD_SCRAMBLE | ⭐⭐⭐⭐⭐ | Reuses vocabulary, simple UI |
| CONJUGATION_DRILL | ⭐⭐⭐⭐⭐ | Great for grammar, fast-paced |
| MULTIPLE_CHOICE | ⭐⭐⭐⭐⭐ | Foundation for quizzes |
| ARTICLE_PRACTICE | ⭐⭐⭐⭐⭐ | Language-specific, simple |
| SENTENCE_CORRECTION | ⭐⭐⭐⭐ | Good LLM fit, grammar focus |
| DICTATION | ⭐⭐⭐⭐ | Extends LISTENING, easy |
| HANGMAN | ⭐⭐⭐⭐ | Fun, engaging |

### Medium Effort
| Activity | Feasibility | Notes |
|----------|-------------|-------|
| TIMED_CHALLENGE | ⭐⭐⭐⭐ | Wrapper, adds gamification |
| READING_COMPREHENSION | ⭐⭐⭐ | Good for intermediate+ |
| WORD_CHAIN | ⭐⭐⭐ | Turn-based, interactive |
| QUICK_RESPONSE | ⭐⭐⭐ | Open-ended, LLM evaluates |
| FILL_THE_GAP_AUDIO | ⭐⭐⭐ | Audio manipulation needed |
| STORY_CONTINUATION | ⭐⭐⭐ | Creative, turn-based |
| DAILY_QUIZ | ⭐⭐⭐ | Needs vocabulary history |

### Significant Effort
| Activity | Feasibility | Notes |
|----------|-------------|-------|
| CROSSWORD_MINI | ⭐⭐ | Complex layout generation |
| PICTURE_DESCRIPTION | ⭐⭐ | Needs image generation |
