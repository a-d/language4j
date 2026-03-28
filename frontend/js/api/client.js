/**
 * API Client for Language Learning Platform
 * ==========================================
 * Handles all HTTP requests to the backend API.
 * 
 * Multi-user support: The client includes the X-User-Id header with all
 * requests, identifying the currently selected user. The user ID is stored
 * in localStorage and can be changed via the user selector.
 * 
 * Demo Mode: When the backend is unavailable or demo mode is manually enabled,
 * the client automatically falls back to using pre-generated offline data.
 * @see frontend/js/services/demo-mode.js
 */

import { demoMode } from '../services/demo-mode.js';

const API_BASE = window.APP_CONFIG?.API_URL || '/api';
const USER_ID_STORAGE_KEY = 'llp_selected_user_id';

/** Track consecutive network failures for auto-fallback */
let consecutiveFailures = 0;
const FALLBACK_THRESHOLD = 2;

/**
 * Get the currently selected user ID from localStorage
 * @returns {string|null} The user ID or null if not set
 */
function getSelectedUserId() {
    return localStorage.getItem(USER_ID_STORAGE_KEY);
}

/**
 * Set the currently selected user ID in localStorage
 * @param {string|null} userId - The user ID to set, or null to clear
 */
function setSelectedUserId(userId) {
    if (userId) {
        localStorage.setItem(USER_ID_STORAGE_KEY, userId);
    } else {
        localStorage.removeItem(USER_ID_STORAGE_KEY);
    }
}

/**
 * Clear the selected user ID from localStorage
 */
function clearSelectedUserId() {
    localStorage.removeItem(USER_ID_STORAGE_KEY);
}

/**
 * Check if error should trigger demo mode fallback
 */
function shouldFallbackToDemo(error) {
    // Network errors (fetch failed)
    if (error.status === 0) return true;
    if (error.message?.includes('Failed to fetch')) return true;
    if (error.message?.includes('NetworkError')) return true;
    if (error.message?.includes('Network error')) return true;
    // Connection refused
    if (error.message?.includes('ECONNREFUSED')) return true;
    return false;
}

/**
 * Make an HTTP request to the API
 * Falls back to demo mode when backend is unavailable
 */
async function request(endpoint, options = {}) {
    // If demo mode is already enabled, use demo handler
    if (demoMode.isEnabled()) {
        return demoMode.handleRequest(endpoint, options);
    }
    
    const url = `${API_BASE}${endpoint}`;
    
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    // Add X-User-Id header if a user is selected
    const userId = getSelectedUserId();
    if (userId) {
        headers['X-User-Id'] = userId;
    }
    
    const config = {
        headers,
        ...options
    };
    
    if (options.body && typeof options.body === 'object') {
        config.body = JSON.stringify(options.body);
    }
    
    try {
        const response = await fetch(url, config);
        
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new ApiError(
                error.message || `HTTP ${response.status}`,
                response.status,
                error
            );
        }
        
        // Success - reset failure counter
        consecutiveFailures = 0;
        
        // Handle empty responses (204 No Content)
        if (response.status === 204) {
            return null;
        }
        
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        const apiError = error instanceof ApiError ? error : new ApiError(error.message || 'Network error', 0);
        
        // Check if we should fallback to demo mode
        if (shouldFallbackToDemo(apiError) && demoMode.hasData()) {
            consecutiveFailures++;
            
            if (consecutiveFailures >= FALLBACK_THRESHOLD) {
                console.warn('Backend unreachable, enabling demo mode');
                demoMode.enable();
                return demoMode.handleRequest(endpoint, options);
            }
        }
        
        throw apiError;
    }
}

/**
 * Custom API Error class
 */
class ApiError extends Error {
    constructor(message, status, details = {}) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.details = details;
    }
}

/**
 * API client with organized endpoints
 * 
 * Backend API structure:
 * - /api/v1/users      - User management
 * - /api/v1/goals      - Learning goals
 * - /api/v1/content    - Content generation (lessons, vocabulary, flashcards, scenarios, learning-plan)
 * - /api/v1/exercises  - Exercise generation and evaluation
 */
export const api = {
    // ==================== Users ====================
    users: {
        /** List all users */
        list: () => request('/v1/users'),
        
        /** Create a new user */
        create: (data) => request('/v1/users', { method: 'POST', body: data }),
        
        /** Get a user by ID */
        getById: (userId) => request(`/v1/users/${userId}`),
        
        /** Get current user profile (based on X-User-Id header) */
        getCurrent: () => request('/v1/users/me'),
        
        /** Update current user profile */
        update: (data) => request('/v1/users/me', { method: 'PUT', body: data }),
        
        /** Update a user by ID */
        updateById: (userId, data) => request(`/v1/users/${userId}`, { method: 'PUT', body: data }),
        
        /** Delete a user by ID */
        delete: (userId) => request(`/v1/users/${userId}`, { method: 'DELETE' }),
        
        /** Check if any users exist */
        checkExists: () => request('/v1/users/exists'),
        
        /** Get the currently selected user ID from localStorage */
        getSelectedId: getSelectedUserId,
        
        /** Set the selected user ID in localStorage */
        setSelectedId: setSelectedUserId,
        
        /** Clear the selected user ID */
        clearSelectedId: clearSelectedUserId
    },
    
    // ==================== Learning Goals ====================
    goals: {
        /** List all goals, optionally filtered by type (DAILY, WEEKLY, MONTHLY, YEARLY) */
        list: (type) => request(`/v1/goals${type ? `?type=${type}` : ''}`),
        
        /** Create a new goal */
        create: (data) => request('/v1/goals', { method: 'POST', body: data }),
        
        /** Get active daily goals (incomplete) */
        getActiveDailyGoals: () => request('/v1/goals/daily/active'),
        
        /** Update goal progress (set value) */
        updateProgress: (id, currentValue) => request(`/v1/goals/${id}/progress`, { 
            method: 'PATCH', 
            body: { currentValue } 
        }),
        
        /** Increment goal progress by amount */
        incrementProgress: (id, amount = 1) => request(`/v1/goals/${id}/increment?amount=${amount}`, { 
            method: 'PATCH'
        }),
        
        /** Mark goal as complete */
        complete: (id) => request(`/v1/goals/${id}/complete`, { method: 'POST' }),
        
        /** Delete a goal */
        delete: (id) => request(`/v1/goals/${id}`, { method: 'DELETE' })
    },
    
    // ==================== Content Generation ====================
    content: {
        /** Generate a lesson on a topic */
        generateLesson: (topic) => request('/v1/content/lessons/generate', { 
            method: 'POST', 
            body: { topic } 
        }),
        
        /** Generate vocabulary for a topic */
        generateVocabulary: (topic, wordCount = 10) => request('/v1/content/vocabulary/generate', { 
            method: 'POST', 
            body: { topic, wordCount } 
        }),
        
        /** Generate flashcards for a topic */
        generateFlashcards: (topic, cardCount = 10) => request('/v1/content/flashcards/generate', { 
            method: 'POST', 
            body: { topic, cardCount } 
        }),
        
        /** Generate a roleplay scenario */
        generateScenario: (scenario) => request('/v1/content/scenarios/generate', { 
            method: 'POST', 
            body: { scenario } 
        }),
        
        /** Generate a personalized learning plan */
        generateLearningPlan: (dailyGoal, weeklyGoal, monthlyGoal) => request('/v1/content/learning-plan/generate', { 
            method: 'POST', 
            body: { dailyGoal, weeklyGoal, monthlyGoal } 
        }),
        
        /**
         * Generate visual learning cards from a topic
         * @param {string} topic - Topic for vocabulary generation (e.g., "Kitchen items", "Animals")
         * @param {number} cardCount - Number of cards to generate (1-10, default 5)
         * @returns {Promise<{topic: string, nativeLanguage: string, targetLanguage: string, cards: Array, cardCount: number, failedCount: number}>}
         */
        generateVisualCards: (topic, cardCount = 5) => request('/v1/content/visual-cards/generate', {
            method: 'POST',
            body: { topic, cardCount }
        })
    },
    
    // ==================== Exercises ====================
    exercises: {
        /**
         * Unified exercise generation API
         * 
         * Generates exercises of any supported type using a single endpoint.
         * 
         * @param {string} type - Exercise type: TEXT_COMPLETION, DRAG_DROP, TRANSLATION, 
         *                        LISTENING, LISTENING_COMPREHENSION, SPEAKING
         * @param {string} topic - Topic for exercise generation
         * @param {number} [count] - Number of exercises (uses type default if not specified)
         * @param {Object} [options] - Type-specific options (e.g., {wordCount, statementCount} for LISTENING_COMPREHENSION)
         * @returns {Promise<{content: string, type: string}>} Generated exercises in JSON format
         * 
         * @example
         * // Simple text completion
         * api.exercises.generate('TEXT_COMPLETION', 'past tense verbs')
         * 
         * // Translation with custom count
         * api.exercises.generate('TRANSLATION', 'restaurant phrases', 10)
         * 
         * // Listening comprehension with options
         * api.exercises.generate('LISTENING_COMPREHENSION', 'daily routines', 1, { wordCount: 150, statementCount: 6 })
         */
        generate: (type, topic, count = null, options = null) => {
            const body = { type, topic };
            if (count !== null) body.count = count;
            if (options !== null) body.options = options;
            return request('/v1/exercises/generate', { 
                method: 'POST', 
                body 
            });
        },
        
        /** Evaluate a user's exercise response */
        evaluate: (exercise, userResponse, expectedAnswer) => request('/v1/exercises/evaluate', { 
            method: 'POST', 
            body: { exercise, userResponse, expectedAnswer } 
        }),
        
        /** Evaluate pronunciation by comparing expected text with transcription */
        evaluatePronunciation: (expectedText, transcription) => request('/v1/exercises/evaluate-pronunciation', { 
            method: 'POST', 
            body: { expectedText, transcription } 
        }),
        
        /** 
         * Save an exercise result
         * @param {Object} result - The exercise result to save
         * @param {string} result.exerciseType - Type (TEXT_COMPLETION, DRAG_DROP, TRANSLATION, etc.)
         * @param {string} [result.exerciseReference] - Optional reference to exercise content
         * @param {number} result.score - Score achieved (0-100)
         * @param {number} result.correctAnswers - Number of correct answers
         * @param {number} result.totalQuestions - Total number of questions
         * @param {number} result.timeSpentSeconds - Time spent in seconds
         * @param {string} [result.userResponse] - User's responses (JSON string)
         * @param {string} [result.correctResponse] - Correct responses (JSON string)
         * @param {string} [result.feedback] - Optional feedback
         * @returns {Promise<Object>} Saved exercise result
         */
        saveResult: (result) => request('/v1/exercises/results', {
            method: 'POST',
            body: result
        }),
        
        /**
         * Get exercise history for the current user
         * @param {Object} options - Query options
         * @param {string} [options.type] - Filter by exercise type
         * @param {number} [options.page=0] - Page number (0-based)
         * @param {number} [options.size=20] - Page size
         * @returns {Promise<Object>} Paginated exercise results
         */
        getHistory: ({ type, page = 0, size = 20 } = {}) => {
            const params = new URLSearchParams({ page, size });
            if (type) params.append('type', type);
            return request(`/v1/exercises/results?${params}`);
        },
        
        /**
         * Get recent exercise results
         * @param {number} [days=7] - Number of days to look back
         * @returns {Promise<Array>} List of recent exercise results
         */
        getRecentResults: (days = 7) => request(`/v1/exercises/results/recent?days=${days}`),
        
        /**
         * Get exercise statistics for the current user
         * @returns {Promise<Object>} Exercise statistics summary
         */
        getStatistics: () => request('/v1/exercises/statistics')
    },
    
    // ==================== Speech ====================
    speech: {
        /** 
         * Convert text to speech audio
         * @returns {Promise<Blob>} MP3 audio blob
         */
        synthesize: async (text, languageCode, slow = false, voice = null) => {
            const url = `${API_BASE}/v1/speech/synthesize`;
            
            const headers = { 'Content-Type': 'application/json' };
            const userId = getSelectedUserId();
            if (userId) {
                headers['X-User-Id'] = userId;
            }
            
            const response = await fetch(url, {
                method: 'POST',
                headers,
                body: JSON.stringify({ text, languageCode, slow, voice })
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new ApiError(error.message || `HTTP ${response.status}`, response.status, error);
            }
            
            return response.blob();
        },
        
        /** 
         * Transcribe audio to text
         * @param {Blob|File} audioFile - Audio file to transcribe
         * @param {string} languageHint - Language hint for better accuracy
         * @returns {Promise<{transcription: string}>}
         */
        transcribe: async (audioFile, languageHint = null) => {
            const url = `${API_BASE}/v1/speech/transcribe`;
            const formData = new FormData();
            formData.append('audio', audioFile);
            if (languageHint) {
                formData.append('languageHint', languageHint);
            }
            
            const headers = {};
            const userId = getSelectedUserId();
            if (userId) {
                headers['X-User-Id'] = userId;
            }
            
            const response = await fetch(url, {
                method: 'POST',
                headers,
                body: formData
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new ApiError(error.message || `HTTP ${response.status}`, response.status, error);
            }
            
            return response.json();
        }
    },
    
    // ==================== Chat ====================
    chat: {
        /** Get or create an active chat session */
        getOrCreateSession: () => request('/v1/chat/session'),
        
        /** Get a specific session by ID */
        getSession: (sessionId) => request(`/v1/chat/session/${sessionId}`),
        
        /** Get all messages in a session */
        getMessages: (sessionId) => request(`/v1/chat/session/${sessionId}/messages`),
        
        /** Send a message and get AI response */
        sendMessage: (sessionId, content) => request(`/v1/chat/session/${sessionId}/messages`, {
            method: 'POST',
            body: { content }
        }),
        
        /** Complete an embedded activity */
        completeActivity: (messageId, score, feedback = null) => request(`/v1/chat/messages/${messageId}/complete`, {
            method: 'POST',
            body: { score, feedback }
        }),
        
        /** Clear a session (delete all messages) */
        clearSession: (sessionId) => request(`/v1/chat/session/${sessionId}`, {
            method: 'DELETE'
        }),
        
        /**
         * Generate topic suggestions for an activity category
         * @param {string} category - Activity category (VOCABULARY, EXERCISE, LESSON, SCENARIO, AUDIO)
         * @param {number} [count=5] - Number of suggestions to generate (1-10)
         * @param {boolean} [includeRandom=true] - Whether to include a random topic selection
         * @returns {Promise<{activityCategory: string, suggestions: Array<{topic: string, description: string, emoji: string, alignsWithGoals: boolean}>, randomTopic?: string}>}
         */
        getTopicSuggestions: (category, count = 5, includeRandom = true) => request('/v1/chat/topics/suggestions', {
            method: 'POST',
            body: { category, count, includeRandom }
        }),
        
        /**
         * Select a random appropriate topic for an activity
         * @param {string} category - Activity category (VOCABULARY, EXERCISE, LESSON, SCENARIO, AUDIO)
         * @returns {Promise<string>} The selected topic name
         */
        selectRandomTopic: async (category) => {
            const url = `${API_BASE}/v1/chat/topics/random?category=${encodeURIComponent(category)}`;
            
            const headers = { 'Content-Type': 'application/json' };
            const userId = getSelectedUserId();
            if (userId) {
                headers['X-User-Id'] = userId;
            }
            
            const response = await fetch(url, {
                method: 'POST',
                headers
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new ApiError(error.message || `HTTP ${response.status}`, response.status, error);
            }
            
            // This endpoint returns plain text, not JSON
            return response.text();
        },
        
        /**
         * Record that a topic was used for an activity (for history tracking)
         * @param {string} topic - The topic that was used
         * @param {string} category - Activity category (VOCABULARY, EXERCISE, LESSON, SCENARIO, AUDIO)
         */
        recordTopicUsage: (topic, category) => request(`/v1/chat/topics/record?topic=${encodeURIComponent(topic)}&category=${encodeURIComponent(category)}`, {
            method: 'POST'
        })
    },
    
    // ==================== I18n ====================
    i18n: {
        /**
         * Get translations for a specific language
         * @param {string} languageCode - ISO 639-1 language code (e.g., 'en', 'de', 'fr')
         * @returns {Promise<Object>} Map of translation keys to translated text
         */
        getLanguage: (languageCode) => request(`/v1/i18n/languages/${languageCode}`),
        
        /**
         * Check if translations exist for a language (without triggering generation)
         * @param {string} languageCode - ISO 639-1 language code
         * @returns {Promise<{languageCode: string, exists: boolean}>}
         */
        hasLanguage: (languageCode) => request(`/v1/i18n/languages/${languageCode}/exists`),
        
        /**
         * Get list of all available languages with translations
         * @returns {Promise<{languages: string[]}>}
         */
        getAvailableLanguages: () => request('/v1/i18n/languages'),
        
        /**
         * Force regeneration of translations for a language
         * @param {string} languageCode - ISO 639-1 language code (cannot be 'en' or 'de')
         * @returns {Promise<Object>} Map of translation keys to translated text
         */
        generateLanguage: (languageCode) => request(`/v1/i18n/languages/${languageCode}/generate`, {
            method: 'POST'
        })
    },
    
    // ==================== Images ====================
    images: {
        /**
         * Generate an image from a text prompt
         * @param {string} prompt - Description of the image to generate
         * @param {Object} options - Optional generation options
         * @param {string} [options.size] - Image size (small, medium, large, wide, tall)
         * @param {string} [options.quality] - Quality level (standard, hd)
         * @param {string} [options.style] - Style (natural, vivid)
         * @returns {Promise<{url: string, revisedPrompt: string, size: string}>}
         */
        generate: (prompt, { size, quality, style } = {}) => request('/v1/images/generate', {
            method: 'POST',
            body: { prompt, size, quality, style }
        }),
        
        /**
         * Generate a flashcard image for a vocabulary word
         * @param {string} word - The vocabulary word
         * @param {string} [context] - Optional context or sentence
         * @returns {Promise<{url: string, revisedPrompt: string, size: string}>}
         */
        generateFlashcard: (word, context = null) => request('/v1/images/flashcard', {
            method: 'POST',
            body: { word, context }
        }),
        
        /**
         * Generate multiple flashcard images in batch
         * @param {Array<{word: string, context?: string}>} requests - Array of word requests (max 5)
         * @returns {Promise<Array<{url: string, revisedPrompt: string, size: string}>>}
         */
        generateFlashcardBatch: (requests) => request('/v1/images/flashcard/batch', {
            method: 'POST',
            body: requests
        })
    }
};

// Legacy alias for backwards compatibility
export const apiClient = api;

export { ApiError, getSelectedUserId, setSelectedUserId, clearSelectedUserId };