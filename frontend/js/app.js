/**
 * Language Learning Platform - Main Application
 * =============================================
 * Vanilla JavaScript frontend for the language learning platform.
 */

import { api } from './api/client.js';
import { toast } from './services/toast.js';

/**
 * Application state management
 */
const state = {
    user: null,
    currentPage: 'dashboard',
    loading: false,
    currentExercise: null,
    exerciseQuestions: [],
    exerciseIndex: 0
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
    document.querySelectorAll('.page').forEach(page => {
        page.classList.add('hidden');
        page.classList.remove('active');
    });
    
    const targetPage = document.getElementById(`${pageName}-page`);
    if (targetPage) {
        targetPage.classList.remove('hidden');
        targetPage.classList.add('active');
    }
    
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === pageName);
    });
    
    state.currentPage = pageName;
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
        state.user = await api.users.getCurrent();
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
        updateUserDisplay();
        toast.error('Backend unavailable - showing offline mode');
    }
}

/**
 * Update user display in navbar
 */
function updateUserDisplay() {
    if (!state.user) return;
    
    document.getElementById('user-name').textContent = state.user.displayName;
    document.getElementById('user-level').textContent = state.user.skillLevel;
    
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
        'de': 'German', 'fr': 'French', 'en': 'English', 'es': 'Spanish',
        'it': 'Italian', 'pt': 'Portuguese', 'nl': 'Dutch', 'pl': 'Polish',
        'ru': 'Russian', 'ja': 'Japanese', 'zh': 'Chinese', 'ko': 'Korean'
    };
    return languages[code] || code.toUpperCase();
}

/**
 * Load dashboard data
 */
async function loadDashboardData() {
    showLoading();
    try {
        await loadDailyGoals();
        await loadRecentActivity();
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Load daily goals from API
 */
async function loadDailyGoals() {
    const container = document.getElementById('daily-goals');
    
    try {
        const goals = await api.goals.list('DAILY');
        
        if (goals.length === 0) {
            container.innerHTML = `
                <p class="empty-state">No daily goals set.</p>
                <button class="btn btn-primary btn-sm" onclick="window.createDefaultGoals()">Create Default Goals</button>
            `;
            return;
        }
        
        container.innerHTML = goals.map(goal => `
            <div class="goal-card" data-goal-id="${goal.id}">
                <div class="goal-card-header">
                    <span class="goal-card-title">${goal.title}</span>
                    <span class="goal-card-type">Daily</span>
                </div>
                <div class="goal-progress">
                    <div class="progress-bar">
                        <div class="progress-fill ${goal.completed ? 'completed' : ''}" 
                             style="width: ${goal.progressPercent}%"></div>
                    </div>
                    <div class="progress-text">
                        <span>${goal.currentValue} / ${goal.targetValue} ${goal.unit}</span>
                        <span>${goal.progressPercent}%</span>
                    </div>
                </div>
                <div class="goal-actions">
                    <button class="btn btn-sm" onclick="window.incrementGoal('${goal.id}')">+1</button>
                    ${!goal.completed ? `<button class="btn btn-sm btn-primary" onclick="window.completeGoal('${goal.id}')">Complete</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load daily goals:', error);
        container.innerHTML = '<p class="empty-state">Failed to load goals. Backend may be unavailable.</p>';
    }
}

/**
 * Load recent activity (would need an activity tracking endpoint in backend)
 */
async function loadRecentActivity() {
    // Note: This would need a backend endpoint to track user activity
    // For now, using placeholder until activity tracking is implemented
    const container = document.getElementById('recent-activity');
    container.innerHTML = `
        <p class="empty-state">Activity tracking coming soon!</p>
        <small>Complete lessons and exercises to see your activity here.</small>
    `;
}

/**
 * Load lessons data - generates lessons dynamically
 */
async function loadLessonsData() {
    showLoading();
    const container = document.getElementById('lessons-list');
    
    container.innerHTML = `
        <div class="lesson-generator">
            <h3>Generate a New Lesson</h3>
            <div class="form-group">
                <label for="lesson-topic">Topic</label>
                <input type="text" id="lesson-topic" placeholder="e.g., greetings, food, travel" class="form-input" />
            </div>
            <button class="btn btn-primary" onclick="window.generateLesson()">
                🎯 Generate Lesson
            </button>
        </div>
        <div id="generated-lesson" class="generated-content hidden"></div>
    `;
    hideLoading();
}

/**
 * Generate a new lesson from API
 */
async function generateLesson() {
    const topicInput = document.getElementById('lesson-topic');
    const topic = topicInput?.value.trim() || 'basic greetings';
    
    toast.info(`Generating lesson on "${topic}"... This may take a moment.`);
    showLoading();
    
    try {
        const response = await api.content.generateLesson(topic);
        const container = document.getElementById('generated-lesson');
        
        // Parse markdown content and display
        container.innerHTML = `
            <div class="lesson-content">
                <div class="lesson-header">
                    <h2>📚 ${topic}</h2>
                    <button class="btn btn-sm" onclick="window.speakText(this.parentElement.nextElementSibling.innerText)">
                        🔊 Listen
                    </button>
                </div>
                <div class="lesson-body markdown-content">${formatMarkdown(response.content)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success('Lesson generated successfully!');
        
        // Increment lessons goal if exists
        await incrementLessonGoal();
    } catch (error) {
        console.error('Failed to generate lesson:', error);
        toast.error('Failed to generate lesson. Please try again.');
    } finally {
        hideLoading();
    }
}

/**
 * Load vocabulary data - generates vocabulary dynamically
 */
async function loadVocabularyData() {
    showLoading();
    const container = document.getElementById('vocabulary-list');
    
    container.innerHTML = `
        <div class="vocab-generator">
            <h3>Generate Vocabulary</h3>
            <div class="form-row">
                <div class="form-group">
                    <label for="vocab-topic">Topic</label>
                    <input type="text" id="vocab-topic" placeholder="e.g., food, colors, animals" class="form-input" />
                </div>
                <div class="form-group">
                    <label for="vocab-count">Words</label>
                    <input type="number" id="vocab-count" value="10" min="5" max="20" class="form-input" />
                </div>
            </div>
            <button class="btn btn-primary" onclick="window.generateVocabulary()">
                📝 Generate Vocabulary
            </button>
        </div>
        <div id="generated-vocabulary" class="generated-content hidden"></div>
        
        <div class="vocab-flashcards-section">
            <h3>Flashcards</h3>
            <div class="form-group">
                <label for="flashcard-topic">Topic</label>
                <input type="text" id="flashcard-topic" placeholder="e.g., common phrases" class="form-input" />
            </div>
            <button class="btn btn-secondary" onclick="window.generateFlashcards()">
                🃏 Generate Flashcards
            </button>
            <div id="flashcards-container" class="hidden"></div>
        </div>
    `;
    hideLoading();
}

/**
 * Generate vocabulary from API
 */
async function generateVocabulary() {
    const topic = document.getElementById('vocab-topic')?.value.trim() || 'common phrases';
    const wordCount = parseInt(document.getElementById('vocab-count')?.value) || 10;
    
    toast.info(`Generating ${wordCount} vocabulary words on "${topic}"...`);
    showLoading();
    
    try {
        const response = await api.content.generateVocabulary(topic, wordCount);
        const container = document.getElementById('generated-vocabulary');
        
        container.innerHTML = `
            <div class="vocabulary-content">
                <h3>📖 ${topic} Vocabulary</h3>
                <div class="vocabulary-body markdown-content">${formatMarkdown(response.content)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success('Vocabulary generated!');
        
        // Increment words goal
        await incrementWordsGoal(wordCount);
    } catch (error) {
        console.error('Failed to generate vocabulary:', error);
        toast.error('Failed to generate vocabulary. Please try again.');
    } finally {
        hideLoading();
    }
}

/**
 * Generate flashcards from API
 */
async function generateFlashcards() {
    const topic = document.getElementById('flashcard-topic')?.value.trim() || 'everyday words';
    
    toast.info(`Generating flashcards on "${topic}"...`);
    showLoading();
    
    try {
        const response = await api.content.generateFlashcards(topic, 10);
        const container = document.getElementById('flashcards-container');
        
        container.innerHTML = `
            <div class="flashcards-content">
                <h3>🃏 Flashcards: ${topic}</h3>
                <div class="flashcards-body markdown-content">${formatMarkdown(response.content)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success('Flashcards generated!');
    } catch (error) {
        console.error('Failed to generate flashcards:', error);
        toast.error('Failed to generate flashcards. Please try again.');
    } finally {
        hideLoading();
    }
}

/**
 * Start an exercise - generates from API
 */
async function startExercise(type) {
    const exerciseArea = document.getElementById('exercise-area');
    exerciseArea.classList.remove('hidden');
    
    const topic = prompt('Enter a topic for the exercise (e.g., greetings, food):') || 'basic vocabulary';
    
    exerciseArea.innerHTML = `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${getExerciseTitle(type)} Exercise</h3>
                <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">✕ Exit</button>
            </div>
            <p class="exercise-instruction">Generating exercise...</p>
        </div>
    `;
    
    showLoading();
    
    try {
        let response;
        switch (type) {
            case 'text-completion':
                response = await api.exercises.generateTextCompletion(topic, 5);
                break;
            case 'drag-drop':
                response = await api.exercises.generateDragDrop(topic, 5);
                break;
            case 'translation':
                response = await api.exercises.generateTranslation(topic, 5);
                break;
            default:
                toast.info(`${type} exercises are coming soon!`);
                closeExercise();
                return;
        }
        
        state.currentExercise = { type, topic, content: response.content };
        
        exerciseArea.innerHTML = `
            <div class="exercise-container">
                <div class="exercise-header">
                    <h3>${getExerciseTitle(type)}: ${topic}</h3>
                    <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">✕ Exit</button>
                </div>
                <div class="exercise-content markdown-content">${formatMarkdown(response.content)}</div>
                <div class="exercise-actions">
                    <button class="btn btn-primary" onclick="window.closeExercise()">Done</button>
                </div>
            </div>
        `;
        
        toast.success('Exercise loaded!');
    } catch (error) {
        console.error('Failed to generate exercise:', error);
        toast.error('Failed to generate exercise. Please try again.');
        closeExercise();
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
        let allGoals = [];
        try {
            allGoals = await api.goals.list();
        } catch (error) {
            console.error('Failed to load goals from API:', error);
        }
        
        // Calculate real stats from goals
        const completedGoals = allGoals.filter(g => g.completed).length;
        const totalProgress = allGoals.reduce((sum, g) => sum + g.currentValue, 0);
        
        document.getElementById('total-exercises').textContent = totalProgress.toString();
        document.getElementById('avg-score').textContent = allGoals.length > 0 
            ? Math.round(allGoals.reduce((sum, g) => sum + g.progressPercent, 0) / allGoals.length) + '%'
            : '0%';
        document.getElementById('time-spent').textContent = '--';
        document.getElementById('current-streak').textContent = completedGoals.toString();
        
        const longTermGoals = allGoals.filter(g => g.type !== 'DAILY');
        const container = document.getElementById('goals-list');
        
        if (longTermGoals.length === 0) {
            container.innerHTML = `
                <p class="empty-state">No long-term goals set yet.</p>
                <button class="btn btn-primary" onclick="window.showCreateGoalModal()">Create Goal</button>
            `;
        } else {
            container.innerHTML = longTermGoals.map(goal => `
                <div class="goal-card" data-goal-id="${goal.id}">
                    <div class="goal-card-header">
                        <span class="goal-card-title">${goal.title}</span>
                        <span class="goal-card-type">${formatGoalType(goal.type)}</span>
                    </div>
                    <div class="goal-progress">
                        <div class="progress-bar">
                            <div class="progress-fill ${goal.completed ? 'completed' : ''}" 
                                 style="width: ${goal.progressPercent}%"></div>
                        </div>
                        <div class="progress-text">
                            <span>${goal.currentValue} / ${goal.targetValue} ${goal.unit}</span>
                            <span>${goal.progressPercent}%</span>
                        </div>
                    </div>
                    <div class="goal-actions">
                        <button class="btn btn-sm" onclick="window.incrementGoal('${goal.id}')">+1</button>
                        <button class="btn btn-sm btn-danger" onclick="window.deleteGoal('${goal.id}')">Delete</button>
                    </div>
                </div>
            `).join('');
        }
    } catch (error) {
        console.error('Failed to load progress:', error);
    } finally {
        hideLoading();
    }
}

// ==================== Goal Management ====================

window.incrementGoal = async function(goalId) {
    try {
        await api.goals.incrementProgress(goalId, 1);
        toast.success('Progress updated!');
        // Reload current page data
        loadPageData(state.currentPage);
    } catch (error) {
        console.error('Failed to increment goal:', error);
        toast.error('Failed to update progress');
    }
};

window.completeGoal = async function(goalId) {
    try {
        await api.goals.complete(goalId);
        toast.success('Goal completed! 🎉');
        loadPageData(state.currentPage);
    } catch (error) {
        console.error('Failed to complete goal:', error);
        toast.error('Failed to complete goal');
    }
};

window.deleteGoal = async function(goalId) {
    if (!confirm('Are you sure you want to delete this goal?')) return;
    
    try {
        await api.goals.delete(goalId);
        toast.success('Goal deleted');
        loadPageData(state.currentPage);
    } catch (error) {
        console.error('Failed to delete goal:', error);
        toast.error('Failed to delete goal');
    }
};

window.createDefaultGoals = async function() {
    // Create some default daily goals
    try {
        await api.goals.create({ title: 'Complete 3 lessons', type: 'DAILY', targetValue: 3, unit: 'lessons' });
        await api.goals.create({ title: 'Learn 10 new words', type: 'DAILY', targetValue: 10, unit: 'words' });
        await api.goals.create({ title: 'Practice 15 minutes', type: 'DAILY', targetValue: 15, unit: 'minutes' });
        toast.success('Default goals created!');
        loadPageData(state.currentPage);
    } catch (error) {
        console.error('Failed to create default goals:', error);
        toast.error('Failed to create goals');
    }
};

// ==================== Speech Functions ====================

window.speakText = async function(text) {
    if (!text) return;
    
    toast.info('Generating audio...');
    
    try {
        const audioBlob = await api.speech.synthesize(text, state.user?.targetLanguage || 'fr', true);
        const audioUrl = URL.createObjectURL(audioBlob);
        const audio = new Audio(audioUrl);
        audio.play();
        toast.success('Playing audio');
    } catch (error) {
        console.error('Failed to generate speech:', error);
        toast.error('Speech synthesis unavailable');
    }
};

// ==================== Helper Functions ====================

async function incrementLessonGoal() {
    try {
        const goals = await api.goals.list('DAILY');
        const lessonGoal = goals.find(g => g.unit === 'lessons' && !g.completed);
        if (lessonGoal) {
            await api.goals.incrementProgress(lessonGoal.id, 1);
        }
    } catch (error) {
        console.error('Failed to increment lesson goal:', error);
    }
}

async function incrementWordsGoal(count) {
    try {
        const goals = await api.goals.list('DAILY');
        const wordsGoal = goals.find(g => g.unit === 'words' && !g.completed);
        if (wordsGoal) {
            await api.goals.incrementProgress(wordsGoal.id, count);
        }
    } catch (error) {
        console.error('Failed to increment words goal:', error);
    }
}

function formatMarkdown(text) {
    if (!text) return '';
    return text
        .replace(/^### (.*$)/gim, '<h4>$1</h4>')
        .replace(/^## (.*$)/gim, '<h3>$1</h3>')
        .replace(/^# (.*$)/gim, '<h2>$1</h2>')
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br>');
}

function formatGoalType(type) {
    const types = { 'DAILY': 'Daily', 'WEEKLY': 'Weekly', 'MONTHLY': 'Monthly', 'YEARLY': 'Yearly' };
    return types[type] || type;
}

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

function handleQuickAction(action) {
    switch (action) {
        case 'start-lesson': navigateTo('lessons'); break;
        case 'practice-vocabulary': navigateTo('vocabulary'); break;
        case 'speaking-practice': startExercise('speaking'); break;
        case 'roleplay': generateRoleplayScenario(); break;
    }
}

async function generateRoleplayScenario() {
    const scenario = prompt('Describe the scenario (e.g., ordering at a restaurant):') || 'casual conversation';
    
    toast.info(`Generating roleplay scenario: "${scenario}"...`);
    showLoading();
    
    try {
        const response = await api.content.generateScenario(scenario);
        
        openModal(`
            <div class="roleplay-scenario">
                <h2>🎭 Roleplay: ${scenario}</h2>
                <div class="scenario-content markdown-content">${formatMarkdown(response.content)}</div>
                <button class="btn btn-primary" onclick="window.closeModal()">Close</button>
            </div>
        `);
        
        toast.success('Scenario generated!');
    } catch (error) {
        console.error('Failed to generate scenario:', error);
        toast.error('Failed to generate scenario');
    } finally {
        hideLoading();
    }
}

function generateNewLesson() {
    navigateTo('lessons');
}

function startFlashcardReview() {
    navigateTo('vocabulary');
}

function showLoading() {
    document.getElementById('loading')?.classList.remove('hidden');
    state.loading = true;
}

function hideLoading() {
    document.getElementById('loading')?.classList.add('hidden');
    state.loading = false;
}

function openModal(content) {
    const modal = document.getElementById('modal-container');
    const modalContent = modal?.querySelector('.modal-content');
    if (modalContent) modalContent.innerHTML = content;
    modal?.classList.remove('hidden');
}

function closeModal() {
    document.getElementById('modal-container')?.classList.add('hidden');
}

function closeExercise() {
    document.getElementById('exercise-area')?.classList.add('hidden');
    state.currentExercise = null;
}

// ==================== Global Functions ====================

window.generateLesson = generateLesson;
window.generateVocabulary = generateVocabulary;
window.generateFlashcards = generateFlashcards;
window.closeExercise = closeExercise;
window.closeModal = closeModal;
window.showCreateGoalModal = function() {
    openModal(`
        <div class="create-goal-form">
            <h2>Create New Goal</h2>
            <div class="form-group">
                <label>Title</label>
                <input type="text" id="goal-title" class="form-input" placeholder="e.g., Learn 100 words" />
            </div>
            <div class="form-group">
                <label>Type</label>
                <select id="goal-type" class="form-input">
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="YEARLY">Yearly</option>
                </select>
            </div>
            <div class="form-group">
                <label>Target</label>
                <input type="number" id="goal-target" class="form-input" value="10" min="1" />
            </div>
            <div class="form-group">
                <label>Unit</label>
                <input type="text" id="goal-unit" class="form-input" placeholder="e.g., words, lessons" />
            </div>
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.closeModal()">Cancel</button>
                <button class="btn btn-primary" onclick="window.submitCreateGoal()">Create</button>
            </div>
        </div>
    `);
};

window.submitCreateGoal = async function() {
    const title = document.getElementById('goal-title')?.value;
    const type = document.getElementById('goal-type')?.value;
    const targetValue = parseInt(document.getElementById('goal-target')?.value) || 10;
    const unit = document.getElementById('goal-unit')?.value || '';
    
    if (!title) {
        toast.error('Please enter a title');
        return;
    }
    
    try {
        await api.goals.create({ title, type, targetValue, unit });
        toast.success('Goal created!');
        closeModal();
        loadPageData(state.currentPage);
    } catch (error) {
        console.error('Failed to create goal:', error);
        toast.error('Failed to create goal');
    }
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', init);