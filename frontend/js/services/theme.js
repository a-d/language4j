/**
 * Theme Service
 * ==============
 * Manages dark mode and theme preferences.
 * Persists user preference to localStorage.
 */

const THEME_STORAGE_KEY = 'language-learning-theme';
const DARK_CLASS = 'dark-mode';

/**
 * Theme state
 */
let isDarkMode = false;

/**
 * Initialize theme from stored preference or system preference.
 * Should be called early in app initialization.
 */
export function initTheme() {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    
    if (stored !== null) {
        // Use stored preference
        isDarkMode = stored === 'dark';
    } else {
        // Check system preference
        isDarkMode = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    }
    
    applyTheme();
    setupSystemThemeListener();
}

/**
 * Toggle between light and dark mode.
 * @returns {boolean} New dark mode state
 */
export function toggleDarkMode() {
    isDarkMode = !isDarkMode;
    localStorage.setItem(THEME_STORAGE_KEY, isDarkMode ? 'dark' : 'light');
    applyTheme();
    return isDarkMode;
}

/**
 * Set dark mode explicitly.
 * @param {boolean} enabled - Whether dark mode should be enabled
 */
export function setDarkMode(enabled) {
    isDarkMode = enabled;
    localStorage.setItem(THEME_STORAGE_KEY, isDarkMode ? 'dark' : 'light');
    applyTheme();
}

/**
 * Get current dark mode state.
 * @returns {boolean} Whether dark mode is enabled
 */
export function isDark() {
    return isDarkMode;
}

/**
 * Apply the current theme to the document.
 */
function applyTheme() {
    if (isDarkMode) {
        document.documentElement.classList.add(DARK_CLASS);
    } else {
        document.documentElement.classList.remove(DARK_CLASS);
    }
    
    // Update any toggle switches in the UI
    updateToggleUI();
}

/**
 * Update toggle switch UI elements to reflect current state.
 */
function updateToggleUI() {
    const toggle = document.getElementById('dark-mode-toggle');
    if (toggle) {
        toggle.checked = isDarkMode;
    }
}

/**
 * Listen for system theme changes (if user hasn't set a preference).
 */
function setupSystemThemeListener() {
    const mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)');
    
    if (mediaQuery?.addEventListener) {
        mediaQuery.addEventListener('change', (e) => {
            // Only auto-switch if user hasn't set a manual preference
            const stored = localStorage.getItem(THEME_STORAGE_KEY);
            if (stored === null) {
                isDarkMode = e.matches;
                applyTheme();
            }
        });
    }
}

/**
 * Export theme service
 */
export const theme = {
    init: initTheme,
    toggle: toggleDarkMode,
    setDark: setDarkMode,
    isDark
};