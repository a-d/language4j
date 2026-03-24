/**
 * Visual Learning Cards Page Module
 * ==================================
 * Handles AI-generated image flashcards for visual vocabulary learning.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t } from '../services/i18n.js';

// State for visual cards
let visualCards = [];
let currentCardIndex = 0;
let isGenerating = false;

// Fallback placeholder image (base64 encoded minimal SVG)
const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48dGV4dCB5PSIuOWVtIiBmb250LXNpemU9IjkwIj7wn5bvPC90ZXh0Pjwvc3ZnPg==';

/**
 * Initialize visual cards page UI.
 * Note: This is synchronous initialization, no loading indicator needed.
 */
export function loadCardsData() {
    const container = document.getElementById('cards-list');
    if (!container) return;
    
    container.innerHTML = `
        <div class="visual-cards-container">
            <!-- Single Word Generator -->
            <div class="card-generator">
                <h3>🖼️ ${t('cards.generateTitle')}</h3>
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
                <button class="btn btn-primary" onclick="window.generateVisualCard()">
                    ${t('cards.generate')}
                </button>
            </div>
            
            <!-- Card Viewer -->
            <div id="card-viewer-section" class="hidden">
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
                        <span>${t('cards.flipCard')}</span>
                    </div>
                </div>
            </div>
            
            <!-- Empty State -->
            <div id="cards-empty-state" class="visual-cards-empty">
                <div class="visual-cards-empty-icon">🖼️</div>
                <div class="visual-cards-empty-text">${t('cards.noCards')}</div>
            </div>
            
            <!-- Batch Generator -->
            <div class="batch-generator">
                <h4>📚 ${t('cards.generateBatch')}</h4>
                <div class="form-group">
                    <label for="batch-words">${t('cards.wordsList')}</label>
                    <textarea id="batch-words" placeholder="${t('cards.wordsListPlaceholder')}"></textarea>
                </div>
                <button class="btn btn-secondary" onclick="window.generateVisualCardsBatch()">
                    ${t('cards.generateBatch')}
                </button>
                <div id="batch-progress" class="batch-progress hidden">
                    <div class="batch-progress-bar">
                        <div class="batch-progress-fill" id="batch-progress-fill" style="width: 0%"></div>
                    </div>
                    <div class="batch-progress-text" id="batch-progress-text"></div>
                </div>
            </div>
            
            <!-- Cards Grid -->
            <div id="cards-grid" class="visual-cards-grid hidden"></div>
        </div>
    `;
    
    // Restore any existing cards
    if (visualCards.length > 0) {
        updateCardDisplay();
        updateCardsGrid();
    }
}

/**
 * Generate a single visual card.
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
    showLoading();
    
    try {
        const response = await api.images.generateFlashcard(word, context);
        
        // Add the card to our collection
        const newCard = {
            word,
            context,
            imageUrl: response.url,
            revisedPrompt: response.revisedPrompt,
            createdAt: new Date().toISOString()
        };
        
        visualCards.push(newCard);
        currentCardIndex = visualCards.length - 1;
        
        // Clear the form
        document.getElementById('card-word').value = '';
        document.getElementById('card-context').value = '';
        
        // Update the display
        updateCardDisplay();
        updateCardsGrid();
        
        toast.success(t('cards.imageGenerated'));
    } catch (error) {
        console.error('Failed to generate visual card:', error);
        toast.error(t('cards.imageGenerateFailed'));
    } finally {
        isGenerating = false;
        hideLoading();
    }
}

/**
 * Generate multiple visual cards in batch using the batch API.
 */
export async function generateVisualCardsBatch(showLoading, hideLoading) {
    if (isGenerating) return;
    
    const wordsInput = document.getElementById('batch-words')?.value.trim();
    if (!wordsInput) {
        toast.warning(t('toast.enterTitle'));
        return;
    }
    
    // Parse comma-separated words
    const words = wordsInput.split(',')
        .map(w => w.trim())
        .filter(w => w.length > 0)
        .slice(0, 5); // Limit to 5 words (API limit)
    
    if (words.length === 0) {
        toast.warning(t('toast.enterTitle'));
        return;
    }
    
    isGenerating = true;
    toast.info(t('cards.generatingBatch'));
    showLoading();
    
    // Show progress
    const progressContainer = document.getElementById('batch-progress');
    const progressFill = document.getElementById('batch-progress-fill');
    const progressText = document.getElementById('batch-progress-text');
    progressContainer?.classList.remove('hidden');
    
    if (progressFill) progressFill.style.width = '50%';
    if (progressText) progressText.textContent = `${t('cards.generatingBatch')} (${words.length} ${t('vocabulary.words').toLowerCase()})`;
    
    try {
        // Use batch API for efficiency
        const requests = words.map(word => ({ word, context: null }));
        const responses = await api.images.generateFlashcardBatch(requests);
        
        // Add all generated cards
        responses.forEach((response, index) => {
            visualCards.push({
                word: words[index],
                context: null,
                imageUrl: response.url,
                revisedPrompt: response.revisedPrompt,
                createdAt: new Date().toISOString()
            });
        });
        
        // Complete
        if (progressFill) progressFill.style.width = '100%';
        if (progressText) progressText.textContent = t('cards.batchComplete');
        
        // Clear the form
        document.getElementById('batch-words').value = '';
        
        // Update display
        currentCardIndex = Math.max(0, visualCards.length - responses.length);
        updateCardDisplay();
        updateCardsGrid();
        
        toast.success(t('cards.batchComplete'));
    } catch (error) {
        console.error('Failed to generate batch:', error);
        toast.error(t('cards.imageGenerateFailed'));
    } finally {
        isGenerating = false;
        hideLoading();
        
        // Hide progress after a delay
        setTimeout(() => {
            progressContainer?.classList.add('hidden');
            if (progressFill) progressFill.style.width = '0%';
        }, 2000);
    }
}

/**
 * Update the card display with current card.
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
    
    if (cardDisplay) {
        cardDisplay.innerHTML = `
            <div class="visual-card" onclick="window.flipCard(this)">
                <div class="visual-card-inner">
                    <div class="visual-card-front">
                        <div class="visual-card-word">${escapeHtml(card.word)}</div>
                        ${card.context ? `<div class="visual-card-context">${escapeHtml(card.context)}</div>` : ''}
                    </div>
                    <div class="visual-card-back">
                        <img src="${card.imageUrl}" alt="${escapeHtml(card.word)}" class="visual-card-image" onerror="window.handleCardImageError(this)" />
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
        <div class="visual-card-thumbnail" onclick="window.viewCard(${index})">
            <img src="${card.imageUrl}" alt="${escapeHtml(card.word)}" class="visual-card-thumbnail-image" onerror="window.handleCardImageError(this)" />
            <div class="visual-card-thumbnail-info">
                <div class="visual-card-thumbnail-word">${escapeHtml(card.word)}</div>
            </div>
        </div>
    `).join('');
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
    updateCardDisplay();
}

/**
 * Navigate to next card.
 */
export function nextCard() {
    if (visualCards.length === 0) return;
    currentCardIndex = (currentCardIndex + 1) % visualCards.length;
    updateCardDisplay();
}

/**
 * View a specific card by index.
 */
export function viewCard(index) {
    if (index >= 0 && index < visualCards.length) {
        currentCardIndex = index;
        updateCardDisplay();
        
        // Scroll to the card viewer
        document.getElementById('card-viewer-section')?.scrollIntoView({ behavior: 'smooth' });
    }
}

/**
 * Escape HTML to prevent XSS.
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
