/**
 * Language Learning Platform - Main Application
 * =============================================
 * 
 * Entry point for the application. Coordinates between modules:
 * - pages/* - Page-specific logic (dashboard, lessons, vocabulary, etc.)
 * - services/* - Shared services (i18n, toast, ui, goals, etc.)
 * - api/client.js - Backend API communication
 * 
 * @see frontend/README.md for architecture overview
 */

import { api } from './api/client.js';
import { toast } from './services/toast.js';
import { setLanguage, t, getLanguageName } from './services/i18n.js';
import { showLoading, hideLoading, openModal, closeModal, speakText } from './services/ui.js';
import { renderContent, ContentType } from './services/content-renderer.js';
import * as goals from './services/goals.js';
import { initTheme, toggleDarkMode } from './services/theme.js';

// Page modules
import { loadDashboardData, getCachedGoal as getDashboardCachedGoal } from './pages/dashboard.js';
import { loadLessonsData, generateLesson } from './pages/lessons.js';
import { loadVocabularyData, generateVocabulary, generateFlashcards } from './pages/vocabulary.js';
import { loadCardsData, generateVisualCard, generateVisualCardsBatch, flipCard, prevCard, nextCard, viewCard, handleCardImageError } from './pages/cards.js';
import { startExercise, closeExercise, initExercisesPage } from './pages/exercises.js';
import { loadProgressData, filterGoals, getCachedGoal as getProgressCachedGoal } from './pages/progress.js';
import { loadSettingsData, showEditProfileModal, submitProfileUpdate } from './pages/settings.js';

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
    
    setupNavigation();
    setupEventListeners();
    await loadUserData();
    handleRoute();
    
    window.addEventListener('hashchange', handleRoute);
    console.log('Application initialized');
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
            showEditProfileModal(state.user, openModal);
        });
    }
}

function navigateTo(page) {
    window.location.hash = page;
}

function handleRoute() {
    const hash = window.location.hash.slice(1) || 'dashboard';
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
window.showEditProfileModal = () => showEditProfileModal(state.user, openModal);
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

// ==================== Start Application ====================

document.addEventListener('DOMContentLoaded', init);