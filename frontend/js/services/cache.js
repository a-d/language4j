/**
 * Session Cache Service
 * =====================
 * Provides session storage-based caching for generated content.
 * Content persists across navigation but clears when the browser tab is closed.
 */

const CACHE_PREFIX = 'llp_cache_';

/**
 * Cache service for persisting page content across navigation.
 */
export const cache = {
    /**
     * Save content to session storage.
     * @param {string} page - Page identifier (e.g., 'lessons', 'vocabulary')
     * @param {string} key - Content key within the page
     * @param {*} content - Content to store (will be JSON serialized)
     */
    save(page, key, content) {
        try {
            const cacheKey = `${CACHE_PREFIX}${page}_${key}`;
            sessionStorage.setItem(cacheKey, JSON.stringify(content));
        } catch (error) {
            console.warn('Cache save failed:', error);
        }
    },
    
    /**
     * Retrieve content from session storage.
     * @param {string} page - Page identifier
     * @param {string} key - Content key within the page
     * @returns {*} Parsed content or null if not found
     */
    get(page, key) {
        try {
            const cacheKey = `${CACHE_PREFIX}${page}_${key}`;
            const data = sessionStorage.getItem(cacheKey);
            return data ? JSON.parse(data) : null;
        } catch (error) {
            console.warn('Cache get failed:', error);
            return null;
        }
    },
    
    /**
     * Check if content exists in cache.
     * @param {string} page - Page identifier
     * @param {string} key - Content key within the page
     * @returns {boolean} True if content exists
     */
    has(page, key) {
        const cacheKey = `${CACHE_PREFIX}${page}_${key}`;
        return sessionStorage.getItem(cacheKey) !== null;
    },
    
    /**
     * Remove specific content from cache.
     * @param {string} page - Page identifier
     * @param {string} key - Content key within the page
     */
    remove(page, key) {
        const cacheKey = `${CACHE_PREFIX}${page}_${key}`;
        sessionStorage.removeItem(cacheKey);
    },
    
    /**
     * Clear all cached content for a specific page.
     * @param {string} page - Page identifier
     */
    clearPage(page) {
        const prefix = `${CACHE_PREFIX}${page}_`;
        const keysToRemove = [];
        
        for (let i = 0; i < sessionStorage.length; i++) {
            const key = sessionStorage.key(i);
            if (key && key.startsWith(prefix)) {
                keysToRemove.push(key);
            }
        }
        
        keysToRemove.forEach(key => sessionStorage.removeItem(key));
    },
    
    /**
     * Clear all cached content.
     */
    clearAll() {
        const keysToRemove = [];
        
        for (let i = 0; i < sessionStorage.length; i++) {
            const key = sessionStorage.key(i);
            if (key && key.startsWith(CACHE_PREFIX)) {
                keysToRemove.push(key);
            }
        }
        
        keysToRemove.forEach(key => sessionStorage.removeItem(key));
    },
    
    /**
     * Get cache statistics for debugging.
     * @returns {Object} Cache statistics
     */
    getStats() {
        let count = 0;
        let totalSize = 0;
        const pages = new Set();
        
        for (let i = 0; i < sessionStorage.length; i++) {
            const key = sessionStorage.key(i);
            if (key && key.startsWith(CACHE_PREFIX)) {
                count++;
                const value = sessionStorage.getItem(key);
                totalSize += value ? value.length : 0;
                
                // Extract page name
                const pagePart = key.substring(CACHE_PREFIX.length);
                const page = pagePart.split('_')[0];
                pages.add(page);
            }
        }
        
        return {
            itemCount: count,
            totalSizeBytes: totalSize,
            pages: Array.from(pages)
        };
    }
};

// Page-specific cache keys for easy reference
export const CacheKeys = {
    // Lessons page
    LESSONS: {
        PAGE: 'lessons',
        CONTENT: 'content',
        TOPIC: 'topic'
    },
    
    // Vocabulary page
    VOCABULARY: {
        PAGE: 'vocabulary',
        CONTENT: 'content',
        FLASHCARDS: 'flashcards',
        TOPIC: 'topic',
        COUNT: 'count',
        FLASHCARD_TOPIC: 'flashcardTopic'
    },
    
    // Cards page
    CARDS: {
        PAGE: 'cards',
        VISUAL_CARDS: 'visualCards',
        CURRENT_INDEX: 'currentIndex'
    },
    
    // Roleplay scenarios
    ROLEPLAY: {
        PAGE: 'roleplay',
        LAST_SCENARIO: 'lastScenario'
    }
};

export default cache;