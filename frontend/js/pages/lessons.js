/**
 * Lessons Page Module
 * Handles lesson generation and display.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { renderContent, ContentType } from '../services/content-renderer.js';

/**
 * Initialize lessons page UI.
 */
export function loadLessonsData(showLoading, hideLoading) {
    showLoading();
    const container = document.getElementById('lessons-list');
    if (!container) { hideLoading(); return; }
    
    container.innerHTML = `
        <div class="lesson-generator">
            <h3>${t('lessons.generateTitle')}</h3>
            <div class="form-group">
                <label for="lesson-topic">${t('lessons.topic')}</label>
                <input type="text" id="lesson-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" />
            </div>
            <button class="btn btn-primary" onclick="window.generateLesson()">
                ${t('lessons.generate')}
            </button>
        </div>
        <div id="generated-lesson" class="generated-content hidden"></div>
    `;
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
        
        container.innerHTML = `
            <div class="lesson-content">
                <div class="lesson-header">
                    <h2>📚 ${topic}</h2>
                    <button class="btn btn-sm" onclick="window.speakText(this.parentElement.nextElementSibling.innerText)">
                        ${t('lessons.listen')}
                    </button>
                </div>
                <div class="lesson-body markdown-content">${renderContent(response.content, response.type || ContentType.LESSON)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success(t('toast.lessonGenerated'));
        
        if (incrementLessonGoal) await incrementLessonGoal();
    } catch (error) {
        console.error('Failed to generate lesson:', error);
        toast.error(t('toast.lessonGenerateFailed'));
    } finally {
        hideLoading();
    }
}

export default { loadLessonsData, generateLesson };