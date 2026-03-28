/**
 * Demo Mode Service
 * =================
 * Handles offline demo mode functionality when the backend is unavailable.
 * 
 * Features:
 * - Automatic fallback when backend is unreachable
 * - Manual toggle via localStorage
 * - Loads pre-generated demo data from local files
 * - Provides mock API responses
 */

// Demo mode storage key
const DEMO_MODE_KEY = 'llp_demo_mode';
const DEMO_DATA_BASE = './demo-data';

/**
 * Demo Mode Service class
 */
class DemoModeService {
    constructor() {
        this.enabled = false;
        this.dataCache = new Map();
        this.index = null;
        this.config = null;
        this.initialized = false;
    }

    /**
     * Initialize demo mode service
     * Loads index and config if demo data exists
     */
    async init() {
        if (this.initialized) return;

        // Check for manual demo mode setting
        const manuallyEnabled = localStorage.getItem(DEMO_MODE_KEY) === 'true';
        
        // Try to load demo data index
        try {
            const indexResponse = await fetch(`${DEMO_DATA_BASE}/index.json`);
            if (indexResponse.ok) {
                this.index = await indexResponse.json();
                
                const configResponse = await fetch(`${DEMO_DATA_BASE}/config.json`);
                if (configResponse.ok) {
                    this.config = await configResponse.json();
                }
                
                console.log('Demo data available:', this.index.topics?.length || 0, 'topics');
            }
        } catch (e) {
            console.log('Demo data not available');
        }

        if (manuallyEnabled && this.index) {
            this.enable();
        }

        this.initialized = true;
    }

    /**
     * Check if demo mode is currently enabled
     */
    isEnabled() {
        return this.enabled;
    }

    /**
     * Check if demo data is available
     */
    hasData() {
        return this.index !== null;
    }

    /**
     * Enable demo mode
     */
    enable() {
        if (!this.index) {
            console.warn('Cannot enable demo mode: no demo data available');
            return false;
        }
        
        this.enabled = true;
        localStorage.setItem(DEMO_MODE_KEY, 'true');
        console.log('Demo mode enabled - using offline data');
        
        // Add demo mode class to body for CSS styling
        document.body.classList.add('demo-mode');
        
        // Show demo mode banner
        this.showBanner();
        
        return true;
    }

    /**
     * Disable demo mode
     */
    disable() {
        this.enabled = false;
        localStorage.removeItem(DEMO_MODE_KEY);
        document.body.classList.remove('demo-mode');
        this.hideBanner();
        console.log('Demo mode disabled');
    }

    /**
     * Toggle demo mode
     */
    toggle() {
        if (this.enabled) {
            this.disable();
        } else {
            this.enable();
        }
        return this.enabled;
    }

    /**
     * Show demo mode banner at top of page
     */
    showBanner() {
        // Remove existing banner if any
        this.hideBanner();
        
        const banner = document.createElement('div');
        banner.id = 'demo-mode-banner';
        banner.className = 'demo-mode-banner';
        banner.innerHTML = `
            <span>📴 Demo Mode - Using offline sample data</span>
            <button onclick="window.demoMode.disable(); window.location.reload();">Go Online</button>
        `;
        document.body.prepend(banner);
    }

    /**
     * Hide demo mode banner
     */
    hideBanner() {
        const existing = document.getElementById('demo-mode-banner');
        if (existing) existing.remove();
    }

    /**
     * Load data from demo data files
     * @param {string} path - Relative path to data file
     * @returns {Promise<Object>} Loaded data
     */
    async loadData(path) {
        const cacheKey = path;
        
        if (this.dataCache.has(cacheKey)) {
            return this.dataCache.get(cacheKey);
        }
        
        try {
            const response = await fetch(`${DEMO_DATA_BASE}/${path}`);
            if (!response.ok) {
                throw new Error(`Failed to load ${path}: ${response.status}`);
            }
            const data = await response.json();
            this.dataCache.set(cacheKey, data);
            return data;
        } catch (error) {
            console.warn(`Demo data not found: ${path}`);
            return null;
        }
    }

    /**
     * Get available topics
     */
    getTopics() {
        return this.index?.topics || [];
    }

    /**
     * Get disabled features list
     */
    getDisabledFeatures() {
        return this.config?.disabledFeatures || [];
    }

    /**
     * Check if a feature is available in demo mode
     */
    isFeatureAvailable(feature) {
        const disabled = this.getDisabledFeatures();
        return !disabled.includes(feature);
    }

    /**
     * Normalize topic name for file lookup
     */
    normalizeTopic(topic) {
        if (!topic) return 'greetings'; // default
        
        // Convert to lowercase and remove special characters
        let normalized = topic.toLowerCase().trim();
        
        // Map common variations to standard topic names
        const topicMap = {
            'greeting': 'greetings',
            'greetings and introductions': 'greetings',
            'introduction': 'greetings',
            'food and dining': 'food',
            'dining': 'food',
            'restaurant': 'food',
            'travel and directions': 'travel',
            'directions': 'travel',
            'family and relationships': 'family',
            'relationships': 'family',
            'shopping and money': 'shopping',
            'money': 'shopping',
            'weather and seasons': 'weather',
            'seasons': 'weather',
            'work and professions': 'work',
            'professions': 'work',
            'job': 'work',
            'hobbies and free time': 'hobbies',
            'free time': 'hobbies',
            'leisure': 'hobbies',
            'health and body': 'health',
            'body': 'health',
            'medical': 'health',
            'home and furniture': 'home',
            'furniture': 'home',
            'house': 'home',
            'time and calendar': 'time',
            'calendar': 'time',
            'dates': 'time',
            'colors and numbers': 'colors',
            'numbers': 'colors',
            'colour': 'colors',
            'animal': 'animals',
            'clothes': 'clothing',
            'tech': 'technology',
            'computer': 'technology',
            'internet': 'technology'
        };
        
        if (topicMap[normalized]) {
            return topicMap[normalized];
        }
        
        // Check if it's already a valid topic
        const topics = this.getTopics();
        if (topics.includes(normalized)) {
            return normalized;
        }
        
        // Try to find a partial match
        for (const t of topics) {
            if (normalized.includes(t) || t.includes(normalized)) {
                return t;
            }
        }
        
        // Default to first topic
        return topics[0] || 'greetings';
    }

    /**
     * Handle API request in demo mode
     * Maps endpoints to local demo data files
     */
    async handleRequest(endpoint, options = {}) {
        const method = options.method || 'GET';
        let body = {};
        
        if (options.body) {
            try {
                body = typeof options.body === 'string' ? JSON.parse(options.body) : options.body;
            } catch (e) {
                body = {};
            }
        }

        // Remove leading /api or /v1 if present
        const cleanEndpoint = endpoint.replace(/^\/api/, '').replace(/^\/v1/, '');
        const routeKey = `${method} ${cleanEndpoint}`;

        console.log('Demo mode handling:', routeKey);

        // Route handlers
        const routes = {
            // I18n
            'GET /i18n/languages/en': () => this.loadData('i18n/en.json'),
            'GET /i18n/languages/de': () => this.loadData('i18n/de.json'),
            'GET /i18n/languages': () => Promise.resolve({ languages: ['en', 'de'] }),

            // Users
            'GET /users/me': () => this.loadData('user.json'),
            'PUT /users/me': () => this.loadData('user.json'),
            'GET /users': () => this.loadData('user.json').then(u => [u]),
            'GET /users/exists': () => Promise.resolve({ exists: true }),

            // Goals
            'GET /goals': () => this.loadData('goals.json'),
            'GET /goals/daily/active': () => this.loadData('goals.json').then(goals => 
                goals.filter(g => g.type === 'DAILY' && !g.completed)
            ),
            'POST /goals': () => Promise.resolve({ 
                id: 'demo-goal-new', 
                ...body,
                currentValue: 0,
                progressPercent: 0 
            }),

            // Content - Lessons
            'POST /content/lessons/generate': async () => {
                const topic = this.normalizeTopic(body.topic);
                return this.loadData(`content/lessons/${topic}.json`);
            },

            // Content - Vocabulary
            'POST /content/vocabulary/generate': async () => {
                const topic = this.normalizeTopic(body.topic);
                return this.loadData(`content/vocabulary/${topic}.json`);
            },

            // Content - Flashcards
            'POST /content/flashcards/generate': async () => {
                const topic = this.normalizeTopic(body.topic);
                return this.loadData(`content/flashcards/${topic}.json`);
            },

            // Content - Scenarios
            'POST /content/scenarios/generate': async () => {
                const topic = this.normalizeTopic(body.scenario || body.topic);
                return this.loadData(`content/scenarios/${topic}.json`);
            },

            // Visual Cards (images)
            'POST /images/flashcard/batch': async () => {
                const topic = this.normalizeTopic(body.topic);
                return this.loadData(`content/visual-cards/${topic}.json`);
            },
            'POST /images/flashcard': async () => {
                const topic = this.normalizeTopic(body.topic);
                const cards = await this.loadData(`content/visual-cards/${topic}.json`);
                // Return first card for single card request
                return cards && cards.length > 0 ? cards[0] : null;
            },

            // Exercises
            'POST /exercises/generate': async () => {
                const topic = this.normalizeTopic(body.topic);
                const type = (body.type || 'TEXT_COMPLETION').toLowerCase().replace('_', '-');
                return this.loadData(`exercises/${type}/${topic}.json`);
            },

            // Exercise results (mock - no persistence)
            'POST /exercises/results': () => Promise.resolve({
                id: 'demo-result-' + Date.now(),
                ...body,
                createdAt: new Date().toISOString()
            }),
            'GET /exercises/results/recent': () => Promise.resolve([]),
            'GET /exercises/statistics': () => Promise.resolve({
                totalExercises: 0,
                exercisesToday: 0,
                averageScore: 0,
                totalTimeSeconds: 0,
                passRate: 0,
                countsByType: {}
            }),

            // Chat
            'GET /chat/session': async () => {
                const greeting = await this.loadData('chat/greeting.json');
                return {
                    id: 'demo-session',
                    userId: 'demo-user',
                    active: true,
                    createdAt: new Date().toISOString(),
                    messages: greeting ? [greeting] : []
                };
            },
            'POST /chat/topics/suggestions': async () => {
                const category = (body.category || 'vocabulary').toLowerCase();
                return this.loadData(`chat/suggestions-${category}.json`);
            },
            'POST /chat/topics/random': () => {
                const topics = this.getTopics();
                return Promise.resolve(topics[Math.floor(Math.random() * topics.length)]);
            }
        };

        // Try exact match first
        if (routes[routeKey]) {
            return routes[routeKey]();
        }

        // Try partial matches for dynamic routes
        for (const [pattern, handler] of Object.entries(routes)) {
            const [pMethod, pPath] = pattern.split(' ');
            if (method === pMethod && cleanEndpoint.startsWith(pPath.replace(/\{.*\}/, ''))) {
                return handler();
            }
        }

        // Default: return empty response
        console.warn(`Demo mode: no handler for ${routeKey}`);
        return Promise.resolve({});
    }

    /**
     * Get chat session messages for demo mode
     */
    async getChatMessages() {
        const greeting = await this.loadData('chat/greeting.json');
        return greeting ? [greeting] : [];
    }

    /**
     * Generate a mock chat response based on user input
     */
    async generateChatResponse(userMessage) {
        const content = userMessage.toLowerCase();
        
        // Check for activity requests
        if (content.includes('vocabulary') || content.includes('words')) {
            const topic = this.extractTopic(content) || this.getRandomTopic();
            const vocabulary = await this.loadData(`content/vocabulary/${topic}.json`);
            return {
                id: 'demo-msg-' + Date.now(),
                role: 'ASSISTANT',
                content: `Here's some vocabulary about **${topic}**! Study these words and their meanings.`,
                embeddedActivityType: 'VOCABULARY',
                embeddedActivityContent: vocabulary?.content,
                createdAt: new Date().toISOString()
            };
        }

        if (content.includes('exercise') || content.includes('practice')) {
            const topic = this.extractTopic(content) || this.getRandomTopic();
            const exercises = await this.loadData(`exercises/text-completion/${topic}.json`);
            return {
                id: 'demo-msg-' + Date.now(),
                role: 'ASSISTANT',
                content: `Let's practice **${topic}**! Fill in the blanks with the correct words.`,
                embeddedActivityType: 'TEXT_COMPLETION',
                embeddedActivityContent: exercises?.content,
                createdAt: new Date().toISOString()
            };
        }

        if (content.includes('lesson') || content.includes('learn')) {
            const topic = this.extractTopic(content) || this.getRandomTopic();
            const lesson = await this.loadData(`content/lessons/${topic}.json`);
            return {
                id: 'demo-msg-' + Date.now(),
                role: 'ASSISTANT',
                content: `Here's a lesson about **${topic}**!`,
                embeddedActivityType: 'LESSON',
                embeddedActivityContent: lesson?.content,
                createdAt: new Date().toISOString()
            };
        }

        if (content.includes('scenario') || content.includes('roleplay')) {
            const topic = this.extractTopic(content) || 'greetings';
            const scenario = await this.loadData(`content/scenarios/${topic}.json`);
            return {
                id: 'demo-msg-' + Date.now(),
                role: 'ASSISTANT',
                content: `Let's practice a roleplay scenario about **${topic}**!`,
                embeddedActivityType: 'SCENARIO',
                embeddedActivityContent: scenario?.content,
                createdAt: new Date().toISOString()
            };
        }

        // Default response
        return {
            id: 'demo-msg-' + Date.now(),
            role: 'ASSISTANT',
            content: "I'm running in demo mode with limited functionality. You can:\n\n• **Practice vocabulary** - Ask about any topic\n• **Do exercises** - Try fill-in-the-blank exercises\n• **Start a lesson** - Learn about a topic\n• **Try roleplay** - Practice conversation scenarios\n\nJust select an activity from the suggestions below!",
            createdAt: new Date().toISOString()
        };
    }

    /**
     * Extract topic from user message
     */
    extractTopic(message) {
        const topics = this.getTopics();
        for (const topic of topics) {
            if (message.toLowerCase().includes(topic)) {
                return topic;
            }
        }
        return null;
    }

    /**
     * Get a random topic
     */
    getRandomTopic() {
        const topics = this.getTopics();
        return topics[Math.floor(Math.random() * topics.length)] || 'greetings';
    }
}

// Create singleton instance
export const demoMode = new DemoModeService();

// Expose globally for onclick handlers
window.demoMode = demoMode;

export default demoMode;