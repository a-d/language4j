/**
 * Language Learning Platform - Main Application
 * =============================================
 * 
 * Entry point for the application. Coordinates between modules:
 * - pages/* - Page-specific logic (dashboard, lessons, vocabulary, etc.)
 * - services/* - Shared services (i18n, toast, ui, goals, etc.)
 * - api/client.js - Backend API communication
 * 
 * Multi-user support: On startup, checks if a user is selected. If not,
 * shows the user selector modal. User selection is stored in localStorage.
 * 
 * @see frontend/README.md for architecture overview
 */

import { api, getSelectedUserId, setSelectedUserId } from './api/client.js';
import { toast } from './services/toast.js';
import { setLanguage, t, getLanguageName } from './services/i18n.js';
import { showLoading, hideLoading, openModal, closeModal, speakText } from './services/ui.js';
import { renderContent, ContentType } from './services/content-renderer.js';
import * as goals from './services/goals.js';
import { initTheme, toggleDarkMode } from './services/theme.js';
import { cache } from './services/cache.js';
import { 
    showUserSelector, 
    toggleCreateUserForm, 
    submitCreateUser, 
    selectUser,
    showUserSwitcher,
    deleteUser,
    getInitial,
    getUserColor
} from './services/user-selector.js';

// Page modules
import { loadDashboardData, getCachedGoal as getDashboardCachedGoal } from './pages/dashboard.js';
import { loadLessonsData, generateLesson } from './pages/lessons.js';
import { loadVocabularyData, generateVocabulary, generateFlashcards } from './pages/vocabulary.js';
import { loadCardsData, generateVisualCard, generateVisualCardsBatch, flipCard, prevCard, nextCard, viewCard, handleCardImageError } from './pages/cards.js';
import { startExercise, closeExercise, initExercisesPage } from './pages/exercises.js';
import { loadProgressData, filterGoals, getCachedGoal as getProgressCachedGoal } from './pages/progress.js';
import { loadSettingsData, showEditProfileModal, submitProfileUpdate } from './pages/settings.js';
import * as chatPage from './pages/chat.js';

// ==================== Application State ====================

const state = {
    user: null,
    currentPage: 'dashboard'
};

// ==================== Initialization ====================

async function init() {
    console.log('Initializing Language Learning Platform...');
    
    // Initialize theme early to prevent flash of wrong theme
    initTheme();
    
    // Initialize i18n with English as default before any UI renders
    // This ensures user selector modal has proper translations
    await setLanguage('en');
    
    setupNavigation();
    setupEventListeners();
    
    // Check for user selection - show selector if no user selected
    await initUserSelection();
    
    console.log('Application initialized');
}

/**
 * Initialize user selection.
 * If no user is selected, show the user selector modal.
 * If a user is selected, load their data.
 */
async function initUserSelection() {
    const selectedUserId = getSelectedUserId();
    
    if (selectedUserId) {
        // Try to load the selected user
        try {
            const user = await api.users.getById(selectedUserId);
            await onUserSelected(user);
            return;
        } catch (error) {
            console.warn('Selected user not found, showing selector:', error);
            // User doesn't exist anymore, show selector
        }
    }
    
    // No user selected or user not found - show selector
    await showUserSelector(openModalForUserSelector, false);
}

/**
 * Open modal with special handling for user selector (non-closable initially)
 */
function openModalForUserSelector(content, options = {}) {
    const modalContainer = document.getElementById('modal-container');
    const modalContent = document.querySelector('.modal-content');
    const modalBackdrop = document.querySelector('.modal-backdrop');
    
    if (!modalContainer || !modalContent) return;
    
    modalContent.innerHTML = content;
    modalContainer.classList.remove('hidden');
    
    // Handle closable option
    if (options.closable === false) {
        modalBackdrop?.removeEventListener('click', closeModal);
    } else {
        modalBackdrop?.addEventListener('click', closeModal);
    }
}

/**
 * Called when a user is selected or created.
 * Loads user data and initializes the app for that user.
 */
async function onUserSelected(user) {
    state.user = user;
    
    // Set language based on user's native language
    await setLanguage(user.nativeLanguage);
    
    // Update global config
    updateAppConfig(user);
    
    // Update UI
    updateUserDisplay();
    
    // Handle routing
    handleRoute();
    
    // Listen for hash changes
    window.removeEventListener('hashchange', handleRoute);
    window.addEventListener('hashchange', handleRoute);
    
    console.log('User loaded:', user.displayName);
}

/**
 * Handle user switch - reload to dashboard with new user.
 */
async function handleUserSwitch(user) {
    // Clear cache for clean state
    cache.clearAll();
    
    // Update state and UI
    await onUserSelected(user);
    
    // Navigate to dashboard
    navigateTo('dashboard');
    
    toast.success(t('users.switchedTo', { name: user.displayName }));
}

// ==================== User Management ====================

async function loadUserData() {
    try {
        state.user = await api.users.getCurrent();
        setLanguage(state.user.nativeLanguage);
        updateAppConfig(state.user);
        updateUserDisplay();
    } catch (error) {
        console.error('Failed to load user data:', error);
        state.user = {
            id: null,
            displayName: 'Learner',
            nativeLanguage: 'de',
            targetLanguage: 'fr',
            skillLevel: 'A1'
        };
        setLanguage(state.user.nativeLanguage);
        updateAppConfig(state.user);
        updateUserDisplay();
        toast.error(t('toast.backendUnavailable'));
    }
}

/**
 * Update global APP_CONFIG with user's language settings.
 * This makes language info available to modules that need it (e.g., exercises).
 * @param {Object} user - User object with language settings
 */
function updateAppConfig(user) {
    if (user) {
        window.APP_CONFIG = {
            ...window.APP_CONFIG,
            nativeLanguage: user.nativeLanguage,
            targetLanguage: user.targetLanguage,
            skillLevel: user.skillLevel
        };
    }
}

function updateUserDisplay() {
    if (!state.user) return;
    
    const nameEl = document.getElementById('user-name');
    const levelEl = document.getElementById('user-level');
    const langInfoEl = document.getElementById('language-info');
    
    if (nameEl) nameEl.textContent = state.user.displayName;
    if (levelEl) levelEl.textContent = state.user.skillLevel;
    
    if (langInfoEl) {
        const nativeLang = getLanguageName(state.user.nativeLanguage);
        const targetLang = getLanguageName(state.user.targetLanguage);
        langInfoEl.textContent = t('dashboard.learning', { target: targetLang, native: nativeLang });
    }
}

// ==================== Navigation ====================

function setupNavigation() {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(link.dataset.page);
        });
    });
    
    const navbarUserBtn = document.getElementById('navbar-user-btn');
    if (navbarUserBtn) {
        navbarUserBtn.addEventListener('click', () => {
            showUserMenu();
        });
    }
}

/**
 * Show user menu with options to edit profile, settings, or switch users
 */
function showUserMenu() {
    // Remove any existing menu
    const existingMenu = document.querySelector('.user-menu-dropdown');
    if (existingMenu) {
        existingMenu.remove();
        return;
    }
    
    const menu = document.createElement('div');
    menu.className = 'user-menu-dropdown';
    menu.innerHTML = `
        <div class="user-menu-item" onclick="window.showEditProfileModal()">
            ✏️ ${t('profile.editTitle')}
        </div>
        <div class="user-menu-item" onclick="window.showUserSwitcher()">
            👥 ${t('users.switchUser')}
        </div>
        <div class="user-menu-item" onclick="window.navigateToSettings()">
            ⚙️ ${t('nav.settings')}
        </div>
    `;
    
    // Position menu near the user button
    const userBtn = document.getElementById('navbar-user-btn');
    if (userBtn) {
        const rect = userBtn.getBoundingClientRect();
        menu.style.position = 'fixed';
        menu.style.top = `${rect.bottom + 5}px`;
        menu.style.right = `${window.innerWidth - rect.right}px`;
    }
    
    document.body.appendChild(menu);
    
    // Close menu when clicking outside
    setTimeout(() => {
        document.addEventListener('click', closeUserMenu, { once: true });
    }, 0);
}

function closeUserMenu() {
    const menu = document.querySelector('.user-menu-dropdown');
    if (menu) menu.remove();
}

function navigateTo(page) {
    window.location.hash = page;
}

function handleRoute() {
    const hash = window.location.hash.slice(1) || 'chat';  // Chat is now the default page
    showPage(hash);
}

function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(page => {
        page.classList.add('hidden');
        page.classList.remove('active');
    });
    
    // Show target page
    const targetPage = document.getElementById(`${pageName}-page`);
    if (targetPage) {
        targetPage.classList.remove('hidden');
        targetPage.classList.add('active');
    }
    
    // Update nav
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageName);
    });
    
    state.currentPage = pageName;
    loadPageData(pageName);
}

async function loadPageData(pageName) {
    switch (pageName) {
        case 'chat':
            // Chat page renders its own content dynamically
            const chatContainer = document.getElementById('chat-page-content');
            if (chatContainer) {
                chatContainer.innerHTML = chatPage.render();
            }
            await chatPage.init();
            break;
        case 'dashboard':
            await loadDashboardData(showLoading, hideLoading);
            break;
        case 'lessons':
            loadLessonsData(showLoading, hideLoading);
            break;
        case 'vocabulary':
            loadVocabularyData(showLoading, hideLoading);
            break;
        case 'cards':
            loadCardsData();
            break;
        case 'exercises':
            // Load exercise history when navigating to exercises page
            initExercisesPage();
            break;
        case 'progress':
            await loadProgressData(showLoading, hideLoading);
            break;
        case 'settings':
            loadSettingsData(state.user);
            break;
    }
}

// ==================== Event Listeners ====================

function setupEventListeners() {
    // Action cards on dashboard
    document.querySelectorAll('.action-card').forEach(card => {
        card.addEventListener('click', () => handleQuickAction(card.dataset.action));
    });
    
    // Exercise type cards
    document.querySelectorAll('.exercise-type-card').forEach(card => {
        card.addEventListener('click', () => startExercise(card.dataset.type, showLoading, hideLoading));
    });
    
    // Generate lesson button
    document.getElementById('generate-lesson-btn')?.addEventListener('click', () => navigateTo('lessons'));
    
    // Review vocabulary button
    document.getElementById('review-vocab-btn')?.addEventListener('click', () => navigateTo('vocabulary'));
    
    // Modal backdrop click to close
    document.querySelector('.modal-backdrop')?.addEventListener('click', closeModal);
}

// ==================== Quick Actions ====================

function handleQuickAction(action) {
    switch (action) {
        case 'start-lesson': navigateTo('lessons'); break;
        case 'practice-vocabulary': navigateTo('vocabulary'); break;
        case 'speaking-practice': startExercise('speaking', showLoading, hideLoading); break;
        case 'roleplay': generateRoleplayScenario(); break;
    }
}

async function generateRoleplayScenario() {
    const scenario = prompt(t('action.roleplayDesc')) || 'casual conversation';
    
    toast.info(t('misc.generating'));
    showLoading();
    
    try {
        const response = await api.content.generateScenario(scenario);
        
        openModal(`
            <div class="roleplay-scenario">
                <h2>${t('misc.roleplayTitle')} ${scenario}</h2>
                <div class="scenario-content markdown-content">${renderContent(response.content, response.type || ContentType.SCENARIO)}</div>
                <button class="btn btn-primary" onclick="window.closeModal()">${t('misc.close')}</button>
            </div>
        `);
        
        toast.success(t('toast.scenarioGenerated'));
    } catch (error) {
        console.error('Failed to generate scenario:', error);
        toast.error(t('toast.scenarioGenerateFailed'));
    } finally {
        hideLoading();
    }
}

// ==================== Global Function Registration ====================

// UI functions
window.closeModal = closeModal;
window.closeExercise = closeExercise;
window.speakText = (text, languageCode, btn) => speakText(text, languageCode || state.user?.targetLanguage, btn);

// User selector functions
window.selectUser = (userId) => selectUser(userId, closeModal, handleUserSwitch);
window.toggleCreateUserForm = toggleCreateUserForm;
window.submitCreateUser = () => submitCreateUser(closeModal, handleUserSwitch);
window.showUserSwitcher = () => {
    closeUserMenu();
    showUserSwitcher(openModal, handleUserSwitch);
};
window.deleteCurrentUser = async () => {
    if (!state.user) return;
    
    if (!confirm(t('users.deleteConfirm', { name: state.user.displayName }))) {
        return;
    }
    
    const deleted = await deleteUser(state.user.id, (wasCurrentUser) => {
        if (wasCurrentUser) {
            // Show user selector since we deleted the current user
            showUserSelector(openModalForUserSelector, false);
        }
    });
    
    if (deleted) {
        toast.success(t('users.deleted'));
    }
};

// Page generation functions
window.generateLesson = () => generateLesson(showLoading, hideLoading, goals.incrementLessonGoal);
window.generateVocabulary = () => generateVocabulary(showLoading, hideLoading, goals.incrementWordsGoal);
window.generateFlashcards = () => generateFlashcards(showLoading, hideLoading);

// Visual cards functions
window.generateVisualCard = () => generateVisualCard(showLoading, hideLoading);
window.generateVisualCardsBatch = () => generateVisualCardsBatch(showLoading, hideLoading);
window.flipCard = flipCard;
window.prevCard = prevCard;
window.nextCard = nextCard;
window.viewCard = viewCard;
window.handleCardImageError = handleCardImageError;

// Goal management functions
const reloadCurrentPage = () => loadPageData(state.currentPage);

/**
 * Get a cached goal by ID from either dashboard or progress page.
 * @param {string} goalId
 * @returns {Object|undefined}
 */
function getCachedGoal(goalId) {
    return getDashboardCachedGoal(goalId) || getProgressCachedGoal(goalId);
}

window.incrementGoal = (id) => goals.incrementGoal(id, reloadCurrentPage);
window.completeGoal = (id) => goals.completeGoal(id, reloadCurrentPage);
window.deleteGoal = (id) => goals.deleteGoal(id, reloadCurrentPage);
window.resetGoalProgress = (id) => goals.resetGoalProgress(id, reloadCurrentPage);
window.createDefaultGoals = () => goals.createDefaultGoals(reloadCurrentPage);

// Create goal modals
window.showCreateGoalModal = (defaultType) => goals.showCreateGoalModal(openModal, defaultType);
window.showCreateDailyGoalModal = () => goals.showCreateGoalModal(openModal, 'DAILY');
window.submitCreateGoal = () => goals.submitCreateGoal(closeModal, reloadCurrentPage);

// Edit goal modal
window.showEditGoalModal = (goalId) => {
    const goal = getCachedGoal(goalId);
    if (goal) {
        goals.showEditGoalModal(goal, openModal);
    } else {
        toast.error(t('toast.goalNotFound'));
    }
};
window.submitEditGoal = () => goals.submitEditGoal(closeModal, reloadCurrentPage);

// Update progress modal
window.showUpdateProgressModal = (goalId) => {
    const goal = getCachedGoal(goalId);
    if (goal) {
        goals.showUpdateProgressModal(goal, openModal);
    } else {
        toast.error(t('toast.goalNotFound'));
    }
};
window.adjustProgress = goals.adjustProgress;
window.setProgressPercent = goals.setProgressPercent;
window.submitUpdateProgress = () => goals.submitUpdateProgress(closeModal, reloadCurrentPage);

// Goal filtering (progress page)
window.filterGoals = filterGoals;

// Goal context menu
window.showGoalMenu = (goalId, event) => {
    event.stopPropagation();
    const goal = getCachedGoal(goalId);
    if (!goal) return;
    
    // Remove any existing menu
    const existingMenu = document.querySelector('.goal-context-menu');
    if (existingMenu) existingMenu.remove();
    
    // Create context menu
    const menu = document.createElement('div');
    menu.className = 'goal-context-menu';
    menu.innerHTML = `
        <button onclick="window.showEditGoalModal('${goalId}'); window.closeGoalMenu()">${t('goals.edit')}</button>
        <button onclick="window.showUpdateProgressModal('${goalId}'); window.closeGoalMenu()">${t('goals.updateProgress')}</button>
        <button onclick="window.resetGoalProgress('${goalId}'); window.closeGoalMenu()">${t('goals.reset')}</button>
        <hr>
        <button class="danger" onclick="window.deleteGoal('${goalId}'); window.closeGoalMenu()">${t('goals.delete')}</button>
    `;
    
    // Position menu near the button
    const rect = event.target.getBoundingClientRect();
    menu.style.position = 'fixed';
    menu.style.top = `${rect.bottom + 5}px`;
    menu.style.left = `${rect.left}px`;
    
    document.body.appendChild(menu);
    
    // Close menu when clicking outside
    setTimeout(() => {
        document.addEventListener('click', window.closeGoalMenu, { once: true });
    }, 0);
};

window.closeGoalMenu = () => {
    const menu = document.querySelector('.goal-context-menu');
    if (menu) menu.remove();
};

// Profile functions
window.showEditProfileModal = () => {
    closeUserMenu();
    showEditProfileModal(state.user, openModal);
};
window.submitProfileUpdate = () => submitProfileUpdate(
    (user) => { 
        state.user = user; 
        updateAppConfig(user); // Also update APP_CONFIG with new user settings
    },
    updateUserDisplay,
    closeModal,
    state.currentPage,
    loadSettingsData
);

// Theme functions
window.toggleDarkMode = toggleDarkMode;

// Navigation functions
window.navigateToSettings = () => {
    closeUserMenu();
    navigateTo('settings');
};

// ==================== Start Application ====================

document.addEventListener('DOMContentLoaded', init);