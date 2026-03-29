/**
 * Topics Service
 * ==============
 * Manages topic suggestions for learning activities.
 * 
 * Features:
 * - Fetches LLM-generated topic suggestions from backend API
 * - Caches suggestions per category to avoid repeated API calls
 * - Provides fallback static topics for demo mode
 * - Builds reusable topic grid UI components
 */

import { api } from '../api/client.js';
import { demoMode } from './demo-mode.js';

/**
 * Fallback topic data (used when API unavailable or in demo mode)
 * These match the demo data topics.
 */
const FALLBACK_TOPICS = [
    { topic: 'greetings', emoji: '👋' },
    { topic: 'food', emoji: '🍕' },
    { topic: 'travel', emoji: '✈️' },
    { topic: 'family', emoji: '👨‍👩‍👧‍👦' },
    { topic: 'shopping', emoji: '🛒' },
    { topic: 'home', emoji: '🏠' },
    { topic: 'weather', emoji: '🌤️' },
    { topic: 'work', emoji: '💼' },
    { topic: 'health', emoji: '🏥' },
    { topic: 'hobbies', emoji: '🎨' },
    { topic: 'animals', emoji: '🐾' },
    { topic: 'colors', emoji: '🎨' },
    { topic: 'clothing', emoji: '👕' }
];

/**
 * Activity category mapping for API
 * Maps frontend section names to backend activity categories
 */
const CATEGORY_MAP = {
    'lesson': 'LESSON',
    'lessons': 'LESSON',
    'exercise': 'EXERCISE',
    'exercises': 'EXERCISE',
    'vocabulary': 'VOCABULARY',
    'vocab': 'VOCABULARY',
    'flashcard': 'VOCABULARY',
    'flashcards': 'VOCABULARY',
    'cards': 'VOCABULARY',
    'visual-cards': 'VOCABULARY',
    'scenario': 'SCENARIO',
    'scenarios': 'SCENARIO'
};

/**
 * Topics Service Class
 */
class TopicsService {
    constructor() {
        // Cache for suggestions: Map<category, { suggestions, timestamp }>
        this.cache = new Map();
        // Cache duration: 10 minutes
        this.cacheDuration = 10 * 60 * 1000;
        // Loading states: Map<category, boolean>
        this.loading = new Map();
        // Callbacks waiting for suggestions: Map<category, Set<Function>>
        this.waitingCallbacks = new Map();
    }

    /**
     * Normalize category name to backend format
     * @param {string} category - Category name (e.g., 'lessons', 'vocab')
     * @returns {string} Backend category (e.g., 'LESSON', 'VOCABULARY')
     */
    normalizeCategory(category) {
        const key = (category || 'vocabulary').toLowerCase();
        return CATEGORY_MAP[key] || 'VOCABULARY';
    }

    /**
     * Check if cached suggestions are still valid
     * @param {string} category - Backend category name
     * @returns {boolean}
     */
    isCacheValid(category) {
        const cached = this.cache.get(category);
        if (!cached) return false;
        return (Date.now() - cached.timestamp) < this.cacheDuration;
    }

    /**
     * Get cached suggestions if available and valid
     * @param {string} category - Backend category name
     * @returns {Array|null}
     */
    getCached(category) {
        if (this.isCacheValid(category)) {
            return this.cache.get(category).suggestions;
        }
        return null;
    }

    /**
     * Fetch topic suggestions from backend API
     * @param {string} category - Activity category (LESSON, EXERCISE, VOCABULARY, SCENARIO)
     * @param {number} count - Number of suggestions to fetch
     * @returns {Promise<Array<{topic: string, description: string, emoji: string}>>}
     */
    async fetchSuggestions(category, count = 8) {
        // If in demo mode, return fallback topics
        if (demoMode.isEnabled()) {
            return this.getFallbackTopics();
        }

        // Check cache first
        const cached = this.getCached(category);
        if (cached) {
            return cached;
        }

        // If already loading, wait for result
        if (this.loading.get(category)) {
            return this.waitForSuggestions(category);
        }

        // Start loading
        this.loading.set(category, true);

        try {
            const response = await api.chat.getTopicSuggestions(category, count, false);
            
            const suggestions = (response.suggestions || []).map(s => ({
                topic: s.topic,
                description: s.description || '',
                emoji: s.emoji || '📚',
                alignsWithGoals: s.alignsWithGoals || false
            }));

            // Cache the results
            this.cache.set(category, {
                suggestions,
                timestamp: Date.now()
            });

            // Notify waiting callbacks
            this.notifyWaiting(category, suggestions);

            return suggestions;

        } catch (error) {
            console.error('Failed to fetch topic suggestions:', error);
            // Return fallback topics on error
            const fallback = this.getFallbackTopics();
            this.notifyWaiting(category, fallback);
            return fallback;
        } finally {
            this.loading.set(category, false);
        }
    }

    /**
     * Wait for suggestions that are currently being fetched
     * @param {string} category
     * @returns {Promise<Array>}
     */
    waitForSuggestions(category) {
        return new Promise(resolve => {
            if (!this.waitingCallbacks.has(category)) {
                this.waitingCallbacks.set(category, new Set());
            }
            this.waitingCallbacks.get(category).add(resolve);
        });
    }

    /**
     * Notify waiting callbacks that suggestions are ready
     * @param {string} category
     * @param {Array} suggestions
     */
    notifyWaiting(category, suggestions) {
        const callbacks = this.waitingCallbacks.get(category);
        if (callbacks) {
            callbacks.forEach(cb => cb(suggestions));
            this.waitingCallbacks.set(category, new Set());
        }
    }

    /**
     * Get fallback topics (used in demo mode or on error)
     * @returns {Array<{topic: string, description: string, emoji: string}>}
     */
    getFallbackTopics() {
        return FALLBACK_TOPICS.map(t => ({
            topic: t.topic,
            description: '',
            emoji: t.emoji
        }));
    }

    /**
     * Clear cached suggestions for a category or all categories
     * @param {string} [category] - Category to clear, or all if not specified
     */
    clearCache(category) {
        if (category) {
            this.cache.delete(this.normalizeCategory(category));
        } else {
            this.cache.clear();
        }
    }

    /**
     * Build a topic grid HTML component
     * Shows loading state, then fetches and displays suggestions.
     * 
     * @param {Object} options - Configuration options
     * @param {string} options.category - Activity category (lesson, exercise, vocabulary, etc.)
     * @param {string} options.onSelectFn - Global function name to call when topic selected
     * @param {string} [options.gridId] - ID for the grid element
     * @param {string} [options.dividerText] - Text for the divider
     * @param {string} [options.hintText] - Hint text below the grid
     * @param {Function} [options.t] - Translation function
     * @returns {string} HTML string with loading placeholder
     */
    buildTopicGridPlaceholder(options = {}) {
        const {
            category = 'vocabulary',
            onSelectFn,
            gridId = 'topic-grid',
            dividerText,
            hintText,
            t = (key) => key
        } = options;

        const divider = dividerText || t('exercises.orSelectBelow') || 'or select a topic below';
        const hint = hintText || t('topics.aiSuggestionsHint') || 'Topic suggestions based on your learning goals';

        return `
            <div class="topic-suggestions-container" data-category="${category}" data-select-fn="${onSelectFn}">
                <div class="topic-divider">
                    <span>${divider}</span>
                </div>
                
                <div class="topic-grid loading" id="${gridId}">
                    <div class="topic-grid-loading">
                        <span class="loading-spinner"></span>
                        <span>${t('topics.loadingSuggestions') || 'Loading suggestions...'}</span>
                    </div>
                </div>
                
                <p class="topic-suggestions-hint">
                    ✨ ${hint}
                </p>
            </div>
        `;
    }

    /**
     * Initialize a topic grid by fetching suggestions and rendering buttons.
     * Call this after the placeholder HTML is inserted into the DOM.
     * 
     * @param {string} gridId - ID of the grid element
     * @param {string} category - Activity category
     * @param {string} onSelectFn - Global function name for selection callback
     * @param {Function} [t] - Translation function
     */
    async initTopicGrid(gridId, category, onSelectFn, t = (key) => key) {
        const grid = document.getElementById(gridId);
        if (!grid) return;

        const backendCategory = this.normalizeCategory(category);

        try {
            const suggestions = await this.fetchSuggestions(backendCategory);
            
            grid.classList.remove('loading');
            grid.innerHTML = suggestions.map(s => `
                <button class="topic-btn" 
                        data-topic="${escapeHtml(s.topic)}" 
                        onclick="window.${onSelectFn}('${escapeHtml(s.topic)}')"
                        title="${escapeHtml(s.description)}">
                    ${s.emoji} ${escapeHtml(s.topic)}
                </button>
            `).join('');

        } catch (error) {
            console.error('Failed to init topic grid:', error);
            grid.classList.remove('loading');
            grid.innerHTML = `
                <div class="topic-grid-error">
                    <span>${t('topics.loadFailed') || 'Failed to load suggestions'}</span>
                    <button class="btn btn-sm btn-secondary" onclick="window.topicsService.retryTopicGrid('${gridId}', '${category}', '${onSelectFn}')">
                        ${t('misc.retry') || 'Retry'}
                    </button>
                </div>
            `;
        }
    }

    /**
     * Retry loading a topic grid
     */
    async retryTopicGrid(gridId, category, onSelectFn) {
        const grid = document.getElementById(gridId);
        if (grid) {
            grid.classList.add('loading');
            grid.innerHTML = `
                <div class="topic-grid-loading">
                    <span class="loading-spinner"></span>
                    <span>Loading suggestions...</span>
                </div>
            `;
        }
        
        // Clear cache for this category to force refetch
        this.clearCache(category);
        await this.initTopicGrid(gridId, category, onSelectFn);
    }

    /**
     * Build a topic selector modal/UI (full page selector).
     * Used by exercises page for more prominent topic selection.
     * 
     * @param {HTMLElement} container - Container element
     * @param {Object} options - Configuration
     * @param {string} options.title - Title to display
     * @param {string} options.category - Activity category
     * @param {Function} options.t - Translation function
     * @param {string} [options.inputPlaceholder] - Input placeholder
     * @param {string} [options.submitButtonText] - Submit button text
     * @param {Function} [options.onCancel] - Cancel callback
     * @returns {Promise<string|null>} Selected topic or null
     */
    showTopicSelector(container, options = {}) {
        const {
            title,
            category = 'exercise',
            t = (key) => key,
            inputPlaceholder = t('lessons.topicPlaceholder') || 'Enter topic...',
            submitButtonText = t('exercises.start') || 'Start',
            onCancel
        } = options;

        const gridId = `selector-topic-grid-${Date.now()}`;
        const backendCategory = this.normalizeCategory(category);

        return new Promise(async (resolve) => {
            // Render initial UI with loading state
            container.innerHTML = `
                <div class="exercise-container">
                    <div class="exercise-header">
                        <h3>${title}</h3>
                        <button class="btn btn-sm btn-secondary" id="topic-selector-cancel">${t('misc.cancel') || 'Cancel'}</button>
                    </div>
                    <div class="topic-selector-content">
                        <p class="selector-instruction">${t('exercises.selectTopic') || 'Select a topic to practice'}</p>
                        
                        <!-- Topic input with submit -->
                        <div class="topic-input-row">
                            <input type="text" 
                                   id="topic-selector-input" 
                                   class="form-input topic-search-input" 
                                   placeholder="${inputPlaceholder}" />
                            <button class="btn btn-primary" id="topic-selector-submit">
                                🎯 ${submitButtonText}
                            </button>
                        </div>
                        
                        <div class="topic-divider">
                            <span>${t('exercises.orSelectBelow') || 'or select a topic below'}</span>
                        </div>
                        
                        <div class="topic-grid loading" id="${gridId}">
                            <div class="topic-grid-loading">
                                <span class="loading-spinner"></span>
                                <span>${t('topics.loadingSuggestions') || 'Loading suggestions...'}</span>
                            </div>
                        </div>
                        
                        <p class="topic-suggestions-hint">
                            ✨ ${t('topics.aiSuggestionsHint') || 'Topic suggestions based on your learning goals'}
                        </p>
                    </div>
                </div>
            `;

            const topicInput = document.getElementById('topic-selector-input');
            const submitBtn = document.getElementById('topic-selector-submit');
            const cancelBtn = document.getElementById('topic-selector-cancel');

            // Handle cancel
            cancelBtn?.addEventListener('click', () => {
                if (onCancel) onCancel();
                resolve(null);
            });

            // Handle submit button
            submitBtn?.addEventListener('click', () => {
                const inputTopic = topicInput?.value.trim();
                if (inputTopic) {
                    resolve(inputTopic);
                }
            });

            // Handle Enter key
            topicInput?.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    submitBtn?.click();
                }
            });

            // Focus input
            topicInput?.focus();

            // Load suggestions asynchronously
            try {
                const suggestions = await this.fetchSuggestions(backendCategory);
                const grid = document.getElementById(gridId);
                
                if (grid) {
                    grid.classList.remove('loading');
                    grid.innerHTML = suggestions.map(s => `
                        <button class="topic-btn" 
                                data-topic="${escapeHtml(s.topic)}"
                                title="${escapeHtml(s.description)}">
                            ${s.emoji} ${escapeHtml(s.topic)}
                        </button>
                    `).join('');

                    // Add click handlers
                    grid.querySelectorAll('.topic-btn').forEach(btn => {
                        btn.addEventListener('click', () => resolve(btn.dataset.topic));
                    });
                }
            } catch (error) {
                console.error('Failed to load topic suggestions:', error);
                const grid = document.getElementById(gridId);
                if (grid) {
                    grid.classList.remove('loading');
                    grid.innerHTML = `
                        <div class="topic-grid-error">
                            <span>${t('topics.loadFailed') || 'Failed to load suggestions'}</span>
                        </div>
                    `;
                }
            }
        });
    }
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = String(text);
    return div.innerHTML;
}

// Create and export singleton instance
export const topicsService = new TopicsService();

// Expose globally for onclick handlers
window.topicsService = topicsService;

export default topicsService;