/**
 * Language Learning Platform - Main Application
 * =============================================
 * Vanilla JavaScript frontend for the language learning platform.
 */

import { api } from './api/client.js';
import { router } from './services/router.js';
import { toast } from './services/toast.js';

/**
 * Application state management
 */
const state = {
    user: null,
    currentPage: 'dashboard',
    loading: false
};

/**
 * Initialize the application
 */
async function init() {
    console.log('Initializing Language Learning Platform...');
    
    // Set up navigation
    setupNavigation();
    
    // Set up page-specific event listeners
    setupEventListeners();
    
    // Load initial data
    await loadUserData();
    
    // Handle initial route
    handleRoute();
    
    // Listen for hash changes
    window.addEventListener('hashchange', handleRoute);
    
    console.log('Application initialized');
}

/**
 * Set up navigation click handlers
 */
function setupNavigation() {
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            navigateTo(page);
        });
    });
}

/**
 * Navigate to a specific page
 */
function navigateTo(page) {
    // Update URL hash
    window.location.hash = page;
}

/**
 * Handle route changes
 */
function handleRoute() {
    const hash = window.location.hash.slice(1) || 'dashboard';
    showPage(hash);
}

/**
 * Show a specific page
 */
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
    
    // Update navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageName);
    });
    
    // Update state
    state.currentPage = pageName;
    
    // Load page-specific data
    loadPageData(pageName);
}

/**
 * Load page-specific data
 */
async function loadPageData(pageName) {
    switch (pageName) {
        case 'dashboard':
            await loadDashboardData();
            break;
        case 'lessons':
            await loadLessonsData();
            break;
        case 'vocabulary':
            await loadVocabularyData();
            break;
        case 'exercises':
            // Exercise types are static, no data to load initially
            break;
        case 'progress':
            await loadProgressData();
            break;
    }
}

/**
 * Set up event listeners
 */
function setupEventListeners() {
    // Action cards on dashboard
    document.querySelectorAll('.action-card').forEach(card => {
        card.addEventListener('click', () => {
            const action = card.dataset.action;
            handleQuickAction(action);
        });
    });
    
    // Exercise type cards
    document.querySelectorAll('.exercise-type-card').forEach(card => {
        card.addEventListener('click', () => {
            const type = card.dataset.type;
            startExercise(type);
        });
    });
    
    // Generate lesson button
    const generateLessonBtn = document.getElementById('generate-lesson-btn');
    if (generateLessonBtn) {
        generateLessonBtn.addEventListener('click', generateNewLesson);
    }
    
    // Review vocabulary button
    const reviewVocabBtn = document.getElementById('review-vocab-btn');
    if (reviewVocabBtn) {
        reviewVocabBtn.addEventListener('click', startFlashcardReview);
    }
    
    // Modal backdrop click to close
    const modalBackdrop = document.querySelector('.modal-backdrop');
    if (modalBackdrop) {
        modalBackdrop.addEventListener('click', closeModal);
    }
}

/**
 * Load user data
 */
async function loadUserData() {
    try {
        // For now, use mock data (API will be implemented)
        state.user = {
            id: 'user-1',
            displayName: 'Learner',
            nativeLanguage: 'de',
            targetLanguage: 'fr',
            skillLevel: 'A1'
        };
        
        // Update UI
        updateUserDisplay();
    } catch (error) {
        console.error('Failed to load user data:', error);
        toast.error('Failed to load user data');
    }
}

/**
 * Update user display in navbar
 */
function updateUserDisplay() {
    if (!state.user) return;
    
    document.getElementById('user-name').textContent = state.user.displayName;
    document.getElementById('user-level').textContent = state.user.skillLevel;
    
    // Update language info
    const nativeLang = getLanguageName(state.user.nativeLanguage);
    const targetLang = getLanguageName(state.user.targetLanguage);
    document.getElementById('native-lang').textContent = nativeLang;
    document.getElementById('target-lang').textContent = targetLang;
}

/**
 * Get full language name from code
 */
function getLanguageName(code) {
    const languages = {
        'de': 'German',
        'fr': 'French',
        'en': 'English',
        'es': 'Spanish',
        'it': 'Italian',
        'pt': 'Portuguese',
        'nl': 'Dutch',
        'pl': 'Polish',
        'ru': 'Russian',
        'ja': 'Japanese',
        'zh': 'Chinese',
        'ko': 'Korean'
    };
    return languages[code] || code.toUpperCase();
}

/**
 * Load dashboard data
 */
async function loadDashboardData() {
    showLoading();
    
    try {
        // Load daily goals
        await loadDailyGoals();
        
        // Load recent activity
        await loadRecentActivity();
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Load daily goals
 */
async function loadDailyGoals() {
    // Mock data for now
    const goals = [
        { id: 1, title: 'Complete 3 lessons', current: 1, target: 3, unit: 'lessons' },
        { id: 2, title: 'Learn 10 new words', current: 4, target: 10, unit: 'words' },
        { id: 3, title: 'Practice speaking', current: 0, target: 15, unit: 'minutes' }
    ];
    
    const container = document.getElementById('daily-goals');
    container.innerHTML = goals.map(goal => `
        <div class="goal-card">
            <div class="goal-card-header">
                <span class="goal-card-title">${goal.title}</span>
                <span class="goal-card-type">Daily</span>
            </div>
            <div class="goal-progress">
                <div class="progress-bar">
                    <div class="progress-fill ${goal.current >= goal.target ? 'completed' : ''}" 
                         style="width: ${Math.min(100, (goal.current / goal.target) * 100)}%"></div>
                </div>
                <div class="progress-text">
                    <span>${goal.current} / ${goal.target} ${goal.unit}</span>
                    <span>${Math.round((goal.current / goal.target) * 100)}%</span>
                </div>
            </div>
        </div>
    `).join('');
}

/**
 * Load recent activity
 */
async function loadRecentActivity() {
    // Mock data for now
    const activities = [
        { icon: '📚', title: 'Completed lesson: Greetings', time: '2 hours ago' },
        { icon: '✅', title: 'Scored 90% on vocabulary quiz', time: '3 hours ago' },
        { icon: '🎯', title: 'Reached daily goal: 10 words', time: 'Yesterday' },
        { icon: '🎤', title: 'Speaking practice: 15 minutes', time: 'Yesterday' }
    ];
    
    const container = document.getElementById('recent-activity');
    container.innerHTML = activities.map(activity => `
        <div class="activity-item">
            <span class="activity-icon">${activity.icon}</span>
            <div class="activity-content">
                <div class="activity-title">${activity.title}</div>
                <div class="activity-time">${activity.time}</div>
            </div>
        </div>
    `).join('');
}

/**
 * Load lessons data
 */
async function loadLessonsData() {
    showLoading();
    
    try {
        // Mock data for now
        const lessons = [
            { id: 1, title: 'Basic Greetings', level: 'A1', description: 'Learn essential French greetings and introductions.', duration: '15 min' },
            { id: 2, title: 'Numbers 1-20', level: 'A1', description: 'Master counting in French.', duration: '20 min' },
            { id: 3, title: 'Colors', level: 'A1', description: 'Describe colors in French.', duration: '15 min' }
        ];
        
        const container = document.getElementById('lessons-list');
        container.innerHTML = lessons.map(lesson => `
            <div class="lesson-card" data-lesson-id="${lesson.id}">
                <div class="lesson-card-header">
                    <h3>${lesson.title}</h3>
                    <span class="lesson-card-level">Level ${lesson.level}</span>
                </div>
                <div class="lesson-card-body">
                    <p class="lesson-card-description">${lesson.description}</p>
                    <div class="lesson-card-footer">
                        <span class="lesson-card-meta">⏱️ ${lesson.duration}</span>
                        <button class="btn btn-primary btn-sm">Start</button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load lessons:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Load vocabulary data
 */
async function loadVocabularyData() {
    showLoading();
    
    try {
        // Mock data for now
        const vocabulary = [
            { word: 'Bonjour', pronunciation: '/bɔ̃.ʒuʁ/', translation: 'Hello / Good morning', example: 'Bonjour, comment allez-vous?' },
            { word: 'Merci', pronunciation: '/mɛʁ.si/', translation: 'Thank you', example: 'Merci beaucoup!' },
            { word: 'S\'il vous plaît', pronunciation: '/sil vu plɛ/', translation: 'Please', example: 'Un café, s\'il vous plaît.' }
        ];
        
        const container = document.getElementById('vocabulary-list');
        container.innerHTML = vocabulary.map(vocab => `
            <div class="vocab-card">
                <div class="vocab-word">${vocab.word}</div>
                <div class="vocab-pronunciation">${vocab.pronunciation}</div>
                <div class="vocab-translation">${vocab.translation}</div>
                <div class="vocab-example">${vocab.example}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load vocabulary:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Load progress data
 */
async function loadProgressData() {
    showLoading();
    
    try {
        // Mock data for now
        document.getElementById('total-exercises').textContent = '47';
        document.getElementById('avg-score').textContent = '82%';
        document.getElementById('time-spent').textContent = '12h';
        document.getElementById('current-streak').textContent = '5';
        
        // Load goals
        const goals = [
            { title: 'Reach A2 level', type: 'Monthly', current: 30, target: 100 },
            { title: 'Learn 500 words', type: 'Yearly', current: 127, target: 500 },
            { title: 'Complete 20 lessons', type: 'Monthly', current: 8, target: 20 }
        ];
        
        const container = document.getElementById('goals-list');
        container.innerHTML = goals.map(goal => `
            <div class="goal-card">
                <div class="goal-card-header">
                    <span class="goal-card-title">${goal.title}</span>
                    <span class="goal-card-type">${goal.type}</span>
                </div>
                <div class="goal-progress">
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${(goal.current / goal.target) * 100}%"></div>
                    </div>
                    <div class="progress-text">
                        <span>${goal.current} / ${goal.target}</span>
                        <span>${Math.round((goal.current / goal.target) * 100)}%</span>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load progress:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Handle quick action clicks
 */
function handleQuickAction(action) {
    switch (action) {
        case 'start-lesson':
            navigateTo('lessons');
            break;
        case 'practice-vocabulary':
            navigateTo('vocabulary');
            break;
        case 'speaking-practice':
            startExercise('speaking');
            break;
        case 'roleplay':
            toast.info('Roleplay scenarios coming soon!');
            break;
    }
}

/**
 * Start an exercise
 */
function startExercise(type) {
    const exerciseArea = document.getElementById('exercise-area');
    exerciseArea.classList.remove('hidden');
    
    // Show exercise based on type
    exerciseArea.innerHTML = `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${getExerciseTitle(type)} Exercise</h3>
                <div class="exercise-progress">
                    <div class="exercise-progress-bar">
                        <div class="exercise-progress-fill" style="width: 0%"></div>
                    </div>
                    <span class="exercise-count">1 / 5</span>
                </div>
            </div>
            <p class="exercise-instruction">Loading exercise...</p>
            <div class="exercise-actions">
                <button class="btn btn-secondary" onclick="closeExercise()">Exit</button>
                <button class="btn btn-primary" onclick="checkAnswer()">Check</button>
            </div>
        </div>
    `;
    
    toast.info(`Starting ${getExerciseTitle(type)} exercise...`);
}

/**
 * Get exercise title from type
 */
function getExerciseTitle(type) {
    const titles = {
        'text-completion': 'Fill in the Blanks',
        'drag-drop': 'Word Order',
        'translation': 'Translation',
        'listening': 'Listening',
        'speaking': 'Speaking'
    };
    return titles[type] || type;
}

/**
 * Generate a new lesson
 */
async function generateNewLesson() {
    toast.info('Generating new lesson... This may take a moment.');
    // API call will be implemented
}

/**
 * Start flashcard review
 */
function startFlashcardReview() {
    toast.info('Starting flashcard review...');
    // Flashcard review will be implemented
}

/**
 * Show loading indicator
 */
function showLoading() {
    document.getElementById('loading').classList.remove('hidden');
    state.loading = true;
}

/**
 * Hide loading indicator
 */
function hideLoading() {
    document.getElementById('loading').classList.add('hidden');
    state.loading = false;
}

/**
 * Open modal
 */
function openModal(content) {
    const modal = document.getElementById('modal-container');
    const modalContent = modal.querySelector('.modal-content');
    modalContent.innerHTML = content;
    modal.classList.remove('hidden');
}

/**
 * Close modal
 */
function closeModal() {
    const modal = document.getElementById('modal-container');
    modal.classList.add('hidden');
}

// Global functions for inline handlers
window.closeExercise = function() {
    document.getElementById('exercise-area').classList.add('hidden');
};

window.checkAnswer = function() {
    toast.success('Answer checked!');
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', init);