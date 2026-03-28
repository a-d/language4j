/**
 * Internationalization (i18n) Service
 * ====================================
 * Dynamic translation system for the Language Learning Platform.
 * 
 * Features:
 * - Loads bundled translations (en, de) from backend
 * - Dynamically generates translations for new languages via LLM
 * - Caches translations in localStorage for offline resilience
 * - Falls back to English if translation is unavailable
 */

import { api } from '../api/client.js';

/**
 * Embedded English translations as fallback (subset for offline mode)
 * Full translations are loaded from backend on init
 */
const fallbackTranslations = {
    'nav.chat': 'Coach',
    'nav.dashboard': 'Dashboard',
    'nav.lessons': 'Lessons',
    'nav.vocabulary': 'Vocabulary',
    'nav.cards': 'Cards',
    'nav.exercises': 'Exercises',
    'nav.progress': 'Progress',
    'nav.settings': 'Settings',
    'misc.loading': 'Loading...',
    'misc.close': 'Close',
    'toast.backendUnavailable': 'Backend unavailable - showing offline mode'
};

/**
 * Translation storage
 */
let translations = {};
let currentLanguage = 'en';
let isLoading = false;
let loadPromise = null;

/**
 * LocalStorage keys for caching
 */
const STORAGE_PREFIX = 'i18n_';
const STORAGE_VERSION_KEY = 'i18n_version';
const CACHE_VERSION = '1.0.0'; // Increment when translation keys change

/**
 * Load translations for a language from backend or cache
 * @param {string} langCode - Language code (e.g., 'en', 'de', 'fr')
 * @returns {Promise<boolean>} True if translations were loaded successfully
 */
async function loadLanguage(langCode) {
    const lang = langCode.toLowerCase();
    
    // Already loaded in memory
    if (translations[lang] && Object.keys(translations[lang]).length > 10) {
        return true;
    }
    
    // Try localStorage cache first (fast)
    const cached = loadFromCache(lang);
    if (cached) {
        translations[lang] = cached;
        console.log(`Loaded ${lang} translations from cache (${Object.keys(cached).length} keys)`);
        return true;
    }
    
    // Load from backend
    try {
        console.log(`Loading ${lang} translations from backend...`);
        const data = await api.i18n.getLanguage(lang);
        
        if (data && Object.keys(data).length > 0) {
            translations[lang] = data;
            saveToCache(lang, data);
            console.log(`Loaded ${lang} translations from backend (${Object.keys(data).length} keys)`);
            return true;
        }
    } catch (error) {
        console.warn(`Failed to load translations for ${lang}:`, error.message);
    }
    
    return false;
}

/**
 * Load translations from localStorage cache
 * @param {string} langCode - Language code
 * @returns {Object|null} Cached translations or null
 */
function loadFromCache(langCode) {
    try {
        // Check cache version
        const version = localStorage.getItem(STORAGE_VERSION_KEY);
        if (version !== CACHE_VERSION) {
            // Clear old cache
            clearCache();
            localStorage.setItem(STORAGE_VERSION_KEY, CACHE_VERSION);
            return null;
        }
        
        const cached = localStorage.getItem(STORAGE_PREFIX + langCode);
        if (cached) {
            return JSON.parse(cached);
        }
    } catch (e) {
        console.warn('Failed to load from localStorage:', e);
    }
    return null;
}

/**
 * Save translations to localStorage cache
 * @param {string} langCode - Language code
 * @param {Object} data - Translation data
 */
function saveToCache(langCode, data) {
    try {
        localStorage.setItem(STORAGE_PREFIX + langCode, JSON.stringify(data));
        localStorage.setItem(STORAGE_VERSION_KEY, CACHE_VERSION);
    } catch (e) {
        console.warn('Failed to save to localStorage:', e);
    }
}

/**
 * Clear all cached translations
 */
function clearCache() {
    try {
        const keysToRemove = [];
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key && key.startsWith(STORAGE_PREFIX)) {
                keysToRemove.push(key);
            }
        }
        keysToRemove.forEach(key => localStorage.removeItem(key));
        console.log('Cleared i18n cache');
    } catch (e) {
        console.warn('Failed to clear cache:', e);
    }
}

/**
 * Set the current language and load translations
 * @param {string} langCode - Language code (e.g., 'en', 'de')
 * @returns {Promise<void>}
 */
export async function setLanguage(langCode) {
    const lang = langCode.toLowerCase();
    currentLanguage = lang;
    
    // Prevent concurrent loading
    if (isLoading) {
        await loadPromise;
        return;
    }
    
    isLoading = true;
    loadPromise = loadLanguage(lang);
    
    try {
        const success = await loadPromise;
        
        if (!success) {
            // Fall back to English if target language failed
            if (lang !== 'en') {
                console.log('Falling back to English translations');
                await loadLanguage('en');
            }
        }
    } finally {
        isLoading = false;
        loadPromise = null;
    }
    
    applyTranslations();
}

/**
 * Get the current language code
 * @returns {string} Current language code
 */
export function getLanguage() {
    return currentLanguage;
}

/**
 * Translate a key with optional parameter substitution
 * @param {string} key - Translation key
 * @param {Object} params - Parameters to substitute (e.g., {target: 'French', native: 'German'})
 * @returns {string} Translated string
 */
export function t(key, params = {}) {
    // Try current language
    let text = translations[currentLanguage]?.[key];
    
    // Fall back to English
    if (!text && currentLanguage !== 'en') {
        text = translations['en']?.[key];
    }
    
    // Fall back to embedded fallback
    if (!text) {
        text = fallbackTranslations[key];
    }
    
    // Return key if no translation found
    if (!text) {
        return key;
    }
    
    // Substitute parameters like {target} with actual values
    Object.entries(params).forEach(([param, value]) => {
        text = text.replace(new RegExp(`\\{${param}\\}`, 'g'), value);
    });
    
    return text;
}

/**
 * Apply translations to all elements with data-i18n attribute
 */
export function applyTranslations() {
    document.querySelectorAll('[data-i18n]').forEach(element => {
        const key = element.getAttribute('data-i18n');
        element.textContent = t(key);
    });
    
    // Handle placeholders
    document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
        const key = element.getAttribute('data-i18n-placeholder');
        element.placeholder = t(key);
    });
    
    // Handle titles
    document.querySelectorAll('[data-i18n-title]').forEach(element => {
        const key = element.getAttribute('data-i18n-title');
        element.title = t(key);
    });
}

/**
 * Get translated language name
 * @param {string} langCode - Language code
 * @returns {string} Translated language name
 */
export function getLanguageName(langCode) {
    return t(`lang.${langCode}`) || langCode.toUpperCase();
}

/**
 * Get translated goal type
 * @param {string} type - Goal type (DAILY, WEEKLY, etc.)
 * @returns {string} Translated goal type
 */
export function getGoalType(type) {
    const typeMap = {
        'DAILY': 'goals.typeDaily',
        'WEEKLY': 'goals.typeWeekly',
        'MONTHLY': 'goals.typeMonthly',
        'YEARLY': 'goals.typeYearly'
    };
    return t(typeMap[type] || type);
}

/**
 * Get translated skill level description
 * @param {string} level - CEFR level (A1-C2)
 * @returns {string} Translated description
 */
export function getSkillLevelDescription(level) {
    return t(`level.${level}`);
}

/**
 * Check if translations are ready (loaded)
 * @returns {boolean}
 */
export function isReady() {
    return Object.keys(translations[currentLanguage] || {}).length > 10;
}

/**
 * Wait for translations to be ready
 * @returns {Promise<void>}
 */
export async function waitForReady() {
    if (loadPromise) {
        await loadPromise;
    }
}

/**
 * Force reload translations from backend
 * @param {string} [langCode] - Language code (defaults to current language)
 * @returns {Promise<boolean>}
 */
export async function reloadTranslations(langCode = currentLanguage) {
    const lang = langCode.toLowerCase();
    
    // Clear cache for this language
    try {
        localStorage.removeItem(STORAGE_PREFIX + lang);
    } catch (e) {
        // Ignore
    }
    
    // Clear memory
    delete translations[lang];
    
    // Reload
    return await loadLanguage(lang);
}

// Export the i18n object for convenience
export const i18n = {
    t,
    setLanguage,
    getLanguage,
    applyTranslations,
    getLanguageName,
    getGoalType,
    getSkillLevelDescription,
    isReady,
    waitForReady,
    reloadTranslations,
    clearCache
};