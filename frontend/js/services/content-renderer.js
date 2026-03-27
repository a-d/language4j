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
    ContentType.SCENARIO,
    ContentType.LEARNING_PLAN
];

/**
 * JSON-based content types.
 */
const JSON_TYPES = [
    ContentType.VOCABULARY,
    ContentType.FLASHCARDS,
    ContentType.TEXT_COMPLETION,
    ContentType.DRAG_DROP,
    ContentType.TRANSLATION,
    ContentType.LISTENING,
    ContentType.SPEAKING,
    ContentType.EVALUATION,
    ContentType.PRONUNCIATION_EVALUATION
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
 * Sanitizes JSON string to fix common LLM output issues.
 * Handles invalid control characters and escape sequences.
 * @param {string} json - Raw JSON string
 * @returns {string} Sanitized JSON string
 */
function sanitizeJson(json) {
    if (!json || typeof json !== 'string') return json;
    
    // Valid JSON escape characters (after the backslash)
    const validEscapes = new Set(['"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u']);
    
    let result = '';
    let inString = false;
    let i = 0;
    
    while (i < json.length) {
        const c = json[i];
        const charCode = json.charCodeAt(i);
        
        if (c === '"' && (i === 0 || json[i - 1] !== '\\' || !inString)) {
            // Toggle string state (but only if not escaped inside a string)
            // Need to count preceding backslashes to handle \\\" correctly
            let backslashCount = 0;
            let j = i - 1;
            while (j >= 0 && json[j] === '\\') {
                backslashCount++;
                j--;
            }
            // Quote is escaped only if odd number of backslashes before it
            if (!inString || backslashCount % 2 === 0) {
                inString = !inString;
            }
            result += c;
            i++;
        } else if (c === '\\' && inString) {
            // Check the next character for escape sequence
            const nextChar = json[i + 1];
            
            if (nextChar === undefined) {
                // Backslash at end of string - remove it
                i++;
            } else if (validEscapes.has(nextChar)) {
                // Valid escape sequence - keep both characters
                result += c + nextChar;
                i += 2;
            } else if (nextChar === "'") {
                // \' is not valid in JSON - convert to just apostrophe
                result += "'";
                i += 2;
            } else if (nextChar === '\n') {
                // Escaped literal newline - convert to \n
                result += '\\n';
                i += 2;
            } else if (nextChar === '\r') {
                // Escaped literal carriage return - convert to \r
                result += '\\r';
                i += 2;
            } else if (nextChar === '\t') {
                // Escaped literal tab - convert to \t
                result += '\\t';
                i += 2;
            } else {
                // Invalid escape sequence (like \x, \a, \c, etc.)
                // Skip the backslash, keep the character as-is
                result += nextChar;
                i += 2;
            }
        } else if (inString && charCode < 32) {
            // Control character inside string - escape it properly
            switch (c) {
                case '\n': result += '\\n'; break;
                case '\r': result += '\\r'; break;
                case '\t': result += '\\t'; break;
                case '\b': result += '\\b'; break;
                case '\f': result += '\\f'; break;
                default:
                    // Other control characters - encode as unicode escape
                    result += '\\u' + charCode.toString(16).padStart(4, '0');
            }
            i++;
        } else {
            // Regular character - keep as-is
            result += c;
            i++;
        }
    }
    
    return result;
}

/**
 * Attempts multiple repair strategies to parse malformed JSON.
 * @param {string} json - Raw JSON string
 * @returns {Object|null} Parsed object or null if all repairs fail
 */
function tryRepairJson(json) {
    // Strategy 1: Basic sanitization (escape sequences, control chars)
    try {
        const sanitized = sanitizeJson(json);
        return JSON.parse(sanitized);
    } catch (e) {
        // Continue to next strategy
    }
    
    // Strategy 2: Use regex to extract just the vocabulary array
    // This handles cases where there's extra text around the JSON
    try {
        const vocabMatch = json.match(/\{\s*"vocabulary"\s*:\s*\[[\s\S]*\]\s*\}/);
        if (vocabMatch) {
            const extracted = sanitizeJson(vocabMatch[0]);
            return JSON.parse(extracted);
        }
    } catch (e) {
        // Continue to next strategy
    }
    
    // Strategy 3: Try to fix common structural issues
    try {
        let fixed = json;
        // Remove trailing commas before } or ]
        fixed = fixed.replace(/,(\s*[}\]])/g, '$1');
        // Try sanitizing the fixed version
        fixed = sanitizeJson(fixed);
        return JSON.parse(fixed);
    } catch (e) {
        // Continue to next strategy
    }
    
    // Strategy 4: Try to manually build vocabulary array from patterns
    try {
        const items = [];
        // Match patterns like "word": "..." and "translation": "..."
        const wordPattern = /"word"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"/g;
        const translationPattern = /"translation"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"/g;
        
        const words = [...json.matchAll(wordPattern)].map(m => m[1]);
        const translations = [...json.matchAll(translationPattern)].map(m => m[1]);
        
        if (words.length > 0 && words.length === translations.length) {
            for (let i = 0; i < words.length; i++) {
                items.push({
                    word: words[i],
                    translation: translations[i]
                });
            }
            console.log('JSON recovered via pattern extraction');
            return { vocabulary: items };
        }
    } catch (e) {
        // All strategies failed
    }
    
    return null;
}

/**
 * Renders JSON-based content.
 */
function renderJsonContent(content, type) {
    let data;
    try {
        // First try to parse as-is
        data = JSON.parse(content);
    } catch (e) {
        // If parsing fails, try multiple repair strategies
        data = tryRepairJson(content);
        if (data) {
            console.log('JSON parsed after repair');
        } else {
            console.warn('Failed to parse JSON content even after repair attempts, falling back to Markdown:', e);
            return renderMarkdown(content);
        }
    }
    
    switch (type) {
        case ContentType.VOCABULARY:
            return renderVocabulary(data);
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
        case ContentType.LISTENING:
            return renderListeningExercises(data);
        case ContentType.SPEAKING:
            return renderSpeakingExercises(data);
        case ContentType.PRONUNCIATION_EVALUATION:
            return renderPronunciationEvaluation(data);
        default:
            // Generic JSON display
            return renderGenericJson(data);
    }
}

// ==================== Vocabulary Renderer ====================

/**
 * Renders vocabulary items as visual cards.
 * 
 * Expected data format:
 * { vocabulary: [{ word, pronunciation, translation, partOfSpeech, example, exampleTranslation, usageNote }] }
 * or
 * [{ word, pronunciation, translation, partOfSpeech, example, exampleTranslation, usageNote }]
 */
function renderVocabulary(data) {
    const items = Array.isArray(data) ? data : (data.vocabulary || data.words || data.items || []);
    
    if (items.length === 0) {
        return '<p class="empty-state">No vocabulary generated.</p>';
    }
    
    return `
        <div class="vocabulary-list">
            <div class="vocabulary-count">${items.length} words</div>
            <div class="vocabulary-cards">
                ${items.map((item, index) => renderSingleVocabularyCard(item, index)).join('')}
            </div>
        </div>
    `;
}

/**
 * Renders a single vocabulary card with collapsible details.
 * Cards are collapsed by default and expand on click.
 * @param {Object} item - The vocabulary item data
 * @param {number} index - The item index
 * @returns {string} HTML string for the card
 */
function renderSingleVocabularyCard(item, index) {
    const word = item.word || item.term || item.phrase || '';
    const pronunciation = item.pronunciation || item.phonetic || '';
    const translation = item.translation || item.meaning || item.definition || '';
    const partOfSpeech = item.partOfSpeech || item.pos || item.type || '';
    const example = item.example || item.exampleSentence || '';
    const exampleTranslation = item.exampleTranslation || '';
    const usageNote = item.usageNote || item.note || item.usage || '';
    
    // Check if there's any expandable content
    const hasExpandableContent = pronunciation || example || usageNote;
    
    return `
        <div class="vocabulary-card${hasExpandableContent ? ' collapsible' : ''}" data-index="${index}" onclick="window.toggleVocabCard(this, event)">
            <div class="vocab-card-header">
                <div class="vocab-header-main">
                    <span class="vocab-word">${escapeHtml(word)}</span>
                    ${partOfSpeech ? `<span class="vocab-pos">${escapeHtml(partOfSpeech)}</span>` : ''}
                    <button class="vocab-play-btn" onclick="window.playVocabWord(this, event)" data-word="${escapeHtml(word)}" title="Listen to pronunciation">
                        🔊
                    </button>
                </div>
                <div class="vocab-header-summary">
                    <span class="vocab-translation-preview">${escapeHtml(translation)}</span>
                    ${hasExpandableContent ? '<span class="vocab-expand-icon">▼</span>' : ''}
                </div>
            </div>
            
            <div class="vocab-card-body">
                ${pronunciation ? `
                    <div class="vocab-pronunciation">
                        <span class="pronunciation-icon">🔊</span>
                        <span class="pronunciation-text">${escapeHtml(pronunciation)}</span>
                    </div>
                ` : ''}
                
                <div class="vocab-translation">
                    <span class="translation-icon">📖</span>
                    <span class="translation-text">${escapeHtml(translation)}</span>
                </div>
                
                ${example ? `
                    <div class="vocab-example">
                        <div class="example-header">
                            <span class="example-icon">💬</span>
                            <span class="example-label">Example</span>
                        </div>
                        <p class="example-text">${escapeHtml(example)}</p>
                        ${exampleTranslation ? `<p class="example-translation">${escapeHtml(exampleTranslation)}</p>` : ''}
                    </div>
                ` : ''}
                
                ${usageNote ? `
                    <div class="vocab-usage-note">
                        <span class="usage-icon">💡</span>
                        <span class="usage-text">${escapeHtml(usageNote)}</span>
                    </div>
                ` : ''}
            </div>
        </div>
    `;
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
 * { exercises: [{ sentence: string, correctAnswer: string, wordBank?: string[], explanation?: string }] }
 * or
 * [{ sentence: string, correctAnswer: string, ... }]
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
                // Check for correctAnswer (from backend), then fallback to other common property names
                const answer = ex.correctAnswer || ex.answer || ex.blank || ex.correct || '';
                const hint = ex.hint || ex.explanation || '';
                const wordBank = ex.wordBank || ex.options || [];
                
                // Replace blank marker with input field
                const displaySentence = sentence.replace(/_{2,}|\[\.\.\.\]|\[blank\]|\{\{blank\}\}/gi, 
                    `<input type="text" class="exercise-input" data-answer="${escapeHtml(answer)}" placeholder="..." />`
                );
                
                return `
                    <div class="exercise-item" data-index="${index}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <p class="exercise-sentence">${displaySentence}</p>
                            ${wordBank.length > 0 ? `
                                <div class="word-bank-hint">
                                    <span class="word-bank-label">💡 Word bank:</span>
                                    <span class="word-bank-words">${wordBank.map(w => `<span class="word-option clickable" onclick="window.fillWordBankOption(this, '${escapeHtml(w)}')">${escapeHtml(w)}</span>`).join(' ')}</span>
                                </div>
                            ` : ''}
                            ${hint ? `<p class="exercise-explanation hidden" data-explanation="${escapeHtml(hint)}">📚 ${escapeHtml(hint)}</p>` : ''}
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
 * 
 * The LLM provides words in CORRECT order via the "words" field.
 * The frontend shuffles them for display. This approach:
 * - Saves tokens (LLM doesn't need to shuffle)
 * - Guarantees words match the expected answer
 * - Allows consistent shuffling behavior controlled by frontend
 */
function renderSingleDragDropExercise(ex, index) {
    // Extract words in correct order - try multiple field names for backwards compatibility
    // Priority: words (new format) > correctOrder (legacy) > correct > sentence
    let wordsInCorrectOrder = [];
    
    if (ex.words && Array.isArray(ex.words)) {
        wordsInCorrectOrder = ex.words;
    } else if (ex.correctOrder && Array.isArray(ex.correctOrder)) {
        wordsInCorrectOrder = ex.correctOrder;
    } else if (ex.correct) {
        wordsInCorrectOrder = typeof ex.correct === 'string' ? ex.correct.split(/\s+/) : ex.correct;
    } else if (ex.sentence) {
        wordsInCorrectOrder = typeof ex.sentence === 'string' ? ex.sentence.split(/\s+/) : ex.sentence;
    }
    
    // Build the correct sentence for validation
    const correctSentence = wordsInCorrectOrder.join(' ');
    
    // Shuffle words for display - guaranteed to be different from correct order
    const words = shuffleArray([...wordsInCorrectOrder]);
    
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
 * 
 * Supports multiple data formats from the backend:
 * 
 * Format 1 (Backend LLM response):
 * { exercises: [{ sourceText: string, modelAnswer: string, alternatives?: string[], keyPoints?: string[] }] }
 * 
 * Format 2 (Simple format):
 * { exercises: [{ source: string, target: string, hint?: string }] }
 * 
 * Format 3 (Array only):
 * [{ sourceText: string, modelAnswer: string, ... }]
 */
function renderTranslationExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || data.sentences || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No exercises generated.</p>';
    }
    
    return `
        <div class="exercises-list translation-exercises">
            ${exercises.map((ex, index) => {
                // Extract source text - try backend format first (sourceText), then fallbacks
                const source = ex.sourceText || ex.source || ex.sentence || ex.original || ex.text || '';
                
                // Extract target answer - try backend format first (modelAnswer), then fallbacks
                const target = ex.modelAnswer || ex.target || ex.translation || ex.answer || '';
                
                // Extract alternatives for showing acceptable variations
                const alternatives = ex.alternatives || [];
                
                // Extract hint - try keyPoints from backend, then standard hint fields
                const keyPoints = ex.keyPoints || [];
                const hint = ex.hint || ex.vocabulary || '';
                
                // Build hint text from keyPoints if available
                let hintText = '';
                if (keyPoints.length > 0) {
                    hintText = `Key points: ${keyPoints.join(', ')}`;
                } else if (hint) {
                    hintText = typeof hint === 'object' ? JSON.stringify(hint) : hint;
                }
                
                return `
                    <div class="exercise-item translation-item" data-index="${index}" data-alternatives="${escapeHtml(JSON.stringify(alternatives))}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <p class="exercise-source"><strong>Translate:</strong> ${escapeHtml(source)}</p>
                            ${hintText ? `<p class="exercise-hint">💡 ${escapeHtml(hintText)}</p>` : ''}
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

// ==================== Listening Exercises Renderer ====================

/**
 * Renders listening comprehension exercises.
 * Expected data format:
 * { exercises: [{ sentence: string, translation: string, hint?: string }] }
 */
function renderListeningExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No listening exercises generated.</p>';
    }
    
    return `
        <div class="exercises-list listening-exercises">
            <div class="listening-instructions">
                <span class="instruction-icon">🎧</span>
                <span>Listen to the audio and type what you hear.</span>
            </div>
            ${exercises.map((ex, index) => {
                const sentence = ex.sentence || ex.text || '';
                const translation = ex.translation || ex.hint || '';
                
                return `
                    <div class="exercise-item listening-item" data-index="${index}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <div class="audio-controls">
                                <button class="btn btn-secondary play-audio-btn" onclick="window.playListeningAudio(this)" data-sentence="${escapeHtml(sentence)}">
                                    🔊 Play Audio
                                </button>
                                <button class="btn btn-secondary play-slow-btn" onclick="window.playListeningAudio(this, true)" data-sentence="${escapeHtml(sentence)}">
                                    🐢 Play Slow
                                </button>
                            </div>
                            ${translation ? `<p class="exercise-hint">💡 Translation: ${escapeHtml(translation)}</p>` : ''}
                            <input type="text" class="exercise-input listening-input" data-answer="${escapeHtml(sentence)}" placeholder="Type what you hear..." />
                            <div class="exercise-feedback hidden"></div>
                        </div>
                        <button class="btn btn-sm btn-primary exercise-check" onclick="window.checkListeningAnswer(this)">Check</button>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

// ==================== Speaking Exercises Renderer ====================

/**
 * Renders speaking/pronunciation exercises.
 * Expected data format:
 * { exercises: [{ text: string, translation: string, pronunciationTips?: string, commonMistakes?: string }] }
 */
function renderSpeakingExercises(data) {
    const exercises = Array.isArray(data) ? data : (data.exercises || []);
    
    if (exercises.length === 0) {
        return '<p class="empty-state">No speaking exercises generated.</p>';
    }
    
    return `
        <div class="exercises-list speaking-exercises">
            <div class="speaking-instructions">
                <span class="instruction-icon">🎤</span>
                <span>Read the text aloud. Click "Record" to practice your pronunciation.</span>
            </div>
            ${exercises.map((ex, index) => {
                const text = ex.text || ex.sentence || '';
                const translation = ex.translation || '';
                const tips = ex.pronunciationTips || ex.tips || '';
                const mistakes = ex.commonMistakes || '';
                
                return `
                    <div class="exercise-item speaking-item" data-index="${index}">
                        <div class="exercise-number">${index + 1}</div>
                        <div class="exercise-content">
                            <p class="speaking-text">${escapeHtml(text)}</p>
                            ${translation ? `<p class="speaking-translation">📖 ${escapeHtml(translation)}</p>` : ''}
                            
                            <div class="audio-controls">
                                <button class="btn btn-secondary play-model-btn" onclick="window.playModelAudio(this)" data-text="${escapeHtml(text)}">
                                    🔊 Listen to Model
                                </button>
                            </div>
                            
                            ${tips ? `
                                <div class="pronunciation-tips">
                                    <span class="tips-icon">💡</span>
                                    <span class="tips-text">${escapeHtml(tips)}</span>
                                </div>
                            ` : ''}
                            
                            ${mistakes ? `
                                <div class="common-mistakes">
                                    <span class="mistakes-icon">⚠️</span>
                                    <span class="mistakes-text">Common mistakes: ${escapeHtml(mistakes)}</span>
                                </div>
                            ` : ''}
                            
                            <div class="recording-controls">
                                <button class="btn btn-primary record-btn" onclick="window.toggleRecording(this)" data-expected="${escapeHtml(text)}">
                                    🎤 Record
                                </button>
                                <span class="recording-status"></span>
                            </div>
                            
                            <div class="exercise-feedback hidden"></div>
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

// ==================== Pronunciation Evaluation Renderer ====================

/**
 * Renders pronunciation evaluation results.
 * Expected data format:
 * { accuracy: number, feedback: string, wordByWord?: [{ word, correct, tip }], tips?: string[] }
 */
function renderPronunciationEvaluation(data) {
    const accuracy = data.accuracy || data.score || 0;
    const feedback = data.feedback || data.message || '';
    const wordByWord = data.wordByWord || data.words || [];
    const tips = data.tips || data.suggestions || [];
    
    const accuracyClass = accuracy >= 80 ? 'excellent' : accuracy >= 60 ? 'good' : 'needs-practice';
    
    return `
        <div class="pronunciation-evaluation ${accuracyClass}">
            <div class="evaluation-header">
                <span class="accuracy-icon">${accuracy >= 80 ? '🎉' : accuracy >= 60 ? '👍' : '🔄'}</span>
                <span class="accuracy-score">${accuracy}% Accuracy</span>
            </div>
            
            ${feedback ? `<p class="evaluation-feedback">${escapeHtml(feedback)}</p>` : ''}
            
            ${wordByWord.length > 0 ? `
                <div class="word-by-word">
                    <strong>Word breakdown:</strong>
                    <div class="word-list">
                        ${wordByWord.map(w => `
                            <span class="word-item ${w.correct ? 'correct' : 'incorrect'}">
                                ${escapeHtml(w.word)}
                                ${w.tip ? `<span class="word-tip">(${escapeHtml(w.tip)})</span>` : ''}
                            </span>
                        `).join('')}
                    </div>
                </div>
            ` : ''}
            
            ${tips.length > 0 ? `
                <div class="pronunciation-tips-list">
                    <strong>Tips for improvement:</strong>
                    <ul>
                        ${tips.map(tip => `<li>${escapeHtml(tip)}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
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
 * Guarantees the result is different from the original if array has 2+ unique elements.
 * @param {Array} array - Array to shuffle
 * @returns {Array} Shuffled array (different order than original)
 */
function shuffleArray(array) {
    if (array.length <= 1) return [...array];
    
    const original = [...array];
    let shuffled = [...array];
    let attempts = 0;
    const maxAttempts = 10;
    
    // Keep shuffling until we get a different order (or hit max attempts)
    do {
        // Fisher-Yates shuffle
        for (let i = shuffled.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
        }
        attempts++;
    } while (arraysEqual(original, shuffled) && attempts < maxAttempts);
    
    // If we still have the same order after max attempts (very unlikely for arrays > 2),
    // just swap the first two elements to guarantee a different order
    if (arraysEqual(original, shuffled) && shuffled.length >= 2) {
        [shuffled[0], shuffled[1]] = [shuffled[1], shuffled[0]];
    }
    
    return shuffled;
}

/**
 * Checks if two arrays have the same elements in the same order.
 * @param {Array} a - First array
 * @param {Array} b - Second array
 * @returns {boolean} True if arrays are equal
 */
function arraysEqual(a, b) {
    if (a.length !== b.length) return false;
    for (let i = 0; i < a.length; i++) {
        if (a[i] !== b[i]) return false;
    }
    return true;
}

// ==================== Interactive Functions (Global) ====================

/**
 * Toggle vocabulary card expanded/collapsed state.
 */
window.toggleVocabCard = function(card, event) {
    // Only toggle if the card is collapsible
    if (!card.classList.contains('collapsible')) return;
    
    // Prevent toggle if clicking on interactive elements inside the card
    if (event && event.target.closest('button, a, input, textarea, .vocab-play-btn')) return;
    
    card.classList.toggle('expanded');
};

/**
 * Play vocabulary word audio using text-to-speech.
 * Uses the speech API to synthesize and play the word.
 */
window.playVocabWord = async function(btn, event) {
    // Prevent event bubbling to card click handler
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    
    const word = btn.dataset.word;
    if (!word) return;
    
    // Disable button and show loading state
    btn.disabled = true;
    const originalContent = btn.innerHTML;
    btn.innerHTML = '⏳';
    btn.classList.add('loading');
    
    try {
        // Import the API dynamically to avoid circular dependency
        const { api } = await import('../api/client.js');
        
        // Get target language from app config (default to user's configured target language)
        const targetLanguage = window.APP_CONFIG?.TARGET_LANGUAGE || 'de';
        
        // Call speech synthesis API
        const audioBlob = await api.speech.synthesize(word, targetLanguage, false);
        
        // Create audio element and play
        const audioUrl = URL.createObjectURL(audioBlob);
        const audio = new Audio(audioUrl);
        
        // Clean up URL after playing
        audio.onended = () => {
            URL.revokeObjectURL(audioUrl);
            btn.disabled = false;
            btn.innerHTML = originalContent;
            btn.classList.remove('loading', 'playing');
        };
        
        audio.onerror = () => {
            console.error('Audio playback error');
            URL.revokeObjectURL(audioUrl);
            btn.disabled = false;
            btn.innerHTML = originalContent;
            btn.classList.remove('loading', 'playing');
        };
        
        btn.innerHTML = '🔊';
        btn.classList.remove('loading');
        btn.classList.add('playing');
        await audio.play();
        
    } catch (error) {
        console.error('Failed to play vocabulary word:', error);
        btn.disabled = false;
        btn.innerHTML = originalContent;
        btn.classList.remove('loading', 'playing');
        
        // Show error feedback (if toast is available)
        if (window.showToast) {
            window.showToast('Failed to play audio', 'error');
        }
    }
};

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
 * Normalizes text for lenient comparison.
 * Removes accents, normalizes whitespace, and lowercases.
 * @param {string} text - Text to normalize
 * @returns {string} Normalized text
 */
function normalizeForComparison(text) {
    if (!text) return '';
    return text
        // Normalize Unicode (decompose accented characters)
        .normalize('NFD')
        // Remove diacritical marks (accents)
        .replace(/[\u0300-\u036f]/g, '')
        // Convert to lowercase
        .toLowerCase()
        // Normalize whitespace (collapse multiple spaces, trim)
        .replace(/\s+/g, ' ')
        .trim();
}

/**
 * Checks if two strings match leniently.
 * Lenient matching ignores: whitespace differences, capitalization, accents.
 * @param {string} userAnswer - User's answer
 * @param {string} correctAnswer - Correct answer
 * @returns {{isMatch: boolean, isExact: boolean}} Match result
 */
function lenientMatch(userAnswer, correctAnswer) {
    const normalizedUser = normalizeForComparison(userAnswer);
    const normalizedCorrect = normalizeForComparison(correctAnswer);
    
    // Exact match (after normalization)
    if (normalizedUser === normalizedCorrect) {
        // Check if it was also an exact match before normalization
        const isExact = userAnswer.trim() === correctAnswer.trim();
        return { isMatch: true, isExact };
    }
    
    return { isMatch: false, isExact: false };
}

/**
 * Text completion exercise check with lenient validation.
 * Lenient matching ignores: whitespace differences, capitalization, accents.
 */
window.checkExerciseAnswer = function(btn) {
    const item = btn.closest('.exercise-item');
    const input = item.querySelector('.exercise-input');
    const feedback = item.querySelector('.exercise-feedback');
    const correctAnswer = input.dataset.answer || '';
    const userAnswer = input.value || '';
    const index = parseInt(item.dataset.index) || 0;
    
    // Use lenient matching
    const { isMatch, isExact } = lenientMatch(userAnswer, correctAnswer);
    
    feedback.classList.remove('hidden');
    
    if (isMatch) {
        feedback.className = 'exercise-feedback correct';
        if (isExact) {
            feedback.innerHTML = '✅ Correct!';
        } else {
            // Matched but with minor differences (case, accents, whitespace)
            feedback.innerHTML = `✅ Correct! <span class="feedback-note">(Note: exact spelling is "${escapeHtml(correctAnswer)}")</span>`;
        }
        input.classList.add('correct');
    } else {
        feedback.className = 'exercise-feedback incorrect';
        feedback.innerHTML = `❌ Incorrect. <button class="btn btn-sm btn-link show-solution-btn" onclick="window.toggleSolution(this)">Show solution</button>
            <span class="solution-text hidden"><strong>${escapeHtml(correctAnswer)}</strong></span>`;
        input.classList.add('incorrect');
    }
    
    btn.disabled = true;
    
    // Show explanation if available (only after checking)
    const explanation = item.querySelector('.exercise-explanation');
    if (explanation) {
        explanation.classList.remove('hidden');
    }
    
    // Record the answer for score tracking
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isMatch, input.value, correctAnswer);
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
 * Translation exercise check with support for alternative answers.
 * Checks the user's answer against the model answer and any provided alternatives.
 */
window.checkTranslationAnswer = function(btn) {
    const item = btn.closest('.translation-item');
    const textarea = item.querySelector('.exercise-textarea');
    const feedback = item.querySelector('.exercise-feedback');
    const correctAnswer = textarea.dataset.answer || '';
    const userAnswer = textarea.value.trim();
    const index = parseInt(item.dataset.index) || 0;
    
    // Get alternatives from data attribute
    let alternatives = [];
    try {
        const altData = item.dataset.alternatives;
        if (altData) {
            alternatives = JSON.parse(altData);
        }
    } catch (e) {
        // Ignore parse errors
    }
    
    // All acceptable answers (model answer + alternatives)
    const allAcceptable = [correctAnswer, ...alternatives].filter(a => a && a.trim());
    
    feedback.classList.remove('hidden');
    
    // Check for exact match (case-insensitive) against model answer or alternatives
    const normalizedUserAnswer = userAnswer.toLowerCase().trim();
    let isCorrect = false;
    let matchedAnswer = null;
    
    for (const acceptable of allAcceptable) {
        if (normalizedUserAnswer === acceptable.toLowerCase().trim()) {
            isCorrect = true;
            matchedAnswer = acceptable;
            break;
        }
    }
    
    if (isCorrect) {
        feedback.className = 'exercise-feedback correct';
        if (matchedAnswer === correctAnswer) {
            feedback.innerHTML = '✅ Perfect translation!';
        } else {
            feedback.innerHTML = `✅ Correct! (This is an acceptable alternative)`;
        }
        textarea.classList.add('correct');
    } else {
        // Check for partial match against model answer
        const similarity = calculateSimilarity(normalizedUserAnswer, correctAnswer.toLowerCase().trim());
        
        // Also check similarity against alternatives
        let bestAltSimilarity = 0;
        for (const alt of alternatives) {
            const altSim = calculateSimilarity(normalizedUserAnswer, alt.toLowerCase().trim());
            if (altSim > bestAltSimilarity) {
                bestAltSimilarity = altSim;
            }
        }
        
        const bestSimilarity = Math.max(similarity, bestAltSimilarity);
        
        if (bestSimilarity > 0.7) {
            feedback.className = 'exercise-feedback partial';
            feedback.innerHTML = `🟡 Close! Expected: <strong>${escapeHtml(correctAnswer)}</strong>`;
            // Partial credit - consider it correct for scoring
            isCorrect = true;
            textarea.classList.add('partial');
        } else {
            feedback.className = 'exercise-feedback incorrect';
            let feedbackHtml = `❌ Expected: <strong>${escapeHtml(correctAnswer)}</strong>`;
            if (alternatives.length > 0) {
                feedbackHtml += `<br><span class="alternatives-note">Also acceptable: ${alternatives.map(a => escapeHtml(a)).join(', ')}</span>`;
            }
            feedback.innerHTML = feedbackHtml;
            textarea.classList.add('incorrect');
        }
    }
    
    btn.disabled = true;
    
    // Record the answer for score tracking
    if (window.recordExerciseAnswer) {
        window.recordExerciseAnswer(index, isCorrect, textarea.value, correctAnswer);
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

/**
 * Fill the input field with a word from the word bank.
 * @param {HTMLElement} wordEl - The clicked word element
 * @param {string} word - The word to fill
 */
window.fillWordBankOption = function(wordEl, word) {
    const item = wordEl.closest('.exercise-item');
    const input = item.querySelector('.exercise-input');
    
    if (input && !input.disabled) {
        input.value = word;
        input.focus();
        
        // Add visual feedback that the word was selected
        const allWords = item.querySelectorAll('.word-bank-hint .word-option');
        allWords.forEach(w => w.classList.remove('selected'));
        wordEl.classList.add('selected');
    }
};

/**
 * Toggle the visibility of the solution text.
 * @param {HTMLElement} btn - The toggle button
 */
window.toggleSolution = function(btn) {
    const solutionText = btn.parentElement.querySelector('.solution-text');
    if (solutionText) {
        const isHidden = solutionText.classList.contains('hidden');
        solutionText.classList.toggle('hidden');
        btn.textContent = isHidden ? 'Hide solution' : 'Show solution';
    }
};

/**
 * Content renderer object for convenient imports.
 */
export const contentRenderer = {
    renderContent,
    renderMarkdown,
    ContentType
};

export default contentRenderer;
