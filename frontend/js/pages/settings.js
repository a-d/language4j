/**
 * Settings Page Module
 * Handles user settings and profile management.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t, getLanguageName, getSkillLevelDescription } from '../services/i18n.js';

/**
 * Load settings page data.
 * @param {Object} user - Current user object
 */
export function loadSettingsData(user) {
    if (!user) return;
    
    const nativeLang = getLanguageName(user.nativeLanguage);
    const targetLang = getLanguageName(user.targetLanguage);
    
    const displayNameEl = document.getElementById('settings-display-name');
    const nativeLangEl = document.getElementById('settings-native-lang');
    const targetLangEl = document.getElementById('settings-target-lang');
    const skillLevelEl = document.getElementById('settings-skill-level');
    const memberSinceEl = document.getElementById('settings-member-since');
    
    if (displayNameEl) displayNameEl.textContent = user.displayName;
    if (nativeLangEl) nativeLangEl.textContent = nativeLang;
    if (targetLangEl) targetLangEl.textContent = targetLang;
    if (skillLevelEl) skillLevelEl.textContent = user.skillLevel;
    
    if (memberSinceEl) {
        if (user.createdAt) {
            const date = new Date(user.createdAt);
            memberSinceEl.textContent = date.toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        } else {
            memberSinceEl.textContent = '--';
        }
    }
}

/**
 * Show edit profile modal.
 * @param {Object} user - Current user object
 * @param {Function} openModal - Function to open modal
 */
export function showEditProfileModal(user, openModal) {
    if (!user) {
        toast.error(t('toast.userDataNotLoaded'));
        return;
    }
    
    const nativeLang = getLanguageName(user.nativeLanguage);
    const targetLang = getLanguageName(user.targetLanguage);
    const currentLevel = user.skillLevel || 'A1';
    
    const skillLevels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
    
    openModal(`
        <div class="edit-profile-form">
            <h2>${t('profile.editTitle')}</h2>
            
            <div class="profile-preview">
                <div class="profile-preview-avatar">👤</div>
                <div class="profile-preview-info">
                    <span class="preview-name" id="preview-name">${user.displayName}</span>
                    <span class="preview-languages">${nativeLang} → ${targetLang}</span>
                </div>
            </div>
            
            <div class="form-group">
                <label for="profile-display-name">${t('profile.displayName')}</label>
                <input type="text" id="profile-display-name" class="form-input" 
                       value="${user.displayName}" 
                       placeholder="${t('profile.displayNamePlaceholder')}"
                       oninput="document.getElementById('preview-name').textContent = this.value || 'Learner'" />
            </div>
            
            <div class="form-group">
                <label>${t('profile.skillLevel')}</label>
                <div class="skill-level-selector">
                    ${skillLevels.map(level => `
                        <div class="skill-level-option">
                            <input type="radio" name="skill-level" id="level-${level}" value="${level}" 
                                   ${level === currentLevel ? 'checked' : ''} />
                            <label for="level-${level}">${level}</label>
                        </div>
                    `).join('')}
                </div>
                <p class="skill-level-description" id="skill-level-desc">${getSkillLevelDescription(currentLevel)}</p>
            </div>
            
            <div class="form-group">
                <label>${t('profile.languages')}</label>
                <p class="text-muted" style="margin: 0; font-size: var(--font-size-sm);">
                    ${nativeLang} → ${targetLang}
                    <br><small>${t('profile.languagesNote')}</small>
                </p>
            </div>
            
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.closeModal()">${t('profile.cancel')}</button>
                <button class="btn btn-primary" onclick="window.submitProfileUpdate()">${t('profile.save')}</button>
            </div>
        </div>
    `);
    
    // Add skill level description update listener
    document.querySelectorAll('input[name="skill-level"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const descEl = document.getElementById('skill-level-desc');
            if (descEl) descEl.textContent = getSkillLevelDescription(e.target.value);
        });
    });
}

/**
 * Submit profile update to API.
 * @param {Function} updateUserState - Callback to update app state
 * @param {Function} updateUserDisplay - Callback to update UI
 * @param {Function} closeModal - Function to close modal
 * @param {string} currentPage - Current page name
 * @param {Function} loadSettingsDataFn - Function to reload settings
 */
export async function submitProfileUpdate(updateUserState, updateUserDisplay, closeModal, currentPage, loadSettingsDataFn) {
    const displayName = document.getElementById('profile-display-name')?.value.trim();
    const skillLevel = document.querySelector('input[name="skill-level"]:checked')?.value;
    
    if (!displayName) {
        toast.error(t('toast.enterDisplayName'));
        return;
    }
    
    try {
        const updateData = {
            displayName: displayName,
            skillLevel: skillLevel
        };
        
        const updatedUser = await api.users.update(updateData);
        updateUserState(updatedUser);
        updateUserDisplay();
        
        if (currentPage === 'settings') {
            loadSettingsDataFn(updatedUser);
        }
        
        toast.success(t('toast.profileUpdated'));
        closeModal();
    } catch (error) {
        console.error('Failed to update profile:', error);
        toast.error(t('toast.profileUpdateFailed'));
    }
}

export default { loadSettingsData, showEditProfileModal, submitProfileUpdate };