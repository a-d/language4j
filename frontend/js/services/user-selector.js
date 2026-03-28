/**
 * User Selector Service
 * =====================
 * Handles user selection, creation, and switching for multi-user support.
 * Displays a modal for selecting or creating users.
 */

import { api, setSelectedUserId, clearSelectedUserId, getSelectedUserId } from '../api/client.js';
import { t, getLanguageName } from './i18n.js';
import { cache } from './cache.js';

/**
 * Available languages for user creation
 */
const AVAILABLE_LANGUAGES = [
    { code: 'en', name: 'English' },
    { code: 'de', name: 'German' },
    { code: 'fr', name: 'French' },
    { code: 'es', name: 'Spanish' },
    { code: 'it', name: 'Italian' },
    { code: 'pt', name: 'Portuguese' },
    { code: 'nl', name: 'Dutch' },
    { code: 'pl', name: 'Polish' },
    { code: 'ru', name: 'Russian' },
    { code: 'ja', name: 'Japanese' },
    { code: 'ko', name: 'Korean' },
    { code: 'zh', name: 'Chinese' },
    { code: 'ar', name: 'Arabic' },
    { code: 'tr', name: 'Turkish' },
    { code: 'sv', name: 'Swedish' },
    { code: 'da', name: 'Danish' },
    { code: 'fi', name: 'Finnish' },
    { code: 'no', name: 'Norwegian' }
];

/**
 * Generate initial/avatar from display name
 * @param {string} name - Display name
 * @returns {string} First character uppercase
 */
export function getInitial(name) {
    return name ? name.charAt(0).toUpperCase() : '?';
}

/**
 * Generate a color based on user ID for avatar background
 * @param {string} userId - User ID
 * @returns {string} CSS hsl color
 */
export function getUserColor(userId) {
    // Simple hash to generate hue
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
        hash = userId.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 65%, 45%)`;
}

/**
 * Render the user selection modal HTML
 * @param {Array} users - List of users
 * @param {boolean} showCreateForm - Whether to show the create form
 * @returns {string} HTML string
 */
function renderUserSelectorModal(users, showCreateForm = false) {
    const languageOptions = AVAILABLE_LANGUAGES.map(lang => 
        `<option value="${lang.code}">${lang.name}</option>`
    ).join('');
    
    const userCards = users.map(user => `
        <div class="user-card" data-user-id="${user.id}" onclick="window.selectUser('${user.id}')">
            <div class="user-avatar" style="background-color: ${getUserColor(user.id)}">
                ${getInitial(user.displayName)}
            </div>
            <div class="user-info">
                <div class="user-name">${user.displayName}</div>
                <div class="user-languages">
                    ${getLanguageName(user.nativeLanguage)} → ${getLanguageName(user.targetLanguage)}
                </div>
                <div class="user-level">${user.skillLevel}</div>
            </div>
        </div>
    `).join('');
    
    const createFormHtml = `
        <div class="user-create-form ${showCreateForm ? '' : 'hidden'}" id="user-create-form">
            <h3>${t('users.createTitle')}</h3>
            <div class="form-group">
                <label for="new-user-name">${t('users.name')}</label>
                <input type="text" id="new-user-name" placeholder="${t('users.namePlaceholder')}" maxlength="50">
            </div>
            <div class="form-group">
                <label for="new-user-native">${t('users.nativeLanguage')}</label>
                <select id="new-user-native">
                    ${languageOptions}
                </select>
            </div>
            <div class="form-group">
                <label for="new-user-target">${t('users.targetLanguage')}</label>
                <select id="new-user-target">
                    ${languageOptions}
                </select>
            </div>
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.toggleCreateUserForm(false)">${t('misc.cancel')}</button>
                <button class="btn btn-primary" onclick="window.submitCreateUser()">${t('users.create')}</button>
            </div>
        </div>
    `;
    
    return `
        <div class="user-selector-modal">
            <div class="user-selector-header">
                <h2>${t('users.selectUser')}</h2>
                <p>${t('users.selectUserDesc')}</p>
            </div>
            
            <div class="user-list ${showCreateForm ? 'hidden' : ''}" id="user-list">
                ${users.length > 0 ? userCards : `
                    <div class="no-users">
                        <p>${t('users.noUsers')}</p>
                    </div>
                `}
            </div>
            
            ${createFormHtml}
            
            <div class="user-selector-footer ${showCreateForm ? 'hidden' : ''}" id="user-selector-footer">
                <button class="btn btn-secondary" onclick="window.toggleCreateUserForm(true)">
                    ➕ ${t('users.createNew')}
                </button>
            </div>
        </div>
    `;
}

/**
 * Show the user selector modal
 * @param {Function} openModal - Function to open modal
 * @param {boolean} forceShow - If true, show even if a user is already selected
 * @returns {Promise<void>}
 */
export async function showUserSelector(openModal, forceShow = false) {
    // Check if user is already selected and valid
    const selectedUserId = getSelectedUserId();
    if (selectedUserId && !forceShow) {
        try {
            // Verify the user still exists
            await api.users.getById(selectedUserId);
            return; // User is valid, no need to show selector
        } catch (error) {
            // User doesn't exist, clear selection and show selector
            clearSelectedUserId();
        }
    }
    
    try {
        const users = await api.users.list();
        
        // If there are no users, show create form immediately
        const showCreateForm = users.length === 0;
        
        openModal(renderUserSelectorModal(users, showCreateForm), {
            closable: users.length > 0 && selectedUserId !== null // Only closable if there are users and one is selected
        });
        
        // Set default language selections based on env config if available
        if (showCreateForm) {
            const nativeSelect = document.getElementById('new-user-native');
            const targetSelect = document.getElementById('new-user-target');
            if (nativeSelect && window.APP_CONFIG?.nativeLanguage) {
                nativeSelect.value = window.APP_CONFIG.nativeLanguage;
            }
            if (targetSelect && window.APP_CONFIG?.targetLanguage) {
                targetSelect.value = window.APP_CONFIG.targetLanguage;
            }
        }
    } catch (error) {
        console.error('Failed to load users:', error);
        // Show create form as fallback
        openModal(renderUserSelectorModal([], true), { closable: false });
    }
}

/**
 * Toggle the create user form visibility
 * @param {boolean} show - Whether to show the form
 */
export function toggleCreateUserForm(show) {
    const form = document.getElementById('user-create-form');
    const list = document.getElementById('user-list');
    const footer = document.getElementById('user-selector-footer');
    
    if (form) form.classList.toggle('hidden', !show);
    if (list) list.classList.toggle('hidden', show);
    if (footer) footer.classList.toggle('hidden', show);
    
    // Focus on name input when showing form
    if (show) {
        const nameInput = document.getElementById('new-user-name');
        if (nameInput) nameInput.focus();
    }
}

/**
 * Submit the create user form
 * @param {Function} closeModal - Function to close modal
 * @param {Function} onUserSelected - Callback when user is selected
 * @returns {Promise<void>}
 */
export async function submitCreateUser(closeModal, onUserSelected) {
    const nameInput = document.getElementById('new-user-name');
    const nativeSelect = document.getElementById('new-user-native');
    const targetSelect = document.getElementById('new-user-target');
    
    const displayName = nameInput?.value?.trim();
    const nativeLanguage = nativeSelect?.value;
    const targetLanguage = targetSelect?.value;
    
    if (!displayName) {
        alert(t('users.nameRequired'));
        nameInput?.focus();
        return;
    }
    
    if (nativeLanguage === targetLanguage) {
        alert(t('users.languagesMustDiffer'));
        return;
    }
    
    try {
        const user = await api.users.create({
            displayName,
            nativeLanguage,
            targetLanguage
        });
        
        // Select the newly created user
        setSelectedUserId(user.id);
        closeModal();
        
        if (onUserSelected) {
            onUserSelected(user);
        }
    } catch (error) {
        console.error('Failed to create user:', error);
        alert(t('users.createFailed') + ': ' + error.message);
    }
}

/**
 * Select a user
 * @param {string} userId - User ID to select
 * @param {Function} closeModal - Function to close modal
 * @param {Function} onUserSelected - Callback when user is selected
 * @returns {Promise<void>}
 */
export async function selectUser(userId, closeModal, onUserSelected) {
    try {
        const user = await api.users.getById(userId);
        setSelectedUserId(userId);
        
        // Clear cache for the previous user's data in this session
        cache.clearAll();
        
        closeModal();
        
        if (onUserSelected) {
            onUserSelected(user);
        }
    } catch (error) {
        console.error('Failed to select user:', error);
        alert(t('users.selectFailed') + ': ' + error.message);
    }
}

/**
 * Show user switcher dropdown/modal for changing users
 * @param {Function} openModal - Function to open modal
 * @param {Function} onUserSelected - Callback when user is selected
 */
export async function showUserSwitcher(openModal, onUserSelected) {
    await showUserSelector(openModal, true);
    
    // Store callbacks for later use
    window._userSelectorCallbacks = { onUserSelected };
}

/**
 * Delete a user
 * @param {string} userId - User ID to delete
 * @param {Function} onDeleted - Callback after deletion
 * @returns {Promise<boolean>} True if deleted successfully
 */
export async function deleteUser(userId, onDeleted) {
    const currentUserId = getSelectedUserId();
    
    try {
        await api.users.delete(userId);
        
        // If we deleted the current user, clear selection
        if (currentUserId === userId) {
            clearSelectedUserId();
            cache.clearAll();
        }
        
        if (onDeleted) {
            onDeleted(userId === currentUserId);
        }
        
        return true;
    } catch (error) {
        console.error('Failed to delete user:', error);
        alert(t('users.deleteFailed') + ': ' + error.message);
        return false;
    }
}

// Export for global window access
export default {
    showUserSelector,
    toggleCreateUserForm,
    submitCreateUser,
    selectUser,
    showUserSwitcher,
    deleteUser,
    getInitial,
    getUserColor
};