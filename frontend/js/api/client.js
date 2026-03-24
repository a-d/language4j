/**
 * API Client for Language Learning Platform
 * ==========================================
 * Handles all HTTP requests to the backend API.
 */

const API_BASE = window.APP_CONFIG?.API_URL || '/api';

/**
 * Make an HTTP request to the API
 */
async function request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    
    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        },
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
        
        // Handle empty responses (204 No Content)
        if (response.status === 204) {
            return null;
        }
        
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(error.message || 'Network error', 0);
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
        /** Get current user profile */
        getCurrent: () => request('/v1/users/me'),
        
        /** Update current user profile */
        update: (data) => request('/v1/users/me', { method: 'PUT', body: data })
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
        })
    },
    
    // ==================== Exercises ====================
    exercises: {
        /** Generate text completion (fill-in-the-blank) exercises */
        generateTextCompletion: (topic, questionCount = 5) => request('/v1/exercises/text-completion', { 
            method: 'POST', 
            body: { topic, questionCount } 
        }),
        
        /** Generate drag-and-drop (word order) exercises */
        generateDragDrop: (topic, questionCount = 5) => request('/v1/exercises/drag-drop', { 
            method: 'POST', 
            body: { topic, questionCount } 
        }),
        
        /** Generate translation exercises */
        generateTranslation: (topic, questionCount = 5) => request('/v1/exercises/translation', { 
            method: 'POST', 
            body: { topic, questionCount } 
        }),
        
        /** Evaluate a user's exercise response */
        evaluate: (exercise, userResponse, expectedAnswer) => request('/v1/exercises/evaluate', { 
            method: 'POST', 
            body: { exercise, userResponse, expectedAnswer } 
        })
    },
    
    // ==================== Speech ====================
    speech: {
        /** 
         * Convert text to speech audio
         * @returns {Promise<Blob>} MP3 audio blob
         */
        synthesize: async (text, languageCode, slow = false, voice = null) => {
            const url = `${API_BASE}/v1/speech/synthesize`;
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
            
            const response = await fetch(url, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new ApiError(error.message || `HTTP ${response.status}`, response.status, error);
            }
            
            return response.json();
        }
    }
    
    // Note: Image endpoints are not yet implemented in the backend controllers.
    // When ImageController is added, uncomment and update these:
    
    // ==================== Images (Not Yet Implemented) ====================
    // images: {
    //     generate: (prompt) => ...
    // }
};

export { ApiError };