/**
 * Vocabulary Page Module
 * Handles vocabulary generation and flashcards with session caching.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { renderContent, ContentType } from '../services/content-renderer.js';
import { cache, CacheKeys } from '../services/cache.js';
import { topicsService } from '../services/topics.js';

const { PAGE, CONTENT, FLASHCARDS, TOPIC, COUNT, FLASHCARD_TOPIC } = CacheKeys.VOCABULARY;

/** Grid ID constants for topic suggestions */
const VOCAB_TOPIC_GRID_ID = 'vocab-topic-grid';
const FLASHCARD_TOPIC_GRID_ID = 'flashcard-topic-grid';

/**
 * Initialize vocabulary page UI with cached content restoration.
 */
export function loadVocabularyData(showLoading, hideLoading) {
    showLoading();
    const container = document.getElementById('vocabulary-list');
    if (!container) { hideLoading(); return; }
    
    // Get cached values
    const cachedTopic = cache.get(PAGE, TOPIC) || '';
    const cachedCount = cache.get(PAGE, COUNT) || 10;
    const cachedContent = cache.get(PAGE, CONTENT);
    const cachedFlashcardTopic = cache.get(PAGE, FLASHCARD_TOPIC) || '';
    const cachedFlashcards = cache.get(PAGE, FLASHCARDS);
    
    const hasAnyCache = cachedContent || cachedFlashcards;
    
    // Build topic grid placeholders (will be populated async)
    const topicGridHtml = topicsService.buildTopicGridPlaceholder({
        category: 'vocabulary',
        onSelectFn: 'selectVocabTopic',
        gridId: VOCAB_TOPIC_GRID_ID,
        dividerText: t('vocabulary.orSelectBelow') || t('exercises.orSelectBelow'),
        hintText: t('topics.aiSuggestionsHint'),
        t
    });
    
    const flashcardTopicGridHtml = topicsService.buildTopicGridPlaceholder({
        category: 'vocabulary',
        onSelectFn: 'selectFlashcardTopic',
        gridId: FLASHCARD_TOPIC_GRID_ID,
        dividerText: t('vocabulary.orSelectBelow') || t('exercises.orSelectBelow'),
        hintText: t('topics.aiSuggestionsHint'),
        t
    });
    
    container.innerHTML = `
        <div class="vocab-generator">
            <h3>${t('vocabulary.generateTitle')}</h3>
            <div class="form-row">
                <div class="form-group">
                    <label for="vocab-topic">${t('lessons.topic')}</label>
                    <input type="text" id="vocab-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" value="${escapeHtml(cachedTopic)}" />
                </div>
                <div class="form-group">
                    <label for="vocab-count">${t('vocabulary.words')}</label>
                    <input type="number" id="vocab-count" value="${cachedCount}" min="5" max="20" class="form-input" />
                </div>
            </div>
            <div class="button-row">
                <button class="btn btn-primary" onclick="window.generateVocabulary()">
                    ${t('vocabulary.generate')}
                </button>
                ${hasAnyCache ? `
                    <button class="btn btn-secondary btn-sm" onclick="window.clearVocabularyCache()">
                        ${t('misc.clearCache') || 'Clear All'}
                    </button>
                ` : ''}
            </div>
            ${topicGridHtml}
        </div>
        <div id="generated-vocabulary" class="generated-content ${cachedContent ? '' : 'hidden'}">
            ${cachedContent || ''}
        </div>
        
        <div class="vocab-flashcards-section">
            <h3>${t('vocabulary.flashcardsTitle')}</h3>
            <div class="form-group">
                <label for="flashcard-topic">${t('lessons.topic')}</label>
                <input type="text" id="flashcard-topic" placeholder="${t('lessons.topicPlaceholder')}" class="form-input" value="${escapeHtml(cachedFlashcardTopic)}" />
            </div>
            <button class="btn btn-secondary" onclick="window.generateFlashcards()">
                ${t('vocabulary.generateFlashcards')}
            </button>
            ${flashcardTopicGridHtml}
            <div id="flashcards-container" class="${cachedFlashcards ? '' : 'hidden'}">
                ${cachedFlashcards || ''}
            </div>
        </div>
    `;
    
    // Save form values on input change
    setupInputListeners();
    
    // Initialize topic grids asynchronously
    topicsService.initTopicGrid(VOCAB_TOPIC_GRID_ID, 'vocabulary', 'selectVocabTopic', t);
    topicsService.initTopicGrid(FLASHCARD_TOPIC_GRID_ID, 'vocabulary', 'selectFlashcardTopic', t);
    
    hideLoading();
}

/**
 * Set up input listeners to cache form values.
 */
function setupInputListeners() {
    const vocabTopic = document.getElementById('vocab-topic');
    const vocabCount = document.getElementById('vocab-count');
    const flashcardTopic = document.getElementById('flashcard-topic');
    
    vocabTopic?.addEventListener('input', (e) => {
        cache.save(PAGE, TOPIC, e.target.value);
    });
    
    vocabCount?.addEventListener('input', (e) => {
        cache.save(PAGE, COUNT, parseInt(e.target.value) || 10);
    });
    
    flashcardTopic?.addEventListener('input', (e) => {
        cache.save(PAGE, FLASHCARD_TOPIC, e.target.value);
    });
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
        
        const contentHtml = `
            <div class="vocabulary-content">
                <h3>📖 ${escapeHtml(topic)}</h3>
                <div class="vocabulary-body markdown-content">${renderContent(response.content, response.type || ContentType.VOCABULARY)}</div>
            </div>
        `;
        
        container.innerHTML = contentHtml;
        container.classList.remove('hidden');
        
        // Cache the generated content
        cache.save(PAGE, CONTENT, contentHtml);
        cache.save(PAGE, TOPIC, topic);
        cache.save(PAGE, COUNT, wordCount);
        
        // Update clear button visibility
        updateClearButton();
        
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
        
        const contentHtml = `
            <div class="flashcards-content">
                <h3>🃏 ${escapeHtml(topic)}</h3>
                <div class="flashcards-body">${renderContent(response.content, response.type || ContentType.FLASHCARDS)}</div>
            </div>
        `;
        
        container.innerHTML = contentHtml;
        container.classList.remove('hidden');
        
        // Cache the generated content
        cache.save(PAGE, FLASHCARDS, contentHtml);
        cache.save(PAGE, FLASHCARD_TOPIC, topic);
        
        // Update clear button visibility
        updateClearButton();
        
        toast.success(t('toast.flashcardsGenerated'));
    } catch (error) {
        console.error('Failed to generate flashcards:', error);
        toast.error(t('toast.flashcardsGenerateFailed'));
    } finally {
        hideLoading();
    }
}

/**
 * Clear cached vocabulary content.
 */
export function clearVocabularyCache() {
    cache.clearPage(PAGE);
    
    // Clear the displayed content
    const vocabContainer = document.getElementById('generated-vocabulary');
    if (vocabContainer) {
        vocabContainer.innerHTML = '';
        vocabContainer.classList.add('hidden');
    }
    
    const flashcardsContainer = document.getElementById('flashcards-container');
    if (flashcardsContainer) {
        flashcardsContainer.innerHTML = '';
        flashcardsContainer.classList.add('hidden');
    }
    
    // Clear inputs
    const vocabTopic = document.getElementById('vocab-topic');
    if (vocabTopic) vocabTopic.value = '';
    
    const vocabCount = document.getElementById('vocab-count');
    if (vocabCount) vocabCount.value = '10';
    
    const flashcardTopic = document.getElementById('flashcard-topic');
    if (flashcardTopic) flashcardTopic.value = '';
    
    // Update clear button visibility
    updateClearButton();
    
    toast.info(t('toast.cacheCleared') || 'Cache cleared');
}

/**
 * Update clear button visibility based on cache state.
 */
function updateClearButton() {
    const hasCache = cache.has(PAGE, CONTENT) || cache.has(PAGE, FLASHCARDS);
    const buttonRow = document.querySelector('.vocab-generator .button-row');
    
    if (buttonRow) {
        // Use a specific class to identify the clear button (more reliable than onclick selector)
        let existingClearBtn = buttonRow.querySelector('.vocab-clear-cache-btn');
        
        // Fallback: also check for onclick attribute (for buttons in initial HTML)
        if (!existingClearBtn) {
            existingClearBtn = buttonRow.querySelector('[onclick*="clearVocabularyCache"]');
        }
        
        if (hasCache && !existingClearBtn) {
            const clearBtn = document.createElement('button');
            clearBtn.className = 'btn btn-secondary btn-sm vocab-clear-cache-btn';
            clearBtn.onclick = () => window.clearVocabularyCache();
            clearBtn.textContent = t('misc.clearCache') || 'Clear All';
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

// ============ Topic Selection Helpers ============

/**
 * Handle topic button click for vocabulary section.
 * @param {string} topic - Selected topic
 */
export function selectVocabTopic(topic) {
    const topicInput = document.getElementById('vocab-topic');
    if (topicInput) {
        topicInput.value = topic;
        cache.save(PAGE, TOPIC, topic);
        // Trigger generation
        window.generateVocabulary();
    }
}

/**
 * Handle topic button click for flashcards section.
 * @param {string} topic - Selected topic
 */
export function selectFlashcardTopic(topic) {
    const topicInput = document.getElementById('flashcard-topic');
    if (topicInput) {
        topicInput.value = topic;
        cache.save(PAGE, FLASHCARD_TOPIC, topic);
        // Trigger generation
        window.generateFlashcards();
    }
}

// Register global functions
window.clearVocabularyCache = clearVocabularyCache;
window.selectVocabTopic = selectVocabTopic;
window.selectFlashcardTopic = selectFlashcardTopic;

export default { loadVocabularyData, generateVocabulary, generateFlashcards, clearVocabularyCache, selectVocabTopic, selectFlashcardTopic };
