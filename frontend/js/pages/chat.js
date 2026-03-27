/**
 * Chat Page Module
 * Manages the chat interface for AI-powered learning moderation
 */

import { apiClient } from '../api/client.js';
import { contentRenderer } from '../services/content-renderer.js';
import { t, applyTranslations } from '../services/i18n.js';
import { showToast } from '../services/toast.js';

/**
 * Chat page state
 */
const state = {
    sessionId: null,
    messages: [],
    suggestions: [],
    isLoading: false,
    isStreaming: false,
    eventSource: null
};

/**
 * Initialize the chat page
 */
export async function init() {
    console.log('Initializing chat page');
    // Apply translations after render
    applyTranslations();
    await loadOrCreateSession();
}

/**
 * Render the chat page
 * @returns {string} HTML content
 */
export function render() {
    return `
        <div class="chat-container">
            <div class="chat-session-controls">
                <button class="chat-session-btn" onclick="window.chatPage.newSession()">
                    🔄 <span data-i18n="chat.newSession">New Session</span>
                </button>
            </div>
            
            <div class="chat-messages" id="chatMessages">
                ${renderMessages()}
            </div>
            
            <div class="chat-input-area">
                <div class="chat-suggestions" id="chatSuggestions">
                    ${renderSuggestions()}
                </div>
                
                <div class="chat-input-row">
                    <textarea 
                        class="chat-input" 
                        id="chatInput"
                        placeholder="${t('chat.placeholder')}"
                        rows="1"
                        onkeydown="window.chatPage.handleKeyDown(event)"
                        oninput="window.chatPage.autoResize(this)"
                    ></textarea>
                    <button 
                        class="chat-send-btn" 
                        id="chatSendBtn"
                        onclick="window.chatPage.sendMessage()"
                        ${state.isLoading ? 'disabled' : ''}
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    `;
}

/**
 * Render all messages
 */
function renderMessages() {
    // Show loading indicator while initial session is loading
    if (state.isLoading && state.messages.length === 0) {
        return `
            <div class="chat-loading">
                <div class="chat-loading-spinner"></div>
                <p class="chat-loading-text" data-i18n="chat.loading">${t('chat.loading')}</p>
            </div>
        `;
    }
    
    if (state.messages.length === 0) {
        return `
            <div class="chat-welcome">
                <div class="chat-welcome-icon">🎓</div>
                <h2 class="chat-welcome-title" data-i18n="chat.title">${t('chat.title')}</h2>
                <p class="chat-welcome-subtitle" data-i18n="chat.subtitle">${t('chat.subtitle')}</p>
            </div>
        `;
    }

    return state.messages.map(msg => renderMessage(msg)).join('');
}

/**
 * Render a single message
 */
function renderMessage(message) {
    const roleClass = message.role.toLowerCase();
    const time = formatTime(message.createdAt);
    
    let contentHtml = `<div class="message-content">${escapeHtml(message.content || '')}</div>`;
    
    // Add embedded activity if present
    if (message.embeddedActivityType && message.embeddedActivityContent) {
        if (message.activityCompleted && message.activitySummary) {
            // Show summary for completed activity
            contentHtml += `
                <div class="activity-summary">
                    <span class="activity-summary-icon">✅</span>
                    <span class="activity-summary-text">${escapeHtml(message.activitySummary)}</span>
                </div>
            `;
        } else {
            // Render the embedded activity
            contentHtml += `
                <div class="embedded-activity" data-message-id="${message.id}" data-activity-type="${message.embeddedActivityType}">
                    ${renderEmbeddedActivity(message)}
                </div>
            `;
        }
    }
    
    return `
        <div class="chat-message ${roleClass}" data-message-id="${message.id}">
            ${contentHtml}
            <span class="message-time">${time}</span>
        </div>
    `;
}

/**
 * Render embedded activity content using content-renderer
 * Maps backend activity types to content-renderer ContentTypes
 */
function renderEmbeddedActivity(message) {
    const type = message.embeddedActivityType;
    const content = message.embeddedActivityContent;
    
    try {
        // Map backend types to content-renderer types
        const typeMapping = {
            'VOCABULARY': contentRenderer.ContentType.VOCABULARY,
            'FLASHCARDS': contentRenderer.ContentType.FLASHCARDS,
            'VISUAL_CARDS': contentRenderer.ContentType.FLASHCARDS, // Visual cards render as flashcards
            'TEXT_COMPLETION': contentRenderer.ContentType.TEXT_COMPLETION,
            'DRAG_DROP': contentRenderer.ContentType.DRAG_DROP,
            'TRANSLATION': contentRenderer.ContentType.TRANSLATION,
            'LISTENING': contentRenderer.ContentType.LISTENING,
            'SPEAKING': contentRenderer.ContentType.SPEAKING,
            'LESSON': contentRenderer.ContentType.LESSON,
            'SCENARIO': contentRenderer.ContentType.SCENARIO,
            'PAIR_MATCHING': contentRenderer.ContentType.PAIR_MATCHING,
            'MEMORY_GAME': contentRenderer.ContentType.MEMORY_GAME
        };
        
        const contentType = typeMapping[type];
        
        if (!contentType) {
            console.warn('Unknown activity type:', type);
            return `<p>${t('chat.unknownActivity')}</p>`;
        }
        
        // Use the unified renderContent function
        return contentRenderer.renderContent(content, contentType);
        
    } catch (error) {
        console.error('Failed to render activity:', error);
        return `<p class="error">${t('chat.activityError')}</p>`;
    }
}

/**
 * Render quick suggestions
 */
function renderSuggestions() {
    if (state.suggestions.length === 0) {
        return '';
    }
    
    return state.suggestions.map(suggestion => `
        <button class="chat-suggestion-btn" onclick="window.chatPage.useSuggestion('${escapeHtml(suggestion)}')">
            ${escapeHtml(suggestion)}
        </button>
    `).join('');
}

/**
 * Load or create a chat session
 */
async function loadOrCreateSession() {
    try {
        state.isLoading = true;
        refreshUI(); // Show loading spinner
        
        const session = await apiClient.chat.getOrCreateSession();
        state.sessionId = session.id;
        
        // Load messages
        const messages = await apiClient.chat.getMessages(session.id);
        state.messages = messages;
        
        // If no messages, trigger the coach to initiate the conversation
        if (messages.length === 0) {
            await initiateConversation();
            return; // initiateConversation will handle UI update
        }
        
        // Extract suggestions from the last assistant message
        const lastAssistant = messages.filter(m => m.role === 'ASSISTANT').pop();
        if (lastAssistant && !lastAssistant.embeddedActivityType) {
            state.suggestions = extractSuggestions(lastAssistant.content);
        }
        
        state.isLoading = false;
        refreshUI();
        scrollToBottom();
    } catch (error) {
        console.error('Failed to load chat session:', error);
        state.isLoading = false;
        refreshUI();
        showToast(t('chat.connectionError'), 'error');
    }
}

/**
 * Initiate the conversation by having the coach send a greeting
 */
async function initiateConversation() {
    try {
        // Send a hidden "start" message to trigger the coach's greeting
        const response = await apiClient.chat.sendMessage(state.sessionId, '__START_SESSION__');
        
        // Only add the assistant response, not the "start" message
        state.messages = [response.message];
        // Always use frontend-translated suggestions (ignore backend English suggestions)
        state.suggestions = extractSuggestions(response.message.content);
        
        state.isLoading = false;
        refreshUI();
        // Don't scroll to bottom - let user read from the start
    } catch (error) {
        console.error('Failed to initiate conversation:', error);
        state.isLoading = false;
        refreshUI();
        showToast(t('chat.connectionError'), 'error');
    }
}

/**
 * Start a new chat session
 */
export async function newSession() {
    if (!confirm(t('chat.newSessionConfirm'))) {
        return;
    }
    
    try {
        if (state.sessionId) {
            await apiClient.chat.clearSession(state.sessionId);
        }
        state.messages = [];
        state.suggestions = [];
        await loadOrCreateSession();
    } catch (error) {
        console.error('Failed to create new session:', error);
        showToast(t('chat.error'), 'error');
    }
}

/**
 * Send a message
 */
export async function sendMessage() {
    const input = document.getElementById('chatInput');
    const content = input?.value?.trim();
    
    if (!content || state.isLoading) {
        return;
    }
    
    // Add user message to UI immediately
    const userMessage = {
        id: 'temp-' + Date.now(),
        role: 'USER',
        content: content,
        createdAt: new Date().toISOString()
    };
    state.messages.push(userMessage);
    state.suggestions = [];
    
    // Clear input
    input.value = '';
    autoResize(input);
    
    // Show loading
    state.isLoading = true;
    refreshUI();
    scrollToBottom();
    
    // Show typing indicator
    showTypingIndicator();
    
    try {
        const response = await apiClient.chat.sendMessage(state.sessionId, content);
        
        // Remove temp message and add real messages
        state.messages = state.messages.filter(m => !m.id.toString().startsWith('temp-'));
        
        // Add the user message with real ID (it's already persisted)
        // The response contains only the assistant message
        state.messages.push(userMessage); // Keep our user message
        state.messages.push(response.message);
        
        // Always use frontend-translated suggestions (ignore backend English suggestions)
        state.suggestions = extractSuggestions(response.message?.content);
        
        hideTypingIndicator();
        state.isLoading = false;
        refreshUI();
        // Don't scroll to bottom - let user read the response from their current position
        
    } catch (error) {
        console.error('Failed to send message:', error);
        hideTypingIndicator();
        state.isLoading = false;
        
        // Remove temp message
        state.messages = state.messages.filter(m => !m.id.toString().startsWith('temp-'));
        
        refreshUI();
        showToast(t('chat.error'), 'error');
    }
}

/**
 * Complete an embedded activity
 */
async function completeActivity(messageId, score) {
    try {
        await apiClient.chat.completeActivity(messageId, score);
        
        // Update the message in state
        const message = state.messages.find(m => m.id === messageId);
        if (message) {
            message.activityCompleted = true;
            message.activitySummary = `Activity completed with ${score}% score`;
        }
        
        refreshUI();
        showToast(t('toast.exerciseSaved'), 'success');
        
    } catch (error) {
        console.error('Failed to complete activity:', error);
        showToast(t('toast.exerciseSaveFailed'), 'error');
    }
}

/**
 * Use a suggestion - automatically sends the message
 */
export function useSuggestion(suggestion) {
    const input = document.getElementById('chatInput');
    if (input) {
        input.value = suggestion;
        autoResize(input);
        // Automatically send the message
        sendMessage();
    }
}

/**
 * Handle keyboard events
 */
export function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

/**
 * Auto-resize textarea
 */
export function autoResize(element) {
    element.style.height = 'auto';
    element.style.height = Math.min(element.scrollHeight, 120) + 'px';
}

/**
 * Show typing indicator
 */
function showTypingIndicator() {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    
    const indicator = document.createElement('div');
    indicator.id = 'typingIndicator';
    indicator.className = 'chat-message assistant';
    indicator.innerHTML = `
        <div class="typing-indicator">
            <div class="chat-loading-dots">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    `;
    container.appendChild(indicator);
    scrollToBottom();
}

/**
 * Hide typing indicator
 */
function hideTypingIndicator() {
    const indicator = document.getElementById('typingIndicator');
    if (indicator) {
        indicator.remove();
    }
}

/**
 * Refresh the UI
 */
function refreshUI() {
    const messagesContainer = document.getElementById('chatMessages');
    const suggestionsContainer = document.getElementById('chatSuggestions');
    const sendBtn = document.getElementById('chatSendBtn');
    
    if (messagesContainer) {
        messagesContainer.innerHTML = renderMessages();
    }
    
    if (suggestionsContainer) {
        suggestionsContainer.innerHTML = renderSuggestions();
    }
    
    if (sendBtn) {
        sendBtn.disabled = state.isLoading;
    }
}

/**
 * Scroll to bottom of messages
 */
function scrollToBottom() {
    const container = document.getElementById('chatMessages');
    if (container) {
        setTimeout(() => {
            container.scrollTop = container.scrollHeight;
        }, 100);
    }
}

/**
 * Extract suggestions from message content
 * Returns translated suggestions in the user's native language
 */
function extractSuggestions(content) {
    // Default suggestions - translated to user's native language
    return [
        t('chat.suggestionPracticeVocab'),
        t('chat.suggestionDoExercises'),
        t('chat.suggestionStartLesson'),
        t('chat.suggestionReview')
    ];
}

/**
 * Format timestamp
 */
function formatTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/**
 * Escape HTML
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Expose functions for onclick handlers
window.chatPage = {
    sendMessage,
    newSession,
    useSuggestion,
    handleKeyDown,
    autoResize
};