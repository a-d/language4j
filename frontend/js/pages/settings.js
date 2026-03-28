/**
 * Settings Page Module
 * Handles user settings and profile management.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t, getLanguageName, getSkillLevelDescription, setLanguage, reloadTranslations } from '../services/i18n.js';

/**
 * Available languages for selection.
 * Uses common ISO 639-1 codes with human-readable names.
 */
const AVAILABLE_LANGUAGES = [
    { code: 'de', name: 'Deutsch (German)' },
    { code: 'en', name: 'English' },
    { code: 'es', name: 'Español (Spanish)' },
    { code: 'fr', name: 'Français (French)' },
    { code: 'it', name: 'Italiano (Italian)' },
    { code: 'ja', name: '日本語 (Japanese)' },
    { code: 'ko', name: '한국어 (Korean)' },
    { code: 'nl', name: 'Nederlands (Dutch)' },
    { code: 'pl', name: 'Polski (Polish)' },
    { code: 'pt', name: 'Português (Portuguese)' },
    { code: 'ru', name: 'Русский (Russian)' },
    { code: 'zh', name: '中文 (Chinese)' },
    { code: 'ar', name: 'العربية (Arabic)' },
    { code: 'hi', name: 'हिन्दी (Hindi)' },
    { code: 'tr', name: 'Türkçe (Turkish)' },
    { code: 'sv', name: 'Svenska (Swedish)' },
    { code: 'da', name: 'Dansk (Danish)' },
    { code: 'fi', name: 'Suomi (Finnish)' },
    { code: 'no', name: 'Norsk (Norwegian)' },
    { code: 'cs', name: 'Čeština (Czech)' },
    { code: 'el', name: 'Ελληνικά (Greek)' },
    { code: 'he', name: 'עברית (Hebrew)' },
    { code: 'hu', name: 'Magyar (Hungarian)' },
    { code: 'id', name: 'Bahasa Indonesia' },
    { code: 'th', name: 'ไทย (Thai)' },
    { code: 'uk', name: 'Українська (Ukrainian)' },
    { code: 'vi', name: 'Tiếng Việt (Vietnamese)' }
];

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
    const avatarEl = document.getElementById('settings-avatar');
    
    if (displayNameEl) displayNameEl.textContent = user.displayName;
    if (nativeLangEl) nativeLangEl.textContent = nativeLang;
    if (targetLangEl) targetLangEl.textContent = targetLang;
    if (skillLevelEl) skillLevelEl.textContent = user.skillLevel;
    
    // Update avatar with user initial
    if (avatarEl && user.displayName) {
        avatarEl.textContent = user.displayName.charAt(0).toUpperCase();
    }
    
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
    
    // Setup delete user button if not already set up
    setupDeleteUserButton();
}

/**
 * Setup the delete user button in settings.
 */
function setupDeleteUserButton() {
    const profileCard = document.getElementById('settings-profile-card');
    if (!profileCard) return;
    
    // Check if delete button already exists
    if (profileCard.querySelector('.btn-danger')) return;
    
    // Add delete button next to edit button
    const editBtn = profileCard.querySelector('.btn-primary');
    if (editBtn) {
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger btn-sm';
        deleteBtn.style.marginLeft = '8px';
        deleteBtn.textContent = `🗑️ ${t('users.deleteAccount')}`;
        deleteBtn.onclick = () => window.deleteCurrentUser();
        editBtn.parentNode.appendChild(deleteBtn);
    }
}

/**
 * Generate language select options HTML.
 * @param {string} selectedCode - Currently selected language code
 * @returns {string} HTML options string
 */
function generateLanguageOptions(selectedCode) {
    return AVAILABLE_LANGUAGES.map(lang => 
        `<option value="${lang.code}" ${lang.code === selectedCode ? 'selected' : ''}>${lang.name}</option>`
    ).join('');
}

/**
 * Update the language preview in the profile modal.
 */
function updateLanguagePreview() {
    const nativeSelect = document.getElementById('profile-native-language');
    const targetSelect = document.getElementById('profile-target-language');
    const previewEl = document.querySelector('.preview-languages');
    
    if (nativeSelect && targetSelect && previewEl) {
        const nativeName = AVAILABLE_LANGUAGES.find(l => l.code === nativeSelect.value)?.name.split(' ')[0] || nativeSelect.value;
        const targetName = AVAILABLE_LANGUAGES.find(l => l.code === targetSelect.value)?.name.split(' ')[0] || targetSelect.value;
        previewEl.textContent = `${nativeName} → ${targetName}`;
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
                <label for="profile-native-language">${t('profile.nativeLanguage')}</label>
                <select id="profile-native-language" class="form-input">
                    ${generateLanguageOptions(user.nativeLanguage)}
                </select>
                <small class="text-muted">${t('profile.nativeLanguageHint')}</small>
            </div>
            
            <div class="form-group">
                <label for="profile-target-language">${t('profile.targetLanguage')}</label>
                <select id="profile-target-language" class="form-input">
                    ${generateLanguageOptions(user.targetLanguage)}
                </select>
                <small class="text-muted">${t('profile.targetLanguageHint')}</small>
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
    
    // Add language preview update listeners
    document.getElementById('profile-native-language')?.addEventListener('change', updateLanguagePreview);
    document.getElementById('profile-target-language')?.addEventListener('change', updateLanguagePreview);
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
    const nativeLanguage = document.getElementById('profile-native-language')?.value;
    const targetLanguage = document.getElementById('profile-target-language')?.value;
    
    if (!displayName) {
        toast.error(t('toast.enterDisplayName'));
        return;
    }
    
    // Validate languages are different
    if (nativeLanguage && targetLanguage && nativeLanguage === targetLanguage) {
        toast.error(t('toast.languagesMustBeDifferent'));
        return;
    }
    
    try {
        const updateData = {
            displayName: displayName,
            skillLevel: skillLevel,
            nativeLanguage: nativeLanguage,
            targetLanguage: targetLanguage
        };
        
        // Get the previous native language to check if it changed
        const previousUser = await api.users.getCurrent();
        const previousNativeLanguage = previousUser.nativeLanguage;
        
        const updatedUser = await api.users.update(updateData);
        updateUserState(updatedUser);
        updateUserDisplay();
        
        // If native language changed, reload translations
        if (previousNativeLanguage !== updatedUser.nativeLanguage) {
            toast.info(t('toast.reloadingTranslations'));
            await setLanguage(updatedUser.nativeLanguage);
            // Force reload of translations for the new language
            await reloadTranslations(updatedUser.nativeLanguage);
        }
        
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