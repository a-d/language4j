/**
 * Session Cache Service
 * =====================
 * Provides session storage-based caching for generated content.
 * Content persists across navigation but clears when the browser tab is closed.
 * 
 * Multi-user support: Cache keys are prefixed with the current user ID to ensure
 * each user has their own isolated cache space.
 */

import { getSelectedUserId } from '../api/client.js';

const CACHE_PREFIX = 'llp_cache_';

/**
 * Get the full cache key including user ID prefix
 * @param {string} page - Page identifier
 * @param {string} key - Content key
 * @returns {string} Full cache key
 */
function getCacheKey(page, key) {
    const userId = getSelectedUserId();
    if (userId) {
        return `${CACHE_PREFIX}${userId}_${page}_${key}`;
    }
    // Fallback for when no user is selected (shouldn't happen normally)
    return `${CACHE_PREFIX}guest_${page}_${key}`;
}

/**
 * Cache service for persisting page content across navigation.
 * All cache operations are user-specific based on the currently selected user.
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
            const cacheKey = getCacheKey(page, key);
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
            const cacheKey = getCacheKey(page, key);
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
        const cacheKey = getCacheKey(page, key);
        return sessionStorage.getItem(cacheKey) !== null;
    },
    
    /**
     * Remove specific content from cache.
     * @param {string} page - Page identifier
     * @param {string} key - Content key within the page
     */
    remove(page, key) {
        const cacheKey = getCacheKey(page, key);
        sessionStorage.removeItem(cacheKey);
    },
    
    /**
     * Clear all cached content for a specific page (current user only).
     * @param {string} page - Page identifier
     */
    clearPage(page) {
        const userId = getSelectedUserId() || 'guest';
        const prefix = `${CACHE_PREFIX}${userId}_${page}_`;
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
     * Clear all cached content for the current user.
     */
    clearAll() {
        const userId = getSelectedUserId() || 'guest';
        const prefix = `${CACHE_PREFIX}${userId}_`;
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
     * Clear all cached content for ALL users.
     * Used when completely resetting the application state.
     */
    clearAllUsers() {
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
     * Clear cache for a specific user (by ID).
     * Used when switching users to ensure clean state.
     * @param {string} userId - User ID to clear cache for
     */
    clearForUser(userId) {
        const prefix = `${CACHE_PREFIX}${userId}_`;
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
     * Get cache statistics for debugging.
     * @returns {Object} Cache statistics
     */
    getStats() {
        const userId = getSelectedUserId() || 'guest';
        const prefix = `${CACHE_PREFIX}${userId}_`;
        let count = 0;
        let totalSize = 0;
        const pages = new Set();
        
        for (let i = 0; i < sessionStorage.length; i++) {
            const key = sessionStorage.key(i);
            if (key && key.startsWith(prefix)) {
                count++;
                const value = sessionStorage.getItem(key);
                totalSize += value ? value.length : 0;
                
                // Extract page name (format: llp_cache_{userId}_{page}_{key})
                const parts = key.substring(prefix.length).split('_');
                if (parts.length > 0) {
                    pages.add(parts[0]);
                }
            }
        }
        
        return {
            userId,
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