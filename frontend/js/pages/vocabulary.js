/**
 * Vocabulary Page Module
 * Handles vocabulary generation and flashcards.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { renderContent, ContentType } from '../services/content-renderer.js';

/**
 * Initialize vocabulary page UI.
 */
export function loadVocabularyData(showLoading, hideLoading) {
    showLoading();
    const container = document.getElementById('vocabulary-list');
    if (!container) { hideLoading(); return; }
    
    container.innerHTML = `
        <div class="vocab-generator">
            <h3>${t('vocabulary.generateTitle')}</h3>
            <div class="form-row">
                <div class="form-group">
                    <label for="vocab-topic">${t('lessons.topic')}</label>
                    <input type="text" id="vocab-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" />
                </div>
                <div class="form-group">
                    <label for="vocab-count">${t('vocabulary.words')}</label>
                    <input type="number" id="vocab-count" value="10" min="5" max="20" class="form-input" />
                </div>
            </div>
            <button class="btn btn-primary" onclick="window.generateVocabulary()">
                ${t('vocabulary.generate')}
            </button>
        </div>
        <div id="generated-vocabulary" class="generated-content hidden"></div>
        
        <div class="vocab-flashcards-section">
            <h3>${t('vocabulary.flashcardsTitle')}</h3>
            <div class="form-group">
                <label for="flashcard-topic">${t('lessons.topic')}</label>
                <input type="text" id="flashcard-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" />
            </div>
            <button class="btn btn-secondary" onclick="window.generateFlashcards()">
                ${t('vocabulary.generateFlashcards')}
            </button>
            <div id="flashcards-container" class="hidden"></div>
        </div>
    `;
    hideLoading();
}

/**
 * Generate vocabulary from API.
 * @param {Function} showLoading
 * @param {Function} hideLoading
 * @param {Function} incrementWordsGoal - Callback to increment words goal
 */
export async function generateVocabulary(showLoading, hideLoading, incrementWordsGoal) {
    const topic = document.getElementById('vocab-topic')?.value.trim() || 'common phrases';
    const wordCount = parseInt(document.getElementById('vocab-count')?.value) || 10;
    
    toast.info(t('misc.generating'));
    showLoading();
    
    try {
        const response = await api.content.generateVocabulary(topic, wordCount);
        const container = document.getElementById('generated-vocabulary');
        
        container.innerHTML = `
            <div class="vocabulary-content">
                <h3>📖 ${topic}</h3>
                <div class="vocabulary-body markdown-content">${renderContent(response.content, response.type || ContentType.VOCABULARY)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success(t('toast.vocabGenerated'));
        
        if (incrementWordsGoal) await incrementWordsGoal(wordCount);
    } catch (error) {
        console.error('Failed to generate vocabulary:', error);
        toast.error(t('toast.vocabGenerateFailed'));
    } finally {
        hideLoading();
    }
}

/**
 * Generate flashcards from API.
 */
export async function generateFlashcards(showLoading, hideLoading) {
    const topic = document.getElementById('flashcard-topic')?.value.trim() || 'everyday words';
    
    toast.info(t('misc.generating'));
    showLoading();
    
    try {
        const response = await api.content.generateFlashcards(topic, 10);
        const container = document.getElementById('flashcards-container');
        
        container.innerHTML = `
            <div class="flashcards-content">
                <h3>🃏 ${topic}</h3>
                <div class="flashcards-body">${renderContent(response.content, response.type || ContentType.FLASHCARDS)}</div>
            </div>
        `;
        container.classList.remove('hidden');
        toast.success(t('toast.flashcardsGenerated'));
    } catch (error) {
        console.error('Failed to generate flashcards:', error);
        toast.error(t('toast.flashcardsGenerateFailed'));
    } finally {
        hideLoading();
    }
}

export default { loadVocabularyData, generateVocabulary, generateFlashcards };