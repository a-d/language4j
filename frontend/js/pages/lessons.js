/**
 * Lessons Page Module
 * Handles lesson generation and display with session caching.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { renderContent, ContentType } from '../services/content-renderer.js';
import { cache, CacheKeys } from '../services/cache.js';
import { demoMode } from '../services/demo-mode.js';

const { PAGE, CONTENT, TOPIC } = CacheKeys.LESSONS;

/**
 * Initialize lessons page UI with cached content restoration.
 */
export function loadLessonsData(showLoading, hideLoading) {
    showLoading();
    const container = document.getElementById('lessons-list');
    if (!container) { hideLoading(); return; }
    
    // Get cached values
    const cachedTopic = cache.get(PAGE, TOPIC) || '';
    const cachedContent = cache.get(PAGE, CONTENT);
    
    const isDemoMode = demoMode.isEnabled();
    const topicGridHtml = isDemoMode ? buildTopicGrid() : '';
    
    container.innerHTML = `
        <div class="lesson-generator">
            <h3>${t('lessons.generateTitle')}</h3>
            <div class="form-group">
                <label for="lesson-topic">${t('lessons.topic')}</label>
                <input type="text" id="lesson-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" value="${escapeHtml(cachedTopic)}" ${isDemoMode ? 'list="lesson-topic-suggestions"' : ''} />
                ${isDemoMode ? `
                    <datalist id="lesson-topic-suggestions">
                        ${getAvailableTopics().map(topic => `<option value="${formatTopic(topic)}">`).join('')}
                    </datalist>
                ` : ''}
            </div>
            <div class="button-row">
                <button class="btn btn-primary" onclick="window.generateLesson()">
                    ${t('lessons.generate')}
                </button>
                ${cachedContent ? `
                    <button class="btn btn-secondary btn-sm" onclick="window.clearLessonsCache()">
                        ${t('misc.clearCache') || 'Clear'}
                    </button>
                ` : ''}
            </div>
            ${topicGridHtml}
        </div>
        <div id="generated-lesson" class="generated-content ${cachedContent ? '' : 'hidden'}">
            ${cachedContent || ''}
        </div>
    `;
    
    // Save topic on input change
    const topicInput = document.getElementById('lesson-topic');
    topicInput?.addEventListener('input', (e) => {
        cache.save(PAGE, TOPIC, e.target.value);
    });
    
    hideLoading();
}

/**
 * Generate a new lesson from API.
 * @param {Function} showLoading
 * @param {Function} hideLoading
 * @param {Function} incrementLessonGoal - Callback to increment lesson goal
 */
export async function generateLesson(showLoading, hideLoading, incrementLessonGoal) {
    const topicInput = document.getElementById('lesson-topic');
    const topic = topicInput?.value.trim() || 'basic greetings';
    
    toast.info(t('misc.generating'));
    showLoading();
    
    try {
        const response = await api.content.generateLesson(topic);
        const container = document.getElementById('generated-lesson');
        
        const contentHtml = `
            <div class="lesson-content">
                <div class="lesson-header">
                    <h2>📚 ${escapeHtml(topic)}</h2>
                    <button class="btn btn-sm" data-speak-btn="true" data-playing="false" onclick="window.speakText(this.parentElement.nextElementSibling.innerText, null, this)">
                        ${t('lessons.listen')}
                    </button>
                </div>
                <div class="lesson-body markdown-content">${renderContent(response.content, response.type || ContentType.LESSON)}</div>
            </div>
        `;
        
        container.innerHTML = contentHtml;
        container.classList.remove('hidden');
        
        // Cache the generated content
        cache.save(PAGE, CONTENT, contentHtml);
        cache.save(PAGE, TOPIC, topic);
        
        // Update clear button visibility
        updateClearButton();
        
        toast.success(t('toast.lessonGenerated'));
        
        if (incrementLessonGoal) await incrementLessonGoal();
    } catch (error) {
        console.error('Failed to generate lesson:', error);
        toast.error(t('toast.lessonGenerateFailed'));
    } finally {
        hideLoading();
    }
}

/**
 * Clear cached lessons content.
 */
export function clearLessonsCache() {
    cache.clearPage(PAGE);
    
    // Clear the displayed content
    const container = document.getElementById('generated-lesson');
    if (container) {
        container.innerHTML = '';
        container.classList.add('hidden');
    }
    
    // Clear input
    const topicInput = document.getElementById('lesson-topic');
    if (topicInput) topicInput.value = '';
    
    // Update clear button visibility
    updateClearButton();
    
    toast.info(t('toast.cacheCleared') || 'Cache cleared');
}

/**
 * Update clear button visibility based on cache state.
 */
function updateClearButton() {
    const hasCache = cache.has(PAGE, CONTENT);
    const buttonRow = document.querySelector('.lesson-generator .button-row');
    
    if (buttonRow) {
        const existingClearBtn = buttonRow.querySelector('[onclick*="clearLessonsCache"]');
        
        if (hasCache && !existingClearBtn) {
            const clearBtn = document.createElement('button');
            clearBtn.className = 'btn btn-secondary btn-sm';
            clearBtn.onclick = () => window.clearLessonsCache();
            clearBtn.textContent = t('misc.clearCache') || 'Clear';
            buttonRow.appendChild(clearBtn);
        } else if (!hasCache && existingClearBtn) {
            existingClearBtn.remove();
        }
    }
}

/**
 * Escape HTML special characters.
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============ Demo Mode Topic Selection Helpers ============

/**
 * Get available lesson topics from demo data.
 * @returns {string[]} Array of topic names (lowercase)
 */
function getAvailableTopics() {
    // These match the files in frontend/demo-data/content/lessons/
    return [
        'greetings', 'food', 'travel', 'family', 'shopping',
        'home', 'weather', 'work', 'health', 'hobbies',
        'animals', 'colors', 'clothing', 'technology', 'time'
    ];
}

/**
 * Format topic name for display (capitalize first letter).
 * @param {string} topic - Topic name
 * @returns {string} Formatted topic name
 */
function formatTopic(topic) {
    if (!topic) return '';
    return topic.charAt(0).toUpperCase() + topic.slice(1);
}

/**
 * Get an emoji for a topic.
 * @param {string} topic - Topic name
 * @returns {string} Emoji for the topic
 */
function getTopicEmoji(topic) {
    const emojiMap = {
        'greetings': '👋',
        'food': '🍕',
        'travel': '✈️',
        'family': '👨‍👩‍👧‍👦',
        'shopping': '🛒',
        'home': '🏠',
        'weather': '🌤️',
        'work': '💼',
        'health': '🏥',
        'hobbies': '🎨',
        'animals': '🐾',
        'colors': '🎨',
        'clothing': '👕',
        'technology': '💻',
        'time': '⏰'
    };
    return emojiMap[topic.toLowerCase()] || '📚';
}

/**
 * Build topic grid HTML for demo mode.
 * @returns {string} HTML string
 */
function buildTopicGrid() {
    const topics = getAvailableTopics();
    
    return `
        <div class="topic-divider">
            <span>${t('lessons.orSelectBelow') || t('exercises.orSelectBelow') || 'or select a topic below'}</span>
        </div>
        
        <div class="topic-grid" id="lesson-topic-grid">
            ${topics.map(topic => `
                <button class="topic-btn" data-topic="${topic}" onclick="window.selectLessonTopic('${topic}')">
                    ${getTopicEmoji(topic)} ${formatTopic(topic)}
                </button>
            `).join('')}
        </div>
        
        <p class="demo-mode-hint">
            📴 ${t('lessons.demoModeHint') || t('exercises.demoModeHint') || 'Demo mode: Only pre-generated topics are available'}
        </p>
    `;
}

/**
 * Handle topic button click for lessons.
 * @param {string} topic - Selected topic
 */
export function selectLessonTopic(topic) {
    const topicInput = document.getElementById('lesson-topic');
    if (topicInput) {
        topicInput.value = formatTopic(topic);
        cache.save(PAGE, TOPIC, formatTopic(topic));
        // Trigger generation
        window.generateLesson();
    }
}

// Register global functions
window.clearLessonsCache = clearLessonsCache;
window.selectLessonTopic = selectLessonTopic;

export default { loadLessonsData, generateLesson, clearLessonsCache, selectLessonTopic };
