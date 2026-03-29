/**
 * Visual Learning Cards Page Module
 * ==================================
 * Handles AI-generated image flashcards for visual vocabulary learning.
 * 
 * Card flow:
 * - FRONT: Shows image + word in NATIVE language (what user knows)
 * - BACK (after flip): Shows image + word in TARGET language (what user learns)
 * 
 * Cards are persisted to session storage for navigation persistence.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';
import { cache, CacheKeys } from '../services/cache.js';
import { demoMode } from '../services/demo-mode.js';

const { PAGE, VISUAL_CARDS, CURRENT_INDEX } = CacheKeys.CARDS;

// State for visual cards - initialized from cache
let visualCards = [];
let currentCardIndex = 0;
let isGenerating = false;

// Fallback placeholder image (base64 encoded minimal SVG)
const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48dGV4dCB5PSIuOWVtIiBmb250LXNpemU9IjkwIj7wn5bvPC90ZXh0Pjwvc3ZnPg==';

/**
 * Initialize visual cards page UI.
 * Restores state from session cache.
 */
export function loadCardsData() {
    // Restore state from cache
    restoreFromCache();
    
    const container = document.getElementById('cards-list');
    if (!container) return;
    
    const hasCards = visualCards.length > 0;
    
    container.innerHTML = `
        <div class="visual-cards-container">
            <!-- Topic-Based Generator (Primary) -->
            <div class="card-generator topic-generator">
                <h3>🎯 ${t('cards.generateFromTopic')}</h3>
                <p class="generator-description">${t('cards.topicDescription')}</p>
                <div class="form-row">
                    <div class="form-group" style="flex: 2;">
                        <label for="topic-input">${t('cards.topicLabel')}</label>
                        <input type="text" id="topic-input" placeholder="${t('cards.topicPlaceholder')}" class="form-input" ${demoMode.isEnabled() ? 'list="cards-topic-suggestions"' : ''} />
                        ${demoMode.isEnabled() ? `
                            <datalist id="cards-topic-suggestions">
                                ${getAvailableTopics().map(topic => `<option value="${formatTopic(topic)}">`).join('')}
                            </datalist>
                        ` : ''}
                    </div>
                    <div class="form-group" style="flex: 1;">
                        <label for="card-count">${t('cards.cardCount')}</label>
                        <select id="card-count" class="form-input">
                            <option value="3">3</option>
                            <option value="5" selected>5</option>
                            <option value="7">7</option>
                            <option value="10">10</option>
                        </select>
                    </div>
                </div>
                <div class="button-row">
                    <button class="btn btn-primary" onclick="window.generateVisualCardsFromTopic()">
                        🖼️ ${t('cards.generateFromTopic')}
                    </button>
                    ${hasCards ? `
                        <button class="btn btn-secondary btn-sm" onclick="window.clearCardsCache()">
                            ${t('misc.clearCache') || 'Clear All'} (${visualCards.length})
                        </button>
                    ` : ''}
                </div>
                
                ${demoMode.isEnabled() ? `
                    <div class="topic-divider">
                        <span>${t('cards.orSelectBelow') || t('exercises.orSelectBelow') || 'or select a topic below'}</span>
                    </div>
                    
                    <div class="topic-grid" id="cards-topic-grid">
                        ${getAvailableTopics().map(topic => `
                            <button class="topic-btn" data-topic="${topic}" onclick="window.selectCardsTopic('${topic}')">
                                ${getTopicEmoji(topic)} ${formatTopic(topic)}
                            </button>
                        `).join('')}
                    </div>
                    
                    <p class="demo-mode-hint">
                        📴 ${t('cards.demoModeHint') || t('exercises.demoModeHint') || 'Demo mode: Only pre-generated topics are available'}
                    </p>
                ` : ''}
                
                <div id="generation-progress" class="generation-progress hidden">
                    <div class="generation-progress-spinner"></div>
                    <div class="generation-progress-text" id="generation-progress-text">${t('cards.generating')}</div>
                </div>
            </div>
            
            <!-- Card Viewer -->
            <div id="card-viewer-section" class="${hasCards ? '' : 'hidden'}">
                <div class="visual-card-viewer">
                    <div id="visual-card-display"></div>
                    <div class="card-navigation">
                        <button class="btn btn-secondary" onclick="window.prevCard()">
                            ◀ ${t('cards.prevCard')}
                        </button>
                        <span class="card-counter" id="card-counter"></span>
                        <button class="btn btn-secondary" onclick="window.nextCard()">
                            ${t('cards.nextCard')} ▶
                        </button>
                    </div>
                    <div class="flip-hint">
                        <span class="flip-hint-icon">🔄</span>
                        <span>${t('cards.flipHint')}</span>
                    </div>
                </div>
            </div>
            
            <!-- Empty State -->
            <div id="cards-empty-state" class="visual-cards-empty ${hasCards ? 'hidden' : ''}">
                <div class="visual-cards-empty-icon">🖼️</div>
                <div class="visual-cards-empty-text">${t('cards.noCards')}</div>
            </div>
            
            <!-- Cards Grid -->
            <div id="cards-grid" class="visual-cards-grid ${hasCards ? '' : 'hidden'}"></div>
            
            <!-- Manual/Custom Generator (Collapsed) -->
            <details class="manual-generator-section">
                <summary class="manual-generator-toggle">
                    ⚙️ ${t('cards.customCardTitle')}
                </summary>
                <div class="card-generator manual-generator">
                    <h4>🖼️ ${t('cards.generateTitle')}</h4>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="card-word">${t('cards.word')}</label>
                            <input type="text" id="card-word" placeholder="${t('cards.wordPlaceholder')}" class="form-input" />
                        </div>
                        <div class="form-group">
                            <label for="card-context">${t('cards.context')}</label>
                            <input type="text" id="card-context" placeholder="${t('cards.contextPlaceholder')}" class="form-input" />
                        </div>
                    </div>
                    <button class="btn btn-secondary" onclick="window.generateVisualCard()">
                        ${t('cards.generate')}
                    </button>
                </div>
            </details>
        </div>
    `;
    
    // Restore card display if we have cards
    if (hasCards) {
        updateCardDisplay();
        updateCardsGrid();
    }
}

/**
 * Restore state from session cache.
 */
function restoreFromCache() {
    const cachedCards = cache.get(PAGE, VISUAL_CARDS);
    const cachedIndex = cache.get(PAGE, CURRENT_INDEX);
    
    if (cachedCards && Array.isArray(cachedCards)) {
        visualCards = cachedCards;
        currentCardIndex = typeof cachedIndex === 'number' ? cachedIndex : 0;
        
        // Ensure index is within bounds
        if (currentCardIndex >= visualCards.length) {
            currentCardIndex = Math.max(0, visualCards.length - 1);
        }
    }
}

/**
 * Save current state to session cache.
 */
function saveToCache() {
    cache.save(PAGE, VISUAL_CARDS, visualCards);
    cache.save(PAGE, CURRENT_INDEX, currentCardIndex);
}

/**
 * Generate visual cards from a topic using the new API.
 * This is the primary generation method - it uses LLM to derive words
 * and generates images for bilingual flashcards.
 */
export async function generateVisualCardsFromTopic(showLoading, hideLoading) {
    if (isGenerating) return;
    
    const topic = document.getElementById('topic-input')?.value.trim();
    const cardCount = parseInt(document.getElementById('card-count')?.value || '5', 10);
    
    if (!topic) {
        toast.warning(t('toast.enterTitle'));
        return;
    }
    
    isGenerating = true;
    
    // Show progress indicator
    const progressContainer = document.getElementById('generation-progress');
    const progressText = document.getElementById('generation-progress-text');
    progressContainer?.classList.remove('hidden');
    if (progressText) {
        progressText.textContent = t('cards.generatingFromTopic', { count: cardCount });
    }
    
    toast.info(t('cards.generatingFromTopic', { count: cardCount }));
    showLoading?.();
    
    try {
        // Call the new visual cards API
        const response = await api.content.generateVisualCards(topic, cardCount);
        
        // Transform response to our card format
        const newCards = response.cards.map(card => ({
            nativeWord: card.nativeWord,
            targetWord: card.targetWord,
            imageUrl: card.imageUrl || PLACEHOLDER_IMAGE,
            exampleSentence: card.exampleSentence,
            pronunciation: card.pronunciation,
            topic: response.topic,
            nativeLanguage: response.nativeLanguage,
            targetLanguage: response.targetLanguage,
            createdAt: new Date().toISOString()
        }));
        
        // Add to collection
        visualCards.push(...newCards);
        currentCardIndex = visualCards.length - newCards.length;  // Go to first new card
        
        // Save to cache
        saveToCache();
        
        // Clear the form
        document.getElementById('topic-input').value = '';
        
        // Update the display
        updateCardDisplay();
        updateCardsGrid();
        updateClearButton();
        
        // Show result
        if (response.failedCount > 0) {
            toast.warning(t('cards.partialSuccess', { 
                success: response.cardCount - response.failedCount, 
                total: response.cardCount 
            }));
        } else {
            toast.success(t('cards.topicSuccess', { count: response.cardCount }));
        }
        
    } catch (error) {
        console.error('Failed to generate visual cards:', error);
        if (error.status === 503) {
            toast.error(t('cards.imageServiceUnavailable'));
        } else {
            toast.error(t('cards.topicGenerateFailed'));
        }
    } finally {
        isGenerating = false;
        hideLoading?.();
        progressContainer?.classList.add('hidden');
    }
}

/**
 * Generate a single visual card (legacy/custom mode).
 */
export async function generateVisualCard(showLoading, hideLoading) {
    if (isGenerating) return;
    
    const word = document.getElementById('card-word')?.value.trim();
    const context = document.getElementById('card-context')?.value.trim() || null;
    
    if (!word) {
        toast.warning(t('toast.enterTitle'));
        return;
    }
    
    isGenerating = true;
    toast.info(t('cards.generating'));
    showLoading?.();
    
    try {
        const response = await api.images.generateFlashcard(word, context);
        
        // Add the card to our collection (legacy format - single language)
        const newCard = {
            nativeWord: word,  // For legacy cards, use same word
            targetWord: word,
            imageUrl: response.url,
            context,
            createdAt: new Date().toISOString()
        };
        
        visualCards.push(newCard);
        currentCardIndex = visualCards.length - 1;
        
        // Save to cache
        saveToCache();
        
        // Clear the form
        document.getElementById('card-word').value = '';
        document.getElementById('card-context').value = '';
        
        // Update the display
        updateCardDisplay();
        updateCardsGrid();
        updateClearButton();
        
        toast.success(t('cards.imageGenerated'));
    } catch (error) {
        console.error('Failed to generate visual card:', error);
        toast.error(t('cards.imageGenerateFailed'));
    } finally {
        isGenerating = false;
        hideLoading?.();
    }
}

/**
 * Update the card display with current card.
 * Card shows:
 * - FRONT: Image + Native language word
 * - BACK (flipped): Image + Target language word
 */
function updateCardDisplay() {
    const viewerSection = document.getElementById('card-viewer-section');
    const emptyState = document.getElementById('cards-empty-state');
    const cardDisplay = document.getElementById('visual-card-display');
    const cardCounter = document.getElementById('card-counter');
    
    if (visualCards.length === 0) {
        viewerSection?.classList.add('hidden');
        emptyState?.classList.remove('hidden');
        return;
    }
    
    viewerSection?.classList.remove('hidden');
    emptyState?.classList.add('hidden');
    
    const card = visualCards[currentCardIndex];
    
    // Determine if this is a bilingual card (from topic generation) or legacy single-word card
    const isBilingual = card.nativeWord !== card.targetWord;
    
    if (cardDisplay) {
        cardDisplay.innerHTML = `
            <div class="visual-card bilingual-card" onclick="window.flipCard(this)">
                <div class="visual-card-inner">
                    <div class="visual-card-front">
                        <img src="${card.imageUrl}" alt="${escapeHtml(card.nativeWord)}" class="visual-card-image" onerror="window.handleCardImageError(this)" />
                        <div class="visual-card-content">
                            <div class="visual-card-word native-word">${escapeHtml(card.nativeWord)}</div>
                            ${isBilingual ? `<div class="visual-card-language-hint">${t('cards.yourLanguage')}</div>` : ''}
                        </div>
                    </div>
                    <div class="visual-card-back">
                        <img src="${card.imageUrl}" alt="${escapeHtml(card.targetWord)}" class="visual-card-image" onerror="window.handleCardImageError(this)" />
                        <div class="visual-card-content">
                            <div class="visual-card-word target-word">${escapeHtml(card.targetWord)}</div>
                            ${card.pronunciation ? `<div class="visual-card-pronunciation">${escapeHtml(card.pronunciation)}</div>` : ''}
                            ${card.exampleSentence ? `<div class="visual-card-example">${escapeHtml(card.exampleSentence)}</div>` : ''}
                            ${isBilingual ? `<div class="visual-card-language-hint">${t('cards.targetLanguage')}</div>` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    if (cardCounter) {
        cardCounter.textContent = t('cards.cardOf', { current: currentCardIndex + 1, total: visualCards.length });
    }
}

/**
 * Update the cards grid view.
 */
function updateCardsGrid() {
    const grid = document.getElementById('cards-grid');
    if (!grid) return;
    
    if (visualCards.length === 0) {
        grid.classList.add('hidden');
        return;
    }
    
    grid.classList.remove('hidden');
    grid.innerHTML = visualCards.map((card, index) => `
        <div class="visual-card-thumbnail ${index === currentCardIndex ? 'active' : ''}" onclick="window.viewCard(${index})">
            <img src="${card.imageUrl}" alt="${escapeHtml(card.nativeWord)}" class="visual-card-thumbnail-image" onerror="window.handleCardImageError(this)" />
            <div class="visual-card-thumbnail-info">
                <div class="visual-card-thumbnail-native">${escapeHtml(card.nativeWord)}</div>
                ${card.nativeWord !== card.targetWord ? `
                    <div class="visual-card-thumbnail-target">${escapeHtml(card.targetWord)}</div>
                ` : ''}
            </div>
        </div>
    `).join('');
}

/**
 * Update clear button visibility based on cards state.
 */
function updateClearButton() {
    const hasCards = visualCards.length > 0;
    const buttonRow = document.querySelector('.topic-generator .button-row');
    
    if (buttonRow) {
        const existingClearBtn = buttonRow.querySelector('[onclick*="clearCardsCache"]');
        
        if (hasCards && !existingClearBtn) {
            const clearBtn = document.createElement('button');
            clearBtn.className = 'btn btn-secondary btn-sm';
            clearBtn.onclick = () => window.clearCardsCache();
            clearBtn.textContent = `${t('misc.clearCache') || 'Clear All'} (${visualCards.length})`;
            buttonRow.appendChild(clearBtn);
        } else if (hasCards && existingClearBtn) {
            existingClearBtn.textContent = `${t('misc.clearCache') || 'Clear All'} (${visualCards.length})`;
        } else if (!hasCards && existingClearBtn) {
            existingClearBtn.remove();
        }
    }
}

/**
 * Clear all cached cards.
 */
export function clearCardsCache() {
    visualCards = [];
    currentCardIndex = 0;
    cache.clearPage(PAGE);
    
    // Update display
    updateCardDisplay();
    updateCardsGrid();
    updateClearButton();
    
    toast.info(t('toast.cacheCleared') || 'Cards cleared');
}

/**
 * Handle image load errors by showing a placeholder.
 * @param {HTMLImageElement} imgElement - The image element that failed to load
 */
export function handleCardImageError(imgElement) {
    if (imgElement && imgElement.src !== PLACEHOLDER_IMAGE) {
        imgElement.src = PLACEHOLDER_IMAGE;
        imgElement.alt = 'Image unavailable';
    }
}

/**
 * Flip the current card.
 */
export function flipCard(cardElement) {
    cardElement?.classList.toggle('flipped');
}

/**
 * Navigate to previous card.
 */
export function prevCard() {
    if (visualCards.length === 0) return;
    currentCardIndex = (currentCardIndex - 1 + visualCards.length) % visualCards.length;
    saveToCache();
    updateCardDisplay();
    updateCardsGrid();
}

/**
 * Navigate to next card.
 */
export function nextCard() {
    if (visualCards.length === 0) return;
    currentCardIndex = (currentCardIndex + 1) % visualCards.length;
    saveToCache();
    updateCardDisplay();
    updateCardsGrid();
}

/**
 * View a specific card by index.
 */
export function viewCard(index) {
    if (index >= 0 && index < visualCards.length) {
        currentCardIndex = index;
        saveToCache();
        updateCardDisplay();
        updateCardsGrid();
        
        // Scroll to the card viewer
        document.getElementById('card-viewer-section')?.scrollIntoView({ behavior: 'smooth' });
    }
}

/**
 * Escape HTML to prevent XSS.
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Register global functions for onclick handlers in HTML
window.clearCardsCache = clearCardsCache;
window.generateVisualCardsFromTopic = generateVisualCardsFromTopic;
window.generateVisualCard = generateVisualCard;
window.flipCard = flipCard;
window.prevCard = prevCard;
window.nextCard = nextCard;
window.viewCard = viewCard;
window.handleCardImageError = handleCardImageError;

/**
 * Legacy function for backwards compatibility.
 * Calls the new topic-based generation if a topic is provided,
 * otherwise shows a warning.
 */
export function generateVisualCardsBatch(showLoading, hideLoading) {
    // Check if there's a topic input, use the new flow
    const topicInput = document.getElementById('topic-input');
    if (topicInput && topicInput.value.trim()) {
        return generateVisualCardsFromTopic(showLoading, hideLoading);
    }
    toast.warning(t('cards.topicPlaceholder'));
}

// ============ Demo Mode Topic Selection Helpers ============

/**
 * Get available visual card topics from demo data.
 * @returns {string[]} Array of topic names (lowercase, no extension)
 */
function getAvailableTopics() {
    // These match the files in frontend/demo-data/content/visual-cards/
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
    return emojiMap[topic.toLowerCase()] || '📝';
}

/**
 * Handle topic button click in demo mode.
 * Sets the topic input and triggers generation.
 * @param {string} topic - Selected topic
 */
export function selectCardsTopic(topic) {
    const topicInput = document.getElementById('topic-input');
    if (topicInput) {
        topicInput.value = formatTopic(topic);
        // Trigger generation
        generateVisualCardsFromTopic();
    }
}

// Register global functions for onclick handlers
window.selectCardsTopic = selectCardsTopic;
