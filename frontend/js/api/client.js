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
        
        // Handle empty responses
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
 */
export const api = {
    // ==================== Users ====================
    users: {
        getCurrent: () => request('/v1/users/me'),
        update: (data) => request('/v1/users/me', { method: 'PUT', body: data }),
        getProgress: () => request('/v1/users/me/progress')
    },
    
    // ==================== Learning Goals ====================
    goals: {
        list: (type) => request(`/v1/goals${type ? `?type=${type}` : ''}`),
        get: (id) => request(`/v1/goals/${id}`),
        create: (data) => request('/v1/goals', { method: 'POST', body: data }),
        update: (id, data) => request(`/v1/goals/${id}`, { method: 'PUT', body: data }),
        delete: (id) => request(`/v1/goals/${id}`, { method: 'DELETE' }),
        updateProgress: (id, progress) => request(`/v1/goals/${id}/progress`, { 
            method: 'PATCH', 
            body: { currentValue: progress } 
        })
    },
    
    // ==================== Lessons ====================
    lessons: {
        list: () => request('/v1/lessons'),
        get: (id) => request(`/v1/lessons/${id}`),
        generate: (topic) => request('/v1/lessons/generate', { 
            method: 'POST', 
            body: { topic } 
        }),
        complete: (id, data) => request(`/v1/lessons/${id}/complete`, { 
            method: 'POST', 
            body: data 
        })
    },
    
    // ==================== Vocabulary ====================
    vocabulary: {
        list: () => request('/v1/vocabulary'),
        get: (id) => request(`/v1/vocabulary/${id}`),
        generate: (topic, count = 10) => request('/v1/vocabulary/generate', { 
            method: 'POST', 
            body: { topic, wordCount: count } 
        }),
        addWords: (words) => request('/v1/vocabulary', { method: 'POST', body: { words } }),
        getFlashcards: () => request('/v1/vocabulary/flashcards')
    },
    
    // ==================== Exercises ====================
    exercises: {
        // Text completion
        getTextCompletion: (topic) => request('/v1/exercises/text-completion', { 
            method: 'POST', 
            body: { topic } 
        }),
        
        // Drag and drop
        getDragDrop: (topic) => request('/v1/exercises/drag-drop', { 
            method: 'POST', 
            body: { topic } 
        }),
        
        // Translation
        getTranslation: (topic) => request('/v1/exercises/translation', { 
            method: 'POST', 
            body: { topic } 
        }),
        
        // Submit result
        submitResult: (data) => request('/v1/exercises/results', { 
            method: 'POST', 
            body: data 
        }),
        
        // Get history
        getHistory: (type) => request(`/v1/exercises/history${type ? `?type=${type}` : ''}`)
    },
    
    // ==================== Speech ====================
    speech: {
        // Text to speech
        synthesize: (text, language) => request('/v1/speech/synthesize', { 
            method: 'POST', 
            body: { text, language } 
        }),
        
        // Speech to text
        transcribe: async (audioBlob, language) => {
            const formData = new FormData();
            formData.append('audio', audioBlob);
            formData.append('language', language);
            
            const response = await fetch(`${API_BASE}/v1/speech/transcribe`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new ApiError('Transcription failed', response.status);
            }
            
            return response.json();
        },
        
        // Evaluate pronunciation
        evaluatePronunciation: (expected, transcription) => request('/v1/speech/evaluate', { 
            method: 'POST', 
            body: { expected, transcription } 
        })
    },
    
    // ==================== Learning Plan ====================
    learningPlan: {
        get: () => request('/v1/learning-plan'),
        generate: (preferences) => request('/v1/learning-plan/generate', { 
            method: 'POST', 
            body: preferences 
        }),
        assessLevel: (responses) => request('/v1/learning-plan/assess', { 
            method: 'POST', 
            body: { responses } 
        })
    },
    
    // ==================== Scenarios ====================
    scenarios: {
        list: () => request('/v1/scenarios'),
        get: (id) => request(`/v1/scenarios/${id}`),
        generate: (type) => request('/v1/scenarios/generate', { 
            method: 'POST', 
            body: { type } 
        }),
        chat: (scenarioId, message) => request(`/v1/scenarios/${scenarioId}/chat`, { 
            method: 'POST', 
            body: { message } 
        })
    },
    
    // ==================== Configuration ====================
    config: {
        get: () => request('/v1/config'),
        update: (data) => request('/v1/config', { method: 'PUT', body: data }),
        getLanguages: () => request('/v1/config/languages')
    }
};

export { ApiError };