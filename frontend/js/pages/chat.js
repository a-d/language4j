/**
 * Chat Page Module
 * Manages the chat interface for AI-powered learning moderation
 * with layered activity and topic selection dialog
 */

import { apiClient } from '../api/client.js';
import { contentRenderer } from '../services/content-renderer.js';
import { t, applyTranslations } from '../services/i18n.js';
import { showToast } from '../services/toast.js';

/**
 * Selection states for the layered dialog
 */
const SelectionState = {
    IDLE: 'IDLE',                       // Normal chat mode
    SELECTING_ACTIVITY: 'SELECTING_ACTIVITY', // Layer 1: Activity type selection
    LOADING_TOPICS: 'LOADING_TOPICS',   // Fetching topic suggestions
    SELECTING_TOPIC: 'SELECTING_TOPIC', // Layer 2: Topic selection
    CUSTOM_TOPIC: 'CUSTOM_TOPIC'        // Custom topic input
};

/**
 * Activity categories matching backend ActivityCategory enum
 */
const ActivityCategory = {
    VOCABULARY: 'VOCABULARY',
    EXERCISE: 'EXERCISE',
    LESSON: 'LESSON',
    SCENARIO: 'SCENARIO',
    AUDIO: 'AUDIO'
};

/**
 * Maps user-facing activity types to backend categories and activity types
 */
const ActivityMapping = {
    vocabulary: { category: ActivityCategory.VOCABULARY, activityType: 'VOCABULARY', label: 'chat.activityVocabulary' },
    exercise: { category: ActivityCategory.EXERCISE, activityType: 'TEXT_COMPLETION', label: 'chat.activityExercise' },
    lesson: { category: ActivityCategory.LESSON, activityType: 'LESSON', label: 'chat.activityLesson' },
    scenario: { category: ActivityCategory.SCENARIO, activityType: 'SCENARIO', label: 'chat.activityScenario' },
    audio: { category: ActivityCategory.AUDIO, activityType: 'LISTENING', label: 'chat.activityAudio' }
};

/**
 * Chat page state
 */
const state = {
    sessionId: null,
    messages: [],
    suggestions: [],
    isLoading: false,
    isStreaming: false,
    eventSource: null,
    // Layered dialog state
    selectionState: SelectionState.IDLE,
    selectedActivity: null,
    topicSuggestions: [],
    randomTopic: null
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
 * Render suggestions based on current selection state
 */
function renderSuggestions() {
    switch (state.selectionState) {
        case SelectionState.SELECTING_ACTIVITY:
            return renderActivitySuggestions();
        case SelectionState.LOADING_TOPICS:
            return renderLoadingTopics();
        case SelectionState.SELECTING_TOPIC:
            return renderTopicSuggestions();
        case SelectionState.CUSTOM_TOPIC:
            return renderCustomTopicInput();
        default:
            return renderDefaultSuggestions();
    }
}

/**
 * Render Layer 1: Activity type selection
 */
function renderActivitySuggestions() {
    return `
        <div class="suggestion-layer activity-selection">
            <div class="suggestion-label">${t('chat.selectActivity')}</div>
            <div class="suggestion-buttons">
                <button class="chat-suggestion-btn activity-btn" onclick="window.chatPage.selectActivity('vocabulary')">
                    ${t('chat.activityVocabulary')}
                </button>
                <button class="chat-suggestion-btn activity-btn" onclick="window.chatPage.selectActivity('exercise')">
                    ${t('chat.activityExercise')}
                </button>
                <button class="chat-suggestion-btn activity-btn" onclick="window.chatPage.selectActivity('lesson')">
                    ${t('chat.activityLesson')}
                </button>
                <button class="chat-suggestion-btn activity-btn" onclick="window.chatPage.selectActivity('scenario')">
                    ${t('chat.activityScenario')}
                </button>
                <button class="chat-suggestion-btn activity-btn" onclick="window.chatPage.selectActivity('audio')">
                    ${t('chat.activityAudio')}
                </button>
                <button class="chat-suggestion-btn surprise-btn" onclick="window.chatPage.surpriseMe()">
                    ${t('chat.activitySurprise')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Render loading state while fetching topics
 */
function renderLoadingTopics() {
    return `
        <div class="suggestion-layer loading-topics">
            <div class="suggestion-label">
                <span class="loading-spinner-small"></span>
                ${t('chat.loadingTopics')}
            </div>
        </div>
    `;
}

/**
 * Render Layer 2: Topic selection
 */
function renderTopicSuggestions() {
    const topicButtons = state.topicSuggestions.map((topic, index) => `
        <button class="chat-suggestion-btn topic-btn ${topic.alignsWithGoals ? 'aligned-goal' : ''}" 
                onclick="window.chatPage.selectTopic(${index})"
                title="${topic.description || ''}">
            <span class="topic-emoji">${topic.emoji || '📝'}</span>
            <span class="topic-text">${escapeHtml(topic.topic)}</span>
            ${topic.alignsWithGoals ? `<span class="goal-badge" title="${t('chat.topicAlignedGoal')}">⭐</span>` : ''}
        </button>
    `).join('');
    
    return `
        <div class="suggestion-layer topic-selection">
            <div class="suggestion-header">
                <button class="back-btn" onclick="window.chatPage.backToActivities()">
                    ${t('chat.backToActivities')}
                </button>
                <span class="suggestion-label">${t('chat.selectTopic')}</span>
            </div>
            <div class="suggestion-buttons topic-buttons">
                ${topicButtons}
                <button class="chat-suggestion-btn choose-for-me-btn" onclick="window.chatPage.chooseForMe()">
                    ${t('chat.chooseForMe')}
                </button>
                <button class="chat-suggestion-btn custom-topic-btn" onclick="window.chatPage.showCustomTopicInput()">
                    ${t('chat.customTopic')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Render custom topic input
 */
function renderCustomTopicInput() {
    return `
        <div class="suggestion-layer custom-topic-input">
            <div class="suggestion-header">
                <button class="back-btn" onclick="window.chatPage.backToTopics()">
                    ${t('chat.backToActivities')}
                </button>
                <span class="suggestion-label">${t('chat.enterCustomTopic')}</span>
            </div>
            <div class="custom-topic-row">
                <input type="text" 
                       class="custom-topic-field" 
                       id="customTopicInput"
                       placeholder="${t('chat.customTopicPlaceholder')}"
                       onkeydown="window.chatPage.handleCustomTopicKeyDown(event)"
                       autofocus />
                <button class="chat-suggestion-btn start-btn" onclick="window.chatPage.startWithCustomTopic()">
                    ${t('chat.startWithTopic')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Render default suggestions
 */
function renderDefaultSuggestions() {
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
        
        // Show activity selection by default for returning users
        showActivitySelection();
        
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
        
        // Show activity selection after greeting
        showActivitySelection();
        
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
 * Show activity selection (Layer 1)
 */
function showActivitySelection() {
    state.selectionState = SelectionState.SELECTING_ACTIVITY;
    state.selectedActivity = null;
    state.topicSuggestions = [];
    state.randomTopic = null;
    state.suggestions = [];
    refreshSuggestionsUI();
}

/**
 * Handle activity selection (Layer 1 -> Layer 2)
 */
export async function selectActivity(activityKey) {
    const mapping = ActivityMapping[activityKey];
    if (!mapping) {
        console.error('Unknown activity:', activityKey);
        return;
    }
    
    state.selectedActivity = { key: activityKey, ...mapping };
    state.selectionState = SelectionState.LOADING_TOPICS;
    refreshSuggestionsUI();
    
    try {
        // Fetch topic suggestions from backend
        const response = await apiClient.chat.getTopicSuggestions(mapping.category, 5, true);
        
        state.topicSuggestions = response.suggestions || [];
        state.randomTopic = response.randomTopic;
        state.selectionState = SelectionState.SELECTING_TOPIC;
        refreshSuggestionsUI();
        
    } catch (error) {
        console.error('Failed to get topic suggestions:', error);
        // Fallback to activity selection
        showActivitySelection();
        showToast(t('chat.error'), 'error');
    }
}

/**
 * Handle "Surprise me!" - random activity with random topic
 */
export async function surpriseMe() {
    state.isLoading = true;
    refreshUI();
    showTypingIndicator();
    
    try {
        // Pick a random activity
        const activityKeys = Object.keys(ActivityMapping);
        const randomActivityKey = activityKeys[Math.floor(Math.random() * activityKeys.length)];
        const mapping = ActivityMapping[randomActivityKey];
        
        // Get a random topic for this activity
        const topic = await apiClient.chat.selectRandomTopic(mapping.category);
        
        // Send the request to the coach
        await executeActivityWithTopic(randomActivityKey, topic);
        
    } catch (error) {
        console.error('Failed to surprise:', error);
        hideTypingIndicator();
        state.isLoading = false;
        refreshUI();
        showToast(t('chat.error'), 'error');
    }
}

/**
 * Handle topic selection (Layer 2)
 */
export function selectTopic(index) {
    const topic = state.topicSuggestions[index];
    if (topic) {
        executeActivityWithTopic(state.selectedActivity.key, topic.topic);
    }
}

/**
 * Handle "Choose for me" - use random topic
 */
export function chooseForMe() {
    if (state.randomTopic) {
        executeActivityWithTopic(state.selectedActivity.key, state.randomTopic);
    } else {
        // Fallback: select first topic suggestion
        if (state.topicSuggestions.length > 0) {
            selectTopic(0);
        }
    }
}

/**
 * Show custom topic input
 */
export function showCustomTopicInput() {
    state.selectionState = SelectionState.CUSTOM_TOPIC;
    refreshSuggestionsUI();
    
    // Focus the input after render
    setTimeout(() => {
        const input = document.getElementById('customTopicInput');
        if (input) input.focus();
    }, 50);
}

/**
 * Handle custom topic key down
 */
export function handleCustomTopicKeyDown(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        startWithCustomTopic();
    } else if (event.key === 'Escape') {
        backToTopics();
    }
}

/**
 * Start activity with custom topic
 */
export function startWithCustomTopic() {
    const input = document.getElementById('customTopicInput');
    const topic = input?.value?.trim();
    
    if (!topic) {
        return;
    }
    
    executeActivityWithTopic(state.selectedActivity.key, topic);
}

/**
 * Go back to activity selection
 */
export function backToActivities() {
    showActivitySelection();
}

/**
 * Go back to topic selection
 */
export function backToTopics() {
    state.selectionState = SelectionState.SELECTING_TOPIC;
    refreshSuggestionsUI();
}

/**
 * Execute activity with selected topic
 */
async function executeActivityWithTopic(activityKey, topic) {
    const mapping = ActivityMapping[activityKey];
    if (!mapping || !topic) return;
    
    // Record topic usage for history
    try {
        await apiClient.chat.recordTopicUsage(topic, mapping.category);
    } catch (e) {
        console.warn('Failed to record topic usage:', e);
    }
    
    // Build the message for the coach using i18n translations (user's native language)
    const activityRequestKeys = {
        vocabulary: 'chat.requestVocabulary',
        exercise: 'chat.requestExercise',
        lesson: 'chat.requestLesson',
        scenario: 'chat.requestScenario',
        audio: 'chat.requestAudio'
    };
    
    const requestKey = activityRequestKeys[activityKey] || 'chat.requestDefault';
    const message = t(requestKey, { topic });
    
    // Reset selection state
    state.selectionState = SelectionState.IDLE;
    state.selectedActivity = null;
    state.topicSuggestions = [];
    state.randomTopic = null;
    
    // Send as user message
    state.isLoading = true;
    refreshUI();
    
    // Add user message to UI immediately
    const userMessage = {
        id: 'temp-' + Date.now(),
        role: 'USER',
        content: message,
        createdAt: new Date().toISOString()
    };
    state.messages.push(userMessage);
    refreshUI();
    scrollToBottom();
    showTypingIndicator();
    
    try {
        const response = await apiClient.chat.sendMessage(state.sessionId, message);
        
        // Remove temp message and add real messages
        state.messages = state.messages.filter(m => !m.id.toString().startsWith('temp-'));
        state.messages.push(userMessage);
        state.messages.push(response.message);
        
        hideTypingIndicator();
        state.isLoading = false;
        
        // Show activity selection again after completing
        showActivitySelection();
        refreshUI();
        
    } catch (error) {
        console.error('Failed to send message:', error);
        hideTypingIndicator();
        state.isLoading = false;
        state.messages = state.messages.filter(m => !m.id.toString().startsWith('temp-'));
        showActivitySelection();
        refreshUI();
        showToast(t('chat.error'), 'error');
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
        state.selectionState = SelectionState.IDLE;
        await loadOrCreateSession();
    } catch (error) {
        console.error('Failed to create new session:', error);
        showToast(t('chat.error'), 'error');
    }
}

/**
 * Send a message (manual text input)
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
    state.selectionState = SelectionState.IDLE;
    
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
        state.messages.push(userMessage);
        state.messages.push(response.message);
        
        hideTypingIndicator();
        state.isLoading = false;
        
        // Show activity selection after response
        showActivitySelection();
        refreshUI();
        
    } catch (error) {
        console.error('Failed to send message:', error);
        hideTypingIndicator();
        state.isLoading = false;
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
 * Show typing indicator with "thinking" message
 */
function showTypingIndicator() {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    
    const indicator = document.createElement('div');
    indicator.id = 'typingIndicator';
    indicator.className = 'chat-message assistant';
    indicator.innerHTML = `
        <div class="typing-indicator">
            <div class="typing-content">
                <div class="chat-loading-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <span class="typing-text">${t('chat.thinking')}</span>
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
 * Refresh the entire UI
 */
function refreshUI() {
    const messagesContainer = document.getElementById('chatMessages');
    const sendBtn = document.getElementById('chatSendBtn');
    
    if (messagesContainer) {
        messagesContainer.innerHTML = renderMessages();
    }
    
    refreshSuggestionsUI();
    
    if (sendBtn) {
        sendBtn.disabled = state.isLoading;
    }
}

/**
 * Refresh only the suggestions UI
 */
function refreshSuggestionsUI() {
    const suggestionsContainer = document.getElementById('chatSuggestions');
    if (suggestionsContainer) {
        suggestionsContainer.innerHTML = renderSuggestions();
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
    autoResize,
    // Layer 1
    selectActivity,
    surpriseMe,
    // Layer 2
    selectTopic,
    chooseForMe,
    showCustomTopicInput,
    handleCustomTopicKeyDown,
    startWithCustomTopic,
    backToActivities,
    backToTopics
};