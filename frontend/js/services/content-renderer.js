/**
 * Content Renderer for Language Learning Platform
 * ================================================
 * Renders different content types (Markdown, JSON exercises, flashcards) to HTML.
 */

import { renderMarkdown } from './markdown.js';
import { t } from './i18n.js';

/**
 * Content type constants matching backend response types.
 */
export const ContentType = {
    LESSON: 'lesson',
    VOCABULARY: 'vocabulary',
    FLASHCARDS: 'flashcards',
    SCENARIO: 'scenario',
    LEARNING_PLAN: 'learning-plan',
    TEXT_COMPLETION: 'text-completion',
    DRAG_DROP: 'drag-drop',
    TRANSLATION: 'translation',
    LISTENING: 'listening',
    SPEAKING: 'speaking',
    EVALUATION: 'evaluation',
    PRONUNCIATION_EVALUATION: 'pronunciation-evaluation'
};

/**
 * Markdown-based content types.
 */
const MARKDOWN_TYPES = [
    ContentType.LESSON,
    ContentType.VOCABULARY,
    ContentType.SCENARIO,
    ContentType.LEARNING_PLAN
];

/**
 * JSON-based content types.
 */
const JSON_TYPES = [
    ContentType.FLASHCARDS,
    ContentType.TEXT_COMPLETION,
    ContentType.DRAG_DROP,
    ContentType.TRANSLATION,
    ContentType.EVALUATION
];

/**
 * Renders content based on its type.
 * Automatically detects whether to use Markdown or JSON rendering.
 * 
 * @param {string} content - The raw content string
 * @param {string} type - The content type (from GeneratedContentResponse.type)
 * @returns {string} HTML string
 */
export function renderContent(content, type) {
    if (!content) return '';
    
    if (MARKDOWN_TYPES.includes(type)) {
        return renderMarkdown(content);
    }
    
    if (JSON_TYPES.includes(type)) {
        return renderJsonContent(content, type);
    }
    
    // Fallback: try JSON first, then Markdown
    try {
        const parsed = JSON.parse(content);
        return renderJsonContent(content, type);
    } catch {
        return renderMarkdown(content);
    }
}

/**
 * Renders JSON-based content.
 */
function renderJsonContent(content, type) {
    let data;
    try {
        data = JSON.parse(content);
    } catch (e) {
        console.warn('Failed to parse JSON content, falling back to Markdown:', e);
        return renderMarkdown(content);
    }
    
    switch (type) {
        case ContentType.FLASHCARDS:
            return renderFlashcards(data);
        case ContentType.TEXT_COMPLETION:
            return renderTextCompletionExercises(data);
        case ContentType.DRAG_DROP:
            return renderDragDropExercises(data);
        case ContentType.TRANSLATION:
            return renderTranslationExercises(data);
        case ContentType.EVALUATION:
            return renderEvaluation(data);
        default:
            // Generic JSON display
            return renderGenericJson(data);
    }
}

// ==================== Flashcards Renderer ====================

/**
 * Renders flashcards as interactive cards with flip effect.
 * 
 * Supports multiple data formats from the backend:
 * 
 * Format 1 (Backend LLM response):
 * { flashcards: [{ front: string, back: { translation, pronunciation, example, exampleTranslation }, mnemonic?, imagePrompt? }] }
 * 
 * Format 2 (Simple format):
 * { cards: [{ front: string, back: string, example?: string }] }
 * 
 * Format 3 (Array only):
 * [{ front: string, back: string|object, ... }]
 */
function renderFlashcards(data) {
    const cards = Array.isArray(data) ? data : (data.cards || data.flashcards || []);
    
    if (cards.length === 0) {
        return '<p class="empty-state">No flashcards generated.</p>';
    }
    
    return `
        <div class="flashcards-deck" data-current="0">
            <div class="flashcards-progress">
                <span class="flashcard-counter">1 / ${cards.length}</span>
            </div>
            <div class="flashcards-container">
                ${cards.map((card, index) => renderSingleFlashcard(card, index)).join('')}
            </div>
            <div class="flashcards-controls">
                <button class="btn btn-secondary flashcard-prev" onclick="window.flashcardPrev(this)" disabled>← Previous</button>
                <button class="btn btn-primary flashcard-flip" onclick="window.flashcardFlip(this)">🔄 Flip Card</button>
                <button class="btn btn-secondary flashcard-next" onclick="window.flashcardNext(this)" ${cards.length <= 1 ? 'disabled' : ''}>Next →</button>
            </div>
            <div class="flashcard-hint">
                <span>💡 Click on the card or press the Flip button to reveal the answer</span>
            </div>
        </div>
    `;
}

/**
 * Renders a single flashcard, handling nested back object structure.
 * @param {Object} card - The flashcard data
 * @param {number} index - The card index
 * @returns {string} HTML string for the card
 */
function renderSingleFlashcard(card, index) {
    // Extract front text (target language word/phrase)
    const frontText = card.front || card.word || card.term || '';
    
    // Handle nested back object or simple string
    let backTranslation = '';
    let backPronunciation = '';
    let backExample = '';
    let backExampleTranslation = '';
    let mnemonic = card.mnemonic || '';
    
    if (card.back && typeof card.back === 'object') {
        // Nested structure from LLM: { translation, pronunciation, example, exampleTranslation }
        backTranslation = card.back.translation || '';
        backPronunciation = card.back.pronunciation || card.pronunciation || '';
        backExample = card.back.example || '';
        backExampleTranslation = card.back.exampleTranslation || '';
    } else {
        // Simple string format
        backTranslation = card.back || card.translation || card.definition || '';
        backPronunciation = card.pronunciation || '';
        backExample = card.example || '';
        backExampleTranslation = card.exampleTranslation || '';
    }
    
    return `
        <div class="flashcard ${index === 0 ? 'active' : ''}" data-index="${index}" onclick="window.flashcardFlipCard(this)">
            <div class="flashcard-inner">
                <div class="flashcard-front">
                    <span class="flashcard-label">🎯 Target Language</span>
                    <p class="flashcard-text">${escapeHtml(frontText)}</p>
                    ${backPronunciation ? `<p class="flashcard-pronunciation">🔊 ${escapeHtml(backPronunciation)}</p>` : ''}
                </div>
                <div class="flashcard-back">
                    <span class="flashcard-label">📖 Translation</span>
                    <p class="flashcard-text">${escapeHtml(backTranslation)}</p>
                    ${backExample ? `
                        <div class="flashcard-example-section">
                            <p class="flashcard-example"><strong>Example:</strong> <em>${escapeHtml(backExample)}</em></p>
                            ${backExampleTranslation ? `<p class="flashcard-example-translation">${escapeHtml(backExampleTranslation)}</p>` : ''}
                        </div>
                    ` : ''}
                    ${mnemonic ? `<p class="flashcard-mnemonic">💡 <em>${escapeHtml(mnemonic)}</em></p>` : ''}
                </div>
            </div>
        </div>
    `;
}

// ==================== Text Completion (Fill-in-the-blank) Renderer ====================

/**
 * Renders fill-in-the-blank exercises.
 * Expected data format:
 * { exercises: [{ sentence: string, blank: string, answer: string, hint?: string }] }
 * or
 * [{ sentence: string, blank: string, answer: string }]
 */
function renderTextCompletionExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || data.questions || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No exercises generated.</p>';
    }
    
    return `
        <div class="exercises-list text-completion-exercises">
            ${exercises.map((ex, index) => {
                const sentence = ex.sentence || ex.text || ex.question || '';
                const answer = ex.answer || ex.blank || ex.correct || '';
                const hint = ex.hint || '';
                
                // Replace blank marker with input field
                const displaySentence = sentence.replace(/_{2,}|\[\.\.\.\]|\[blank\]|\{\{blank\}\}/gi, 
                    `<input type="text" class="exercise-input" data-answer="${escapeHtml(answer)}" placeholder="..." />`
                );
                
                return `
                    <div class="exercise-item" data-index="${index}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <p class="exercise-sentence">${displaySentence}</p>
                            ${hint ? `<p class="exercise-hint">💡 ${escapeHtml(hint)}</p>` : ''}
                            <div class="exercise-feedback hidden"></div>
                        </div>
                        <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkExerciseAnswer(this)">Check</button>
                    </div>
                `;
            }).join('')}
            <div class="exercises-summary hidden">
                <p class="summary-text"></p>
            </div>
        </div>
    `;
}

// ==================== Drag & Drop (Word Order) Renderer ====================

/**
 * Renders word-ordering exercises with improved visualization.
 * 
 * Expected data formats:
 * Format 1: { exercises: [{ scrambledWords: string[], correctOrder: string[], translation: string, explanation: string }] }
 * Format 2: { exercises: [{ words: string[], correctOrder: string[], translation?: string }] }
 * Format 3: [{ shuffled: string[], correct: string }]
 */
function renderDragDropExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || data.sentences || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No exercises generated.</p>';
    }
    
    return `
        <div class="drag-drop-container">
            <div class="drag-drop-instructions">
                <span class="instruction-icon">💡</span>
                <span>Click on words to add them to the sentence. Click on words in the sentence to remove them.</span>
            </div>
            <div class="exercises-list drag-drop-exercises">
                ${exercises.map((ex, index) => renderSingleDragDropExercise(ex, index)).join('')}
            </div>
        </div>
    `;
}

/**
 * Renders a single drag-drop exercise with proper data extraction.
 */
function renderSingleDragDropExercise(ex, index) {
    // Handle different data formats for words
    let words = ex.scrambledWords || ex.words || ex.shuffled || [];
    let correctSentence = '';
    
    // Extract correct sentence
    if (ex.correctOrder) {
        correctSentence = Array.isArray(ex.correctOrder) ? ex.correctOrder.join(' ') : ex.correctOrder;
    } else if (ex.correct) {
        correctSentence = ex.correct;
    } else if (ex.sentence) {
        correctSentence = ex.sentence;
    }
    
    // If scrambledWords is an array, use it; otherwise shuffle from correct
    if (words.length === 0 && correctSentence) {
        words = shuffleArray(correctSentence.split(/\s+/));
    }
    
    // Extract translation (must be in native language)
    const translation = ex.translation || ex.hint || '';
    
    // Extract explanation (grammar tip)
    const explanation = ex.explanation || ex.grammarNote || '';
    
    return `
        <div class="exercise-item drag-drop-item" data-index="${index}" data-correct="${escapeHtml(correctSentence)}">
            <div class="exercise-number">${index + 1}</div>
            <div class="exercise-content">
                <div class="drag-drop-header">
                    ${translation ? `
                        <div class="drag-drop-translation">
                            <span class="translation-label">🎯 Translate this:</span>
                            <p class="translation-text">${escapeHtml(translation)}</p>
                        </div>
                    ` : `
                        <div class="drag-drop-instruction">
                            <span class="instruction-label">📝 Arrange the words to form a correct sentence</span>
                        </div>
                    `}
                </div>
                
                <div class="word-bank-section">
                    <div class="word-bank-label">Available words:</div>
                    <div class="word-bank" id="word-bank-${index}">
                        ${words.map((word, wordIndex) => `
                            <span class="draggable-word" 
                                  data-word="${escapeHtml(word)}" 
                                  data-original-index="${wordIndex}"
                                  onclick="window.toggleWordSelection(this)">
                                ${escapeHtml(word)}
                            </span>
                        `).join('')}
                    </div>
                </div>
                
                <div class="sentence-builder-section">
                    <div class="sentence-builder-label">
                        <span>Your sentence:</span>
                        <button class="btn btn-sm btn-secondary clear-sentence-btn" onclick="window.clearSentence(this)" title="Clear all words">
                            🔄 Clear
                        </button>
                    </div>
                    <div class="drop-zone" id="drop-zone-${index}">
                        <span class="drop-placeholder">👆 Click words above to build the sentence...</span>
                    </div>
                </div>
                
                ${explanation ? `
                    <div class="grammar-hint hidden" id="grammar-hint-${index}">
                        <span class="grammar-icon">📚</span>
                        <span class="grammar-text">${escapeHtml(explanation)}</span>
                    </div>
                ` : ''}
                
                <div class="exercise-feedback hidden"></div>
            </div>
            <div class="exercise-actions">
                <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkDragDropAnswer(this)">✓ Check Answer</button>
            </div>
        </div>
    `;
}

// ==================== Translation Renderer ====================

/**
 * Renders translation exercises.
 * Expected data format:
 * { exercises: [{ source: string, target?: string, hint?: string }] }
 */
function renderTranslationExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || data.sentences || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No exercises generated.</p>';
    }
    
    return `
        <div class="exercises-list translation-exercises">
            ${exercises.map((ex, index) => {
                const source = ex.source || ex.sentence || ex.original || '';
                const target = ex.target || ex.translation || ex.answer || '';
                const hint = ex.hint || ex.vocabulary || '';
                
                return `
                    <div class="exercise-item translation-item" data-index="${index}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <p class="exercise-source"><strong>Translate:</strong> ${escapeHtml(source)}</p>
                            ${hint ? `<p class="exercise-hint">💡 ${escapeHtml(typeof hint === 'object' ? JSON.stringify(hint) : hint)}</p>` : ''}
                            <textarea class="exercise-textarea" data-answer="${escapeHtml(target)}" placeholder="Type your translation here..." rows="2"></textarea>
                            <div class="exercise-feedback hidden"></div>
                        </div>
                        <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkTranslationAnswer(this)">Check</button>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

// ==================== Evaluation Renderer ====================

/**
 * Renders evaluation results.
 * Expected data format:
 * { correct: boolean, score?: number, feedback: string, corrections?: string[] }
 */
function renderEvaluation(data) {
    const isCorrect = data.correct || data.isCorrect || false;
    const score = data.score || data.percentage || null;
    const feedback = data.feedback || data.message || '';
    const corrections = data.corrections || data.suggestions || [];
    
    return `
        <div class="evaluation-result ${isCorrect ? 'correct' : 'incorrect'}">
            <div class="evaluation-header">
                <span class="evaluation-icon">${isCorrect ? '✅' : '❌'}</span>
                <span class="evaluation-status">${isCorrect ? 'Correct!' : 'Not quite right'}</span>
                ${score !== null ? `<span class="evaluation-score">${score}%</span>` : ''}
            </div>
            ${feedback ? `<p class="evaluation-feedback">${escapeHtml(feedback)}</p>` : ''}
            ${corrections.length > 0 ? `
                <div class="evaluation-corrections">
                    <strong>Suggestions:</strong>
                    <ul>
                        ${corrections.map(c => `<li>${escapeHtml(c)}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
        </div>
    `;
}

// ==================== Generic JSON Renderer ====================

/**
 * Renders generic JSON data in a readable format.
 */
function renderGenericJson(data) {
    return `<pre class="json-display"><code>${escapeHtml(JSON.stringify(data, null, 2))}</code></pre>`;
}

// ==================== Utility Functions ====================

/**
 * Escapes HTML special characters.
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

/**
 * Shuffles an array using Fisher-Yates algorithm.
 */
function shuffleArray(array) {
    const shuffled = [...array];
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
}

// ==================== Interactive Functions (Global) ====================

/**
 * Flashcard navigation and flip functions.
 */
window.flashcardFlip = function(btn) {
    const deck = btn.closest('.flashcards-deck');
    const currentCard = deck.querySelector('.flashcard.active');
    if (currentCard) {
        currentCard.classList.toggle('flipped');
    }
};

/**
 * Flip a specific flashcard when clicked.
 */
window.flashcardFlipCard = function(cardElement) {
    if (cardElement && cardElement.classList.contains('active')) {
        cardElement.classList.toggle('flipped');
    }
};

window.flashcardNext = function(btn) {
    const deck = btn.closest('.flashcards-deck');
    const cards = deck.querySelectorAll('.flashcard');
    let current = parseInt(deck.dataset.current) || 0;
    
    if (current < cards.length - 1) {
        cards[current].classList.remove('active', 'flipped');
        current++;
        cards[current].classList.add('active');
        deck.dataset.current = current;
        updateFlashcardControls(deck, current, cards.length);
    }
};

window.flashcardPrev = function(btn) {
    const deck = btn.closest('.flashcards-deck');
    const cards = deck.querySelectorAll('.flashcard');
    let current = parseInt(deck.dataset.current) || 0;
    
    if (current > 0) {
        cards[current].classList.remove('active', 'flipped');
        current--;
        cards[current].classList.add('active');
        deck.dataset.current = current;
        updateFlashcardControls(deck, current, cards.length);
    }
};

function updateFlashcardControls(deck, current, total) {
    deck.querySelector('.flashcard-counter').textContent = `${current + 1} / ${total}`;
    deck.querySelector('.flashcard-prev').disabled = current === 0;
    deck.querySelector('.flashcard-next').disabled = current === total - 1;
}

/**
 * Text completion exercise check.
 */
window.checkExerciseAnswer = function(btn) {
    const item = btn.closest('.exercise-item');
    const input = item.querySelector('.exercise-input');
    const feedback = item.querySelector('.exercise-feedback');
    const answer = input.dataset.answer.toLowerCase().trim();
    const userAnswer = input.value.toLowerCase().trim();
    const index = parseInt(item.dataset.index) || 0;
    
    const isCorrect = userAnswer === answer;
    
    feedback.classList.remove('hidden');
    
    if (isCorrect) {
        feedback.className = 'exercise-feedback correct';
        feedback.innerHTML = '✅ Correct!';
        input.classList.add('correct');
    } else {
        feedback.className = 'exercise-feedback incorrect';
        feedback.innerHTML = `❌ The correct answer is: <strong>${escapeHtml(input.dataset.answer)}</strong>`;
        input.classList.add('incorrect');
    }
    
    btn.disabled = true;
    
    // Record the answer for score tracking
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isCorrect, input.value, input.dataset.answer);
    }
};

/**
 * Word selection for drag-drop exercises.
 */
window.toggleWordSelection = function(wordEl) {
    const item = wordEl.closest('.drag-drop-item');
    const dropZone = item.querySelector('.drop-zone');
    
    if (wordEl.classList.contains('selected')) {
        // Remove from drop zone
        wordEl.classList.remove('selected');
        const inZone = dropZone.querySelector(`[data-word="${wordEl.dataset.word}"]`);
        if (inZone) inZone.remove();
    } else {
        // Add to drop zone
        wordEl.classList.add('selected');
        const clone = document.createElement('span');
        clone.className = 'dropped-word';
        clone.dataset.word = wordEl.dataset.word;
        clone.textContent = wordEl.dataset.word;
        clone.onclick = () => {
            wordEl.classList.remove('selected');
            clone.remove();
            updateDropZonePlaceholder(dropZone);
        };
        
        // Remove placeholder if present
        const placeholder = dropZone.querySelector('.drop-placeholder');
        if (placeholder) placeholder.remove();
        
        dropZone.appendChild(clone);
    }
    
    updateDropZonePlaceholder(dropZone);
};

window.handleDropZoneClick = function(dropZone) {
    // Do nothing, clicks on dropped words are handled by their own onclick
};

function updateDropZonePlaceholder(dropZone) {
    const hasWords = dropZone.querySelectorAll('.dropped-word').length > 0;
    let placeholder = dropZone.querySelector('.drop-placeholder');
    
    if (!hasWords && !placeholder) {
        placeholder = document.createElement('span');
        placeholder.className = 'drop-placeholder';
        placeholder.textContent = 'Click words above to build the sentence...';
        dropZone.appendChild(placeholder);
    } else if (hasWords && placeholder) {
        placeholder.remove();
    }
}

/**
 * Clear all words from the sentence builder in drag-drop exercise.
 */
window.clearSentence = function(btn) {
    const item = btn.closest('.drag-drop-item');
    const dropZone = item.querySelector('.drop-zone');
    const wordBank = item.querySelector('.word-bank');
    
    // Remove all dropped words
    const droppedWords = dropZone.querySelectorAll('.dropped-word');
    droppedWords.forEach(droppedWord => {
        droppedWord.remove();
    });
    
    // Reset all word bank words
    const allWords = wordBank.querySelectorAll('.draggable-word');
    allWords.forEach(word => {
        word.classList.remove('selected');
    });
    
    // Restore placeholder
    updateDropZonePlaceholder(dropZone);
};

/**
 * Drag-drop exercise check with grammar hint support.
 */
window.checkDragDropAnswer = function(btn) {
    const item = btn.closest('.drag-drop-item');
    const dropZone = item.querySelector('.drop-zone');
    const feedback = item.querySelector('.exercise-feedback');
    const correctAnswer = item.dataset.correct.toLowerCase().trim();
    const index = parseInt(item.dataset.index) || 0;
    
    // Get user's answer
    const droppedWords = Array.from(dropZone.querySelectorAll('.dropped-word'))
        .map(w => w.dataset.word)
        .join(' ')
        .toLowerCase()
        .trim();
    
    const userAnswer = Array.from(dropZone.querySelectorAll('.dropped-word'))
        .map(w => w.dataset.word)
        .join(' ');
    
    const isCorrect = droppedWords === correctAnswer;
    
    feedback.classList.remove('hidden');
    
    if (isCorrect) {
        feedback.className = 'exercise-feedback correct';
        feedback.innerHTML = '✅ Correct! Well done!';
        dropZone.classList.add('correct');
    } else {
        feedback.className = 'exercise-feedback incorrect';
        feedback.innerHTML = `❌ Not quite. The correct sentence is: <strong>${escapeHtml(item.dataset.correct)}</strong>`;
        dropZone.classList.add('incorrect');
    }
    
    // Show grammar hint if available (after checking)
    const grammarHint = item.querySelector('.grammar-hint');
    if (grammarHint) {
        grammarHint.classList.remove('hidden');
    }
    
    // Disable check button and clear button
    btn.disabled = true;
    const clearBtn = item.querySelector('.clear-sentence-btn');
    if (clearBtn) clearBtn.disabled = true;
    
    // Record the answer for score tracking
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isCorrect, userAnswer, item.dataset.correct);
    }
};

/**
 * Translation exercise check.
 */
window.checkTranslationAnswer = function(btn) {
    const item = btn.closest('.translation-item');
    const textarea = item.querySelector('.exercise-textarea');
    const feedback = item.querySelector('.exercise-feedback');
    const correctAnswer = textarea.dataset.answer.toLowerCase().trim();
    const userAnswer = textarea.value.toLowerCase().trim();
    const index = parseInt(item.dataset.index) || 0;
    
    feedback.classList.remove('hidden');
    
    // Simple comparison - in real app, you might use fuzzy matching or API evaluation
    let isCorrect = false;
    if (userAnswer === correctAnswer) {
        feedback.className = 'exercise-feedback correct';
        feedback.innerHTML = '✅ Perfect translation!';
        textarea.classList.add('correct');
        isCorrect = true;
    } else {
        // Check for partial match
        const similarity = calculateSimilarity(userAnswer, correctAnswer);
        if (similarity > 0.7) {
            feedback.className = 'exercise-feedback partial';
            feedback.innerHTML = `🟡 Close! Expected: <strong>${escapeHtml(textarea.dataset.answer)}</strong>`;
            // Partial credit - consider it correct for scoring
            isCorrect = true;
        } else {
            feedback.className = 'exercise-feedback incorrect';
            feedback.innerHTML = `❌ Expected: <strong>${escapeHtml(textarea.dataset.answer)}</strong>`;
        }
        textarea.classList.add('incorrect');
    }
    
    btn.disabled = true;
    
    // Record the answer for score tracking
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isCorrect, textarea.value, textarea.dataset.answer);
    }
};

/**
 * Simple similarity calculation (Jaccard similarity on words).
 */
function calculateSimilarity(str1, str2) {
    const words1 = new Set(str1.split(/\s+/));
    const words2 = new Set(str2.split(/\s+/));
    const intersection = new Set([...words1].filter(x => words2.has(x)));
    const union = new Set([...words1, ...words2]);
    return intersection.size / union.size;
}

export default {
    renderContent,
    renderMarkdown,
    ContentType
};