/**
 * Exercises Page Module
 * Handles exercise generation, display, scoring, and history.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { renderContent, ContentType } from '../services/content-renderer.js';
import { topicsService } from '../services/topics.js';
import { demoMode } from '../services/demo-mode.js';

/** Exercise type mapping for backend */
const EXERCISE_TYPE_MAP = {
    'text-completion': 'TEXT_COMPLETION',
    'drag-drop': 'DRAG_DROP',
    'translation': 'TRANSLATION',
    'listening': 'LISTENING',
    'listening-comprehension': 'LISTENING_COMPREHENSION',
    'speaking': 'SPEAKING'
};

/** Map exercise types to feature names for disabled features check */
const EXERCISE_FEATURE_MAP = {
    'listening': 'listening',
    'listening-comprehension': 'listeningComprehension',
    'speaking': 'speaking'
};

/** Current exercise state */
let currentExercise = null;
let exerciseStartTime = null;
let exerciseScores = {
    correct: 0,
    total: 0,
    answers: []
};

/** Audio state for listening/speaking exercises */
let currentAudioUrl = null;
let currentAudio = null;
let mediaRecorder = null;
let audioChunks = [];
let isRecording = false;

/**
 * Initialize the exercises page.
 */
export function initExercisesPage() {
    // Load exercise history on page load
    loadExerciseHistory();
}

/**
 * Start an exercise of the specified type.
 * @param {string} type - Exercise type (text-completion, drag-drop, translation, etc.)
 * @param {Function} showLoading
 * @param {Function} hideLoading
 */
export async function startExercise(type, showLoading, hideLoading) {
    const exerciseArea = document.getElementById('exercise-area');
    if (!exerciseArea) return;
    
    // Check if this exercise type is disabled in demo mode
    const featureName = EXERCISE_FEATURE_MAP[type];
    if (featureName && demoMode.isEnabled() && !demoMode.isFeatureAvailable(featureName)) {
        const exerciseTitles = {
            'listening': t('exercises.listening'),
            'listening-comprehension': t('exercises.listeningComprehension') || '🎧 Listening Comprehension',
            'speaking': t('exercises.speakingExercise')
        };
        
        exerciseArea.classList.remove('hidden');
        exerciseArea.innerHTML = `
            <div class="exercise-container">
                <div class="exercise-header">
                    <h3>${exerciseTitles[type] || type}</h3>
                    <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                </div>
                <div class="demo-mode-notice">
                    <div class="notice-icon">🔇</div>
                    <h4>${t('exercises.notAvailableInDemo') || 'Not Available in Demo Mode'}</h4>
                    <p>${t('exercises.requiresBackend') || 'This exercise type requires the backend server for audio processing.'}</p>
                    <p class="notice-hint">${t('exercises.tryOtherExercises') || 'Try Fill-in-the-Blanks, Word Order, or Translation exercises instead.'}</p>
                </div>
            </div>
        `;
        toast.warning(t('toast.featureDisabledInDemo') || 'This feature is not available in demo mode');
        return;
    }
    
    exerciseArea.classList.remove('hidden');
    
    // Always show topic selector for better UX (with LLM-powered suggestions)
    const topic = await showTopicSelector(exerciseArea, type);
    if (!topic) {
        // User cancelled
        closeExercise();
        return;
    }
    
    const exerciseTitles = {
        'text-completion': t('exercises.fillBlanks'),
        'drag-drop': t('exercises.wordOrder'),
        'translation': t('exercises.translation'),
        'listening': t('exercises.listening'),
        'listening-comprehension': t('exercises.listeningComprehension') || '🎧 Listening Comprehension',
        'speaking': t('exercises.speakingExercise')
    };
    
    exerciseArea.innerHTML = `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${exerciseTitles[type] || type}</h3>
                <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
            </div>
            <p class="exercise-instruction">${t('exercises.generating')}</p>
        </div>
    `;
    
    showLoading();
    
    // Reset score tracking
    exerciseScores = { correct: 0, total: 0, answers: [] };
    exerciseStartTime = Date.now();
    
    try {
        let response;
        const backendType = EXERCISE_TYPE_MAP[type];
        
        if (!backendType) {
            toast.info(`${type} ${t('misc.comingSoon')}`);
            closeExercise();
            return;
        }
        
        // Use unified exercise generation API
        if (type === 'listening-comprehension') {
            // Listening comprehension has special options
            response = await api.exercises.generate(backendType, topic, 1, { wordCount: 100, statementCount: 5 });
        } else {
            response = await api.exercises.generate(backendType, topic, 5);
        }
        
        currentExercise = { type, topic, content: response.content };
        
        // Parse content to get total questions
        let parsedExercises = [];
        try {
            const parsed = JSON.parse(response.content);
            parsedExercises = Array.isArray(parsed) ? parsed : (parsed.exercises || parsed.questions || []);
            exerciseScores.total = parsedExercises.length;
        } catch (e) {
            exerciseScores.total = 5; // Default
        }
        
        const exerciseTypeMap = {
            'text-completion': ContentType.TEXT_COMPLETION,
            'drag-drop': ContentType.DRAG_DROP,
            'translation': ContentType.TRANSLATION,
            'listening': ContentType.LISTENING,
            'listening-comprehension': ContentType.LISTENING_COMPREHENSION,
            'speaking': ContentType.SPEAKING
        };
        
        // Handle listening comprehension - uses content-renderer
        if (type === 'listening-comprehension') {
            // Listening comprehension has a different score structure (statements)
            const parsed = JSON.parse(response.content);
            exerciseScores.total = parsed.statements?.length || 5;
            
            exerciseArea.innerHTML = `
                <div class="exercise-container">
                    <div class="exercise-header">
                        <h3>${exerciseTitles[type]}: ${topic}</h3>
                        <div class="exercise-header-actions">
                            <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                        </div>
                    </div>
                    <div class="exercise-content">${renderContent(response.content, ContentType.LISTENING_COMPREHENSION)}</div>
                </div>
            `;
            toast.success(t('toast.exerciseLoaded'));
        }
        // Handle listening and speaking exercises with custom renderers
        else if (type === 'listening') {
            exerciseArea.innerHTML = renderListeningExercises(parsedExercises, topic, exerciseTitles[type]);
            toast.success(t('toast.exerciseLoaded'));
        } else if (type === 'speaking') {
            exerciseArea.innerHTML = renderSpeakingExercises(parsedExercises, topic, exerciseTitles[type]);
            toast.success(t('toast.exerciseLoaded'));
        } else {
            exerciseArea.innerHTML = `
                <div class="exercise-container">
                    <div class="exercise-header">
                        <h3>${exerciseTitles[type]}: ${topic}</h3>
                        <div class="exercise-header-actions">
                            <span class="exercise-score-display" id="live-score">
                                ${t('exercises.score')}: <strong>0</strong> / ${exerciseScores.total}
                            </span>
                            <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                        </div>
                    </div>
                    <div class="exercise-content">${renderContent(response.content, response.type || exerciseTypeMap[type])}</div>
                    <div class="exercise-footer">
                        <button class="btn btn-primary btn-lg" id="submit-all-btn" onclick="window.submitAllExercises()">
                            ${t('exercises.submitAll')}
                        </button>
                    </div>
                </div>
            `;
            toast.success(t('toast.exerciseLoaded'));
        }
    } catch (error) {
        console.error('Failed to generate exercise:', error);
        toast.error(t('toast.exerciseGenerateFailed'));
        closeExercise();
    } finally {
        hideLoading();
    }
}

/**
 * Update the live score display.
 */
function updateScoreDisplay() {
    const scoreDisplay = document.getElementById('live-score');
    if (scoreDisplay) {
        scoreDisplay.innerHTML = `${t('exercises.score')}: <strong>${exerciseScores.correct}</strong> / ${exerciseScores.total}`;
    }
}

/**
 * Record an answer result.
 * @param {number} index - Question index
 * @param {boolean} isCorrect - Whether the answer was correct
 * @param {string} userAnswer - The user's answer
 * @param {string} correctAnswer - The correct answer
 */
export function recordAnswer(index, isCorrect, userAnswer, correctAnswer) {
    if (isCorrect) {
        exerciseScores.correct++;
    }
    exerciseScores.answers[index] = { isCorrect, userAnswer, correctAnswer };
    updateScoreDisplay();
}

/**
 * Submit all exercises and save the result.
 */
async function submitAllExercises() {
    if (!currentExercise) return;
    
    // Calculate time spent
    const timeSpentSeconds = Math.round((Date.now() - exerciseStartTime) / 1000);
    
    // Calculate score percentage
    const scorePercentage = exerciseScores.total > 0 
        ? Math.round((exerciseScores.correct / exerciseScores.total) * 100)
        : 0;
    
    // Prepare result data
    const resultData = {
        exerciseType: EXERCISE_TYPE_MAP[currentExercise.type] || 'TEXT_COMPLETION',
        exerciseReference: currentExercise.topic,
        score: scorePercentage,
        correctAnswers: exerciseScores.correct,
        totalQuestions: exerciseScores.total,
        timeSpentSeconds: timeSpentSeconds,
        userResponse: JSON.stringify(exerciseScores.answers.map(a => a?.userAnswer || '')),
        correctResponse: JSON.stringify(exerciseScores.answers.map(a => a?.correctAnswer || '')),
        feedback: null
    };
    
    try {
        // Save result to backend
        await api.exercises.saveResult(resultData);
        
        // Show completion summary
        showCompletionSummary(scorePercentage, exerciseScores.correct, exerciseScores.total, timeSpentSeconds);
        
        toast.success(t('toast.exerciseSaved'));
        
        // Reload history
        loadExerciseHistory();
        
    } catch (error) {
        console.error('Failed to save exercise result:', error);
        toast.error(t('toast.exerciseSaveFailed'));
        
        // Still show summary even if save failed
        showCompletionSummary(scorePercentage, exerciseScores.correct, exerciseScores.total, timeSpentSeconds);
    }
}

/**
 * Show the exercise completion summary.
 */
function showCompletionSummary(score, correct, total, timeSeconds) {
    const exerciseArea = document.getElementById('exercise-area');
    if (!exerciseArea) return;
    
    const passed = score >= 70;
    const minutes = Math.floor(timeSeconds / 60);
    const seconds = timeSeconds % 60;
    const timeDisplay = minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
    
    const summaryHtml = `
        <div class="exercise-container">
            <div class="exercise-completion-summary ${passed ? 'passed' : 'needs-practice'}">
                <div class="completion-icon">${passed ? '🎉' : '📚'}</div>
                <h2>${passed ? t('exercises.greatJob') : t('exercises.keepPracticing')}</h2>
                
                <div class="completion-stats">
                    <div class="stat-item">
                        <span class="stat-label">${t('exercises.score')}</span>
                        <span class="stat-value ${passed ? 'text-success' : 'text-warning'}">${score}%</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">${t('exercises.correctAnswers')}</span>
                        <span class="stat-value">${correct} / ${total}</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">${t('exercises.timeSpent')}</span>
                        <span class="stat-value">${timeDisplay}</span>
                    </div>
                </div>
                
                <div class="completion-actions">
                    <button class="btn btn-primary" onclick="window.closeExercise()">
                        ${t('exercises.done')}
                    </button>
                    <button class="btn btn-secondary" onclick="window.retryExercise()">
                        ${t('exercises.tryAgain')}
                    </button>
                </div>
            </div>
        </div>
    `;
    
    exerciseArea.innerHTML = summaryHtml;
}

/**
 * Retry the current exercise with the same topic.
 */
function retryExercise() {
    if (currentExercise) {
        const type = currentExercise.type;
        closeExercise();
        // Delay slightly to allow UI to reset
        setTimeout(() => {
            const showLoading = () => document.getElementById('loading')?.classList.remove('hidden');
            const hideLoading = () => document.getElementById('loading')?.classList.add('hidden');
            startExercise(type, showLoading, hideLoading);
        }, 100);
    }
}

/**
 * Close the current exercise.
 */
export function closeExercise() {
    const exerciseArea = document.getElementById('exercise-area');
    if (exerciseArea) exerciseArea.classList.add('hidden');
    currentExercise = null;
    exerciseStartTime = null;
    exerciseScores = { correct: 0, total: 0, answers: [] };
}

/**
 * Get current exercise state.
 */
export function getCurrentExercise() {
    return currentExercise;
}

/**
 * Load and display exercise history.
 */
async function loadExerciseHistory() {
    const historyContainer = document.getElementById('exercise-history');
    if (!historyContainer) return;
    
    try {
        const results = await api.exercises.getRecentResults(7);
        
        if (!results || results.length === 0) {
            historyContainer.innerHTML = `
                <div class="empty-state">
                    <p>${t('exercises.noHistory')}</p>
                </div>
            `;
            return;
        }
        
        historyContainer.innerHTML = `
            <div class="history-list">
                ${results.map(result => renderHistoryItem(result)).join('')}
            </div>
        `;
        
    } catch (error) {
        console.error('Failed to load exercise history:', error);
        historyContainer.innerHTML = `
            <div class="empty-state">
                <p>${t('exercises.historyLoadFailed')}</p>
            </div>
        `;
    }
}

/**
 * Render a single history item.
 */
function renderHistoryItem(result) {
    const typeNames = {
        'TEXT_COMPLETION': t('exercises.fillBlanks'),
        'DRAG_DROP': t('exercises.wordOrder'),
        'TRANSLATION': t('exercises.translation'),
        'LISTENING': t('exercises.listening'),
        'SPEAKING': t('exercises.speakingExercise')
    };
    
    const typeName = typeNames[result.exerciseType] || result.exerciseType;
    const passed = result.passed;
    const date = new Date(result.createdAt);
    const dateStr = date.toLocaleDateString();
    const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    const minutes = Math.floor(result.timeSpentSeconds / 60);
    const seconds = result.timeSpentSeconds % 60;
    const timeSpent = minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
    
    return `
        <div class="history-item ${passed ? 'passed' : 'failed'}">
            <div class="history-item-icon">${passed ? '✅' : '📝'}</div>
            <div class="history-item-content">
                <div class="history-item-title">${typeName}</div>
                <div class="history-item-details">
                    ${result.exerciseReference ? `<span class="topic">${result.exerciseReference}</span> • ` : ''}
                    <span class="date">${dateStr} ${timeStr}</span>
                </div>
            </div>
            <div class="history-item-stats">
                <div class="score ${passed ? 'text-success' : 'text-warning'}">${result.score}%</div>
                <div class="details">${result.correctAnswers}/${result.totalQuestions} • ${timeSpent}</div>
            </div>
        </div>
    `;
}

// ==================== Listening Exercise Functions ====================

/**
 * Render listening exercises.
 * @param {Array} exercises - Array of listening exercise objects
 * @param {string} topic - The topic
 * @param {string} title - The exercise title
 * @returns {string} HTML string
 */
function renderListeningExercises(exercises, topic, title) {
    if (!exercises || exercises.length === 0) {
        return `<p class="empty-state">${t('exercises.noExercisesGenerated')}</p>`;
    }
    
    return `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${title}: ${escapeHtml(topic)}</h3>
                <div class="exercise-header-actions">
                    <span class="exercise-score-display" id="live-score">
                        ${t('exercises.score')}: <strong>0</strong> / ${exercises.length}
                    </span>
                    <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                </div>
            </div>
            <div class="exercise-content">
                <div class="exercises-list listening-exercises">
                    ${exercises.map((ex, index) => renderListeningItem(ex, index)).join('')}
                </div>
            </div>
            <div class="exercise-footer">
                <button class="btn btn-primary btn-lg" id="submit-all-btn" onclick="window.submitAllExercises()">
                    ${t('exercises.submitAll')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Render a single listening exercise item.
 */
function renderListeningItem(ex, index) {
    const text = ex.text || ex.sentence || '';
    const translation = ex.translation || '';
    const hint = ex.hint || '';
    const difficulty = ex.difficulty || 'medium';
    
    const difficultyBadge = {
        'easy': '🟢 Easy',
        'medium': '🟡 Medium',
        'hard': '🔴 Hard'
    };
    
    return `
        <div class="exercise-item listening-item" data-index="${index}" data-text="${escapeHtml(text)}">
            <div class="exercise-number">${index + 1}</div>
            <div class="exercise-content">
                <div class="listening-exercise">
                    <div class="audio-player">
                        <button class="audio-btn play-btn" onclick="window.playListeningAudio(this, ${index})" data-playing="false">
                            🔊
                        </button>
                        <span class="play-count" id="play-count-${index}">${t('exercises.clickToListen')}</span>
                    </div>
                    ${translation ? `<p class="exercise-translation">📝 ${escapeHtml(translation)}</p>` : ''}
                    ${hint ? `<p class="exercise-hint">💡 ${escapeHtml(hint)}</p>` : ''}
                    <span class="difficulty-badge">${difficultyBadge[difficulty] || difficulty}</span>
                    <input type="text" 
                           class="listening-answer-input exercise-input" 
                           data-answer="${escapeHtml(text)}"
                           placeholder="${t('exercises.typeWhatYouHear')}" />
                    <div class="exercise-feedback hidden"></div>
                </div>
            </div>
            <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkListeningAnswer(this)">${t('exercises.check')}</button>
        </div>
    `;
}

/**
 * Stop any currently playing audio.
 */
function stopCurrentAudio() {
    if (currentAudio) {
        currentAudio.pause();
        currentAudio.currentTime = 0;
        currentAudio = null;
    }
}

/**
 * Play audio for a listening exercise with play/pause toggle.
 */
async function playListeningAudio(btn, index) {
    const item = btn.closest('.listening-item');
    const text = item.dataset.text;
    const playCountEl = document.getElementById(`play-count-${index}`);
    
    // Get user's target language from config or storage
    const languageCode = window.APP_CONFIG?.targetLanguage || 'en';
    
    // Check if this button's audio is already playing - toggle pause
    if (currentAudio && btn.dataset.playing === 'true') {
        if (currentAudio.paused) {
            // Resume playback
            await currentAudio.play();
            btn.textContent = '⏸️';
            playCountEl.textContent = t('exercises.playingAudio');
        } else {
            // Pause playback
            currentAudio.pause();
            btn.textContent = '▶️';
            playCountEl.textContent = t('exercises.paused');
        }
        return;
    }
    
    // Stop any other audio that might be playing
    stopCurrentAudio();
    
    // Reset all other play buttons
    document.querySelectorAll('.audio-btn.play-btn').forEach(otherBtn => {
        if (otherBtn !== btn) {
            otherBtn.textContent = '🔊';
            otherBtn.dataset.playing = 'false';
            otherBtn.disabled = false;
        }
    });
    
    btn.disabled = true;
    btn.textContent = '⏳';
    
    try {
        // Synthesize audio
        const audioBlob = await api.speech.synthesize(text, languageCode, true); // slow = true for learning
        
        // Create audio URL and play
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
        }
        currentAudioUrl = URL.createObjectURL(audioBlob);
        
        currentAudio = new Audio(currentAudioUrl);
        
        currentAudio.onended = () => {
            btn.textContent = '🔊';
            btn.disabled = false;
            btn.dataset.playing = 'false';
            playCountEl.textContent = t('exercises.clickToReplay');
            currentAudio = null;
        };
        
        currentAudio.onerror = () => {
            btn.textContent = '🔊';
            btn.disabled = false;
            btn.dataset.playing = 'false';
            currentAudio = null;
            toast.error(t('toast.audioPlayFailed'));
        };
        
        await currentAudio.play();
        btn.textContent = '⏸️';
        btn.disabled = false;
        btn.dataset.playing = 'true';
        playCountEl.textContent = t('exercises.playingAudio');
        
    } catch (error) {
        console.error('Failed to play audio:', error);
        btn.textContent = '🔊';
        btn.disabled = false;
        btn.dataset.playing = 'false';
        toast.error(t('toast.audioGenerationFailed'));
    }
}

/**
 * Check a listening exercise answer.
 */
function checkListeningAnswer(btn) {
    const item = btn.closest('.listening-item');
    const input = item.querySelector('.listening-answer-input');
    const feedback = item.querySelector('.exercise-feedback');
    const correctAnswer = input.dataset.answer.toLowerCase().trim();
    const userAnswer = input.value.toLowerCase().trim();
    const index = parseInt(item.dataset.index) || 0;
    
    // Calculate similarity for partial credit
    const similarity = calculateStringSimilarity(userAnswer, correctAnswer);
    const isCorrect = similarity > 0.85; // 85% match threshold
    
    feedback.classList.remove('hidden');
    
    if (similarity === 1) {
        feedback.className = 'exercise-feedback correct';
        feedback.innerHTML = '✅ Perfect!';
        input.classList.add('correct');
    } else if (isCorrect) {
        feedback.className = 'exercise-feedback correct';
        feedback.innerHTML = `✅ Close enough! (${Math.round(similarity * 100)}% match)`;
        input.classList.add('correct');
    } else {
        feedback.className = 'exercise-feedback incorrect';
        feedback.innerHTML = `❌ Expected: <strong>${escapeHtml(input.dataset.answer)}</strong>`;
        input.classList.add('incorrect');
    }
    
    btn.disabled = true;
    
    // Record the answer
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isCorrect, input.value, input.dataset.answer);
    }
}

// ==================== Speaking Exercise Functions ====================

/**
 * Render speaking exercises.
 * @param {Array} exercises - Array of speaking exercise objects
 * @param {string} topic - The topic
 * @param {string} title - The exercise title
 * @returns {string} HTML string
 */
function renderSpeakingExercises(exercises, topic, title) {
    if (!exercises || exercises.length === 0) {
        return `<p class="empty-state">${t('exercises.noExercisesGenerated')}</p>`;
    }
    
    return `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${title}: ${escapeHtml(topic)}</h3>
                <div class="exercise-header-actions">
                    <span class="exercise-score-display" id="live-score">
                        ${t('exercises.score')}: <strong>0</strong> / ${exercises.length}
                    </span>
                    <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                </div>
            </div>
            <div class="exercise-content">
                <div class="exercises-list speaking-exercises">
                    ${exercises.map((ex, index) => renderSpeakingItem(ex, index)).join('')}
                </div>
            </div>
            <div class="exercise-footer">
                <button class="btn btn-primary btn-lg" id="submit-all-btn" onclick="window.submitAllExercises()">
                    ${t('exercises.submitAll')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Render a single speaking exercise item.
 */
function renderSpeakingItem(ex, index) {
    const text = ex.text || ex.sentence || '';
    const translation = ex.translation || '';
    const pronunciationTips = ex.pronunciationTips || ex.tips || '';
    const commonMistakes = ex.commonMistakes || [];
    const difficulty = ex.difficulty || 'medium';
    
    const difficultyBadge = {
        'easy': '🟢 Easy',
        'medium': '🟡 Medium',
        'hard': '🔴 Hard'
    };
    
    return `
        <div class="exercise-item speaking-item" data-index="${index}" data-text="${escapeHtml(text)}">
            <div class="exercise-number">${index + 1}</div>
            <div class="exercise-content">
                <div class="speaking-exercise">
                    <div class="target-text">
                        <p class="speak-this">${escapeHtml(text)}</p>
                        ${translation ? `<p class="translation-hint">${escapeHtml(translation)}</p>` : ''}
                    </div>
                    
                    <div class="audio-controls">
                        <button class="audio-btn play-btn" onclick="window.playSpeakingExample(this, ${index})" title="${t('exercises.listenExample')}">
                            🔊
                        </button>
                        <button class="audio-btn record-btn" onclick="window.toggleRecording(this, ${index})" title="${t('exercises.recordYourVoice')}">
                            🎤
                        </button>
                    </div>
                    
                    ${pronunciationTips ? `<p class="exercise-hint">💬 ${escapeHtml(pronunciationTips)}</p>` : ''}
                    ${commonMistakes.length > 0 ? `
                        <p class="common-mistakes">⚠️ Common mistakes: ${commonMistakes.map(m => escapeHtml(m)).join(', ')}</p>
                    ` : ''}
                    <span class="difficulty-badge">${difficultyBadge[difficulty] || difficulty}</span>
                    
                    <div class="transcription hidden" id="transcription-${index}">
                        <span class="transcription-label">${t('exercises.yourSpeech')}:</span>
                        <p class="transcription-text"></p>
                    </div>
                    
                    <div class="exercise-feedback hidden"></div>
                </div>
            </div>
            <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkSpeakingAnswer(this)" disabled>${t('exercises.check')}</button>
        </div>
    `;
}

/**
 * Play example audio for a speaking exercise with play/pause toggle.
 */
async function playSpeakingExample(btn, index) {
    const item = btn.closest('.speaking-item');
    const text = item.dataset.text;
    
    const languageCode = window.APP_CONFIG?.targetLanguage || 'en';
    
    // Check if this button's audio is already playing - toggle pause
    if (currentAudio && btn.dataset.playing === 'true') {
        if (currentAudio.paused) {
            // Resume playback
            await currentAudio.play();
            btn.textContent = '⏸️';
        } else {
            // Pause playback
            currentAudio.pause();
            btn.textContent = '▶️';
        }
        return;
    }
    
    // Stop any other audio that might be playing
    stopCurrentAudio();
    
    // Reset all other play buttons
    document.querySelectorAll('.audio-btn.play-btn').forEach(otherBtn => {
        if (otherBtn !== btn) {
            otherBtn.textContent = '🔊';
            otherBtn.dataset.playing = 'false';
            otherBtn.disabled = false;
        }
    });
    
    btn.disabled = true;
    btn.textContent = '⏳';
    
    try {
        const audioBlob = await api.speech.synthesize(text, languageCode, true);
        
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
        }
        currentAudioUrl = URL.createObjectURL(audioBlob);
        
        currentAudio = new Audio(currentAudioUrl);
        
        currentAudio.onended = () => {
            btn.textContent = '🔊';
            btn.disabled = false;
            btn.dataset.playing = 'false';
            currentAudio = null;
        };
        
        currentAudio.onerror = () => {
            btn.textContent = '🔊';
            btn.disabled = false;
            btn.dataset.playing = 'false';
            currentAudio = null;
            toast.error(t('toast.audioPlayFailed'));
        };
        
        await currentAudio.play();
        btn.textContent = '⏸️';
        btn.disabled = false;
        btn.dataset.playing = 'true';
        
    } catch (error) {
        console.error('Failed to play audio:', error);
        btn.textContent = '🔊';
        btn.disabled = false;
        btn.dataset.playing = 'false';
        toast.error(t('toast.audioGenerationFailed'));
    }
}

/**
 * Toggle audio recording for speaking exercise.
 */
async function toggleRecording(btn, index) {
    const item = btn.closest('.speaking-item');
    
    if (isRecording) {
        // Stop recording
        stopRecording(btn, index);
    } else {
        // Start recording
        await startRecording(btn, index);
    }
}

/**
 * Start audio recording.
 */
async function startRecording(btn, index) {
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];
        
        mediaRecorder.ondataavailable = (event) => {
            audioChunks.push(event.data);
        };
        
        mediaRecorder.onstop = async () => {
            const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
            stream.getTracks().forEach(track => track.stop());
            
            // Transcribe the audio
            await transcribeRecording(audioBlob, index);
        };
        
        mediaRecorder.start();
        isRecording = true;
        btn.classList.add('recording');
        btn.textContent = '⏹️';
        
        toast.info(t('toast.recordingStarted'));
        
    } catch (error) {
        console.error('Failed to start recording:', error);
        toast.error(t('toast.microphoneAccessDenied'));
    }
}

/**
 * Stop audio recording.
 */
function stopRecording(btn, index) {
    if (mediaRecorder && isRecording) {
        mediaRecorder.stop();
        isRecording = false;
        btn.classList.remove('recording');
        btn.textContent = '🎤';
        toast.info(t('toast.recordingStopped'));
    }
}

/**
 * Transcribe recorded audio.
 */
async function transcribeRecording(audioBlob, index) {
    const item = document.querySelector(`.speaking-item[data-index="${index}"]`);
    const transcriptionDiv = document.getElementById(`transcription-${index}`);
    const transcriptionText = transcriptionDiv.querySelector('.transcription-text');
    const checkBtn = item.querySelector('.exercise-check');
    
    const languageHint = window.APP_CONFIG?.targetLanguage || 'en';
    
    toast.info(t('toast.transcribing'));
    
    try {
        const result = await api.speech.transcribe(audioBlob, languageHint);
        
        transcriptionDiv.classList.remove('hidden');
        transcriptionText.textContent = result.transcription || t('exercises.noSpeechDetected');
        
        // Store transcription for checking
        item.dataset.transcription = result.transcription || '';
        
        // Enable check button
        checkBtn.disabled = false;
        
        toast.success(t('toast.transcriptionComplete'));
        
    } catch (error) {
        console.error('Failed to transcribe audio:', error);
        transcriptionDiv.classList.remove('hidden');
        transcriptionText.textContent = t('exercises.transcriptionFailed');
        toast.error(t('toast.transcriptionFailed'));
    }
}

/**
 * Check a speaking exercise answer.
 */
async function checkSpeakingAnswer(btn) {
    const item = btn.closest('.speaking-item');
    const feedback = item.querySelector('.exercise-feedback');
    const expectedText = item.dataset.text;
    const transcription = item.dataset.transcription || '';
    const index = parseInt(item.dataset.index) || 0;
    
    if (!transcription) {
        toast.warning(t('exercises.recordFirst'));
        return;
    }
    
    btn.disabled = true;
    feedback.classList.remove('hidden');
    feedback.innerHTML = `<span class="evaluating">${t('exercises.evaluating')}...</span>`;
    
    try {
        // Use pronunciation evaluation API
        const evaluationResult = await api.exercises.evaluatePronunciation(expectedText, transcription);
        
        let evalData;
        try {
            evalData = JSON.parse(evaluationResult.content);
        } catch (e) {
            evalData = { accuracy: calculateStringSimilarity(transcription.toLowerCase(), expectedText.toLowerCase()) * 100 };
        }
        
        const accuracy = evalData.accuracy || 0;
        const isCorrect = accuracy >= 70;
        
        if (accuracy >= 90) {
            feedback.className = 'exercise-feedback correct';
            feedback.innerHTML = `✅ Excellent pronunciation! (${Math.round(accuracy)}% accuracy)`;
        } else if (isCorrect) {
            feedback.className = 'exercise-feedback correct';
            feedback.innerHTML = `✅ Good job! (${Math.round(accuracy)}% accuracy)`;
        } else {
            feedback.className = 'exercise-feedback incorrect';
            feedback.innerHTML = `❌ Keep practicing. (${Math.round(accuracy)}% accuracy)<br>Expected: <strong>${escapeHtml(expectedText)}</strong>`;
        }
        
        // Add tips if available
        if (evalData.generalTips && evalData.generalTips.length > 0) {
            feedback.innerHTML += `<div class="pronunciation-tips"><strong>Tips:</strong> ${evalData.generalTips.join(', ')}</div>`;
        }
        
        // Record the answer
        if (window.recordExerciseAnswer) {
            window.recordExerciseAnswer(index, isCorrect, transcription, expectedText);
        }
        
    } catch (error) {
        console.error('Failed to evaluate pronunciation:', error);
        
        // Fallback to simple comparison
        const similarity = calculateStringSimilarity(transcription.toLowerCase(), expectedText.toLowerCase());
        const isCorrect = similarity >= 0.7;
        
        if (isCorrect) {
            feedback.className = 'exercise-feedback correct';
            feedback.innerHTML = `✅ Good! (${Math.round(similarity * 100)}% match)`;
        } else {
            feedback.className = 'exercise-feedback incorrect';
            feedback.innerHTML = `❌ Expected: <strong>${escapeHtml(expectedText)}</strong>`;
        }
        
        if (window.recordExerciseAnswer) {
            window.recordExerciseAnswer(index, isCorrect, transcription, expectedText);
        }
    }
}

// ==================== Utility Functions ====================

/**
 * Calculate string similarity using Levenshtein distance.
 */
function calculateStringSimilarity(str1, str2) {
    if (!str1 || !str2) return 0;
    if (str1 === str2) return 1;
    
    const longer = str1.length > str2.length ? str1 : str2;
    const shorter = str1.length > str2.length ? str2 : str1;
    
    if (longer.length === 0) return 1;
    
    const editDistance = levenshteinDistance(longer, shorter);
    return (longer.length - editDistance) / longer.length;
}

/**
 * Levenshtein distance calculation.
 */
function levenshteinDistance(str1, str2) {
    const matrix = [];
    
    for (let i = 0; i <= str2.length; i++) {
        matrix[i] = [i];
    }
    
    for (let j = 0; j <= str1.length; j++) {
        matrix[0][j] = j;
    }
    
    for (let i = 1; i <= str2.length; i++) {
        for (let j = 1; j <= str1.length; j++) {
            if (str2.charAt(i - 1) === str1.charAt(j - 1)) {
                matrix[i][j] = matrix[i - 1][j - 1];
            } else {
                matrix[i][j] = Math.min(
                    matrix[i - 1][j - 1] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j] + 1
                );
            }
        }
    }
    
    return matrix[str2.length][str1.length];
}

/**
 * Escape HTML special characters.
 */
function escapeHtml(text) {
    if (!text) return '';
    const str = String(text);
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return str.replace(/[&<>"']/g, m => map[m]);
}

// ==================== Topic Selection ====================

/**
 * Show topic selector UI with LLM-powered suggestions.
 * Uses the centralized topicsService for dynamic topic suggestions.
 * @param {HTMLElement} exerciseArea - The exercise area container
 * @param {string} type - Exercise type
 * @returns {Promise<string|null>} Selected topic or null if cancelled
 */
function showTopicSelector(exerciseArea, type) {
    const exerciseTitles = {
        'text-completion': t('exercises.fillBlanks'),
        'drag-drop': t('exercises.wordOrder'),
        'translation': t('exercises.translation'),
        'listening': t('exercises.listening'),
        'listening-comprehension': t('exercises.listeningComprehension') || '🎧 Listening Comprehension',
        'speaking': t('exercises.speakingExercise')
    };
    
    // In demo mode, use the demo mode selector (uses static topics)
    if (demoMode.isEnabled()) {
        return demoMode.showDemoTopicSelector(exerciseArea, {
            title: exerciseTitles[type] || type,
            t,
            inputPlaceholder: t('lessons.topicPlaceholder'),
            submitButtonText: t('exercises.start') || 'Start'
        });
    }
    
    // In production mode, use the topics service with LLM suggestions
    return topicsService.showTopicSelector(exerciseArea, {
        title: exerciseTitles[type] || type,
        category: 'exercise',
        t,
        inputPlaceholder: t('lessons.topicPlaceholder'),
        submitButtonText: t('exercises.start') || 'Start'
    });
}

/**
 * Generate exercise with a specific topic (used after demo mode topic selection).
 * @param {string} type - Exercise type
 * @param {string} topic - Selected topic
 * @param {Function} showLoading
 * @param {Function} hideLoading
 */
async function generateExerciseWithTopic(type, topic, showLoading, hideLoading) {
    const exerciseArea = document.getElementById('exercise-area');
    if (!exerciseArea) return;
    
    const exerciseTitles = {
        'text-completion': t('exercises.fillBlanks'),
        'drag-drop': t('exercises.wordOrder'),
        'translation': t('exercises.translation'),
        'listening': t('exercises.listening'),
        'listening-comprehension': t('exercises.listeningComprehension') || '🎧 Listening Comprehension',
        'speaking': t('exercises.speakingExercise')
    };
    
    exerciseArea.innerHTML = `
        <div class="exercise-container">
            <div class="exercise-header">
                <h3>${exerciseTitles[type] || type}</h3>
                <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
            </div>
            <p class="exercise-instruction">${t('exercises.generating')}</p>
        </div>
    `;
    
    showLoading();
    
    // Reset score tracking
    exerciseScores = { correct: 0, total: 0, answers: [] };
    exerciseStartTime = Date.now();
    
    try {
        const backendType = EXERCISE_TYPE_MAP[type];
        const response = await api.exercises.generate(backendType, topic, 5);
        
        currentExercise = { type, topic, content: response.content };
        
        // Parse content to get total questions
        let parsedExercises = [];
        try {
            const parsed = JSON.parse(response.content);
            parsedExercises = Array.isArray(parsed) ? parsed : (parsed.exercises || parsed.questions || []);
            exerciseScores.total = parsedExercises.length;
        } catch (e) {
            exerciseScores.total = 5;
        }
        
        const exerciseTypeMap = {
            'text-completion': ContentType.TEXT_COMPLETION,
            'drag-drop': ContentType.DRAG_DROP,
            'translation': ContentType.TRANSLATION
        };
        
        exerciseArea.innerHTML = `
            <div class="exercise-container">
                <div class="exercise-header">
                    <h3>${exerciseTitles[type]}: ${topic.charAt(0).toUpperCase() + topic.slice(1)}</h3>
                    <div class="exercise-header-actions">
                        <span class="exercise-score-display" id="live-score">
                            ${t('exercises.score')}: <strong>0</strong> / ${exerciseScores.total}
                        </span>
                        <button class="btn btn-sm btn-secondary" onclick="window.closeExercise()">${t('exercises.exit')}</button>
                    </div>
                </div>
                <div class="exercise-content">${renderContent(response.content, response.type || exerciseTypeMap[type])}</div>
                <div class="exercise-footer">
                    <button class="btn btn-primary btn-lg" id="submit-all-btn" onclick="window.submitAllExercises()">
                        ${t('exercises.submitAll')}
                    </button>
                </div>
            </div>
        `;
        toast.success(t('toast.exerciseLoaded'));
        
    } catch (error) {
        console.error('Failed to generate exercise:', error);
        toast.error(t('toast.exerciseGenerateFailed'));
        closeExercise();
    } finally {
        hideLoading();
    }
}

// Register global functions
window.closeExercise = closeExercise;
window.submitAllExercises = submitAllExercises;
window.retryExercise = retryExercise;
window.recordExerciseAnswer = recordAnswer;
window.playListeningAudio = playListeningAudio;
window.checkListeningAnswer = checkListeningAnswer;
window.playSpeakingExample = playSpeakingExample;
window.toggleRecording = toggleRecording;
window.checkSpeakingAnswer = checkSpeakingAnswer;

export default { 
    startExercise, 
    closeExercise, 
    getCurrentExercise, 
    recordAnswer, 
    initExercisesPage 
};
