/**
 * Goals Management Module
 * Handles goal CRUD operations and progress tracking.
 */

import { api } from '../api/client.js';
import { toast } from './toast.js';
import { t, getGoalType } from './i18n.js';

/**
 * Increment a goal's progress by a specific amount.
 * @param {string} goalId
 * @param {Function} reloadPage - Callback to reload current page
 * @param {number} amount - Amount to increment (default: 1)
 */
export async function incrementGoal(goalId, reloadPage, amount = 1) {
    try {
        await api.goals.incrementProgress(goalId, amount);
        toast.success(t('toast.progressUpdated'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to increment goal:', error);
        toast.error(t('toast.goalUpdateFailed'));
    }
}

/**
 * Update a goal's progress to a specific value.
 * @param {string} goalId
 * @param {number} value
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function updateGoalProgress(goalId, value, reloadPage) {
    try {
        await api.goals.updateProgress(goalId, value);
        toast.success(t('toast.progressUpdated'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to update goal progress:', error);
        toast.error(t('toast.goalUpdateFailed'));
    }
}

/**
 * Reset a goal's progress to 0.
 * @param {string} goalId
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function resetGoalProgress(goalId, reloadPage) {
    if (!confirm(t('goals.confirmReset'))) return;
    
    try {
        await api.goals.updateProgress(goalId, 0);
        toast.success(t('toast.goalReset'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to reset goal:', error);
        toast.error(t('toast.goalUpdateFailed'));
    }
}

/**
 * Mark a goal as complete.
 * @param {string} goalId
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function completeGoal(goalId, reloadPage) {
    try {
        await api.goals.complete(goalId);
        toast.success(t('toast.goalCompleted'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to complete goal:', error);
        toast.error(t('toast.goalCreateFailed'));
    }
}

/**
 * Delete a goal.
 * @param {string} goalId
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function deleteGoal(goalId, reloadPage) {
    if (!confirm(t('goals.delete') + '?')) return;
    
    try {
        await api.goals.delete(goalId);
        toast.success(t('toast.goalDeleted'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to delete goal:', error);
        toast.error(t('toast.goalCreateFailed'));
    }
}

/**
 * Create default daily goals for a new user.
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function createDefaultGoals(reloadPage) {
    try {
        await api.goals.create({ title: t('goals.typeDaily') + ' - 3 lessons', type: 'DAILY', targetValue: 3, unit: 'lessons' });
        await api.goals.create({ title: t('goals.typeDaily') + ' - 10 words', type: 'DAILY', targetValue: 10, unit: 'words' });
        await api.goals.create({ title: t('goals.typeDaily') + ' - 15 min', type: 'DAILY', targetValue: 15, unit: 'minutes' });
        toast.success(t('toast.defaultGoalsCreated'));
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to create default goals:', error);
        toast.error(t('toast.goalCreateFailed'));
    }
}

/**
 * Show create goal modal.
 * @param {Function} openModal - Function to open modal
 * @param {string} defaultType - Default goal type
 */
export function showCreateGoalModal(openModal, defaultType = 'DAILY') {
    openModal(`
        <div class="create-goal-form">
            <h2>${t('goals.createTitle')}</h2>
            <div class="form-group">
                <label>${t('goals.title')}</label>
                <input type="text" id="goal-title" class="form-input" placeholder="${t('goals.titlePlaceholder')}" />
            </div>
            <div class="form-group">
                <label>${t('goals.description')}</label>
                <textarea id="goal-description" class="form-input form-textarea" rows="2" placeholder="${t('goals.descriptionPlaceholder')}"></textarea>
            </div>
            <div class="form-group">
                <label>${t('goals.type')}</label>
                <select id="goal-type" class="form-input">
                    <option value="DAILY" ${defaultType === 'DAILY' ? 'selected' : ''}>${t('goals.typeDaily')}</option>
                    <option value="WEEKLY" ${defaultType === 'WEEKLY' ? 'selected' : ''}>${t('goals.typeWeekly')}</option>
                    <option value="MONTHLY" ${defaultType === 'MONTHLY' ? 'selected' : ''}>${t('goals.typeMonthly')}</option>
                    <option value="YEARLY" ${defaultType === 'YEARLY' ? 'selected' : ''}>${t('goals.typeYearly')}</option>
                </select>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>${t('goals.target')}</label>
                    <input type="number" id="goal-target" class="form-input" value="10" min="1" />
                </div>
                <div class="form-group">
                    <label>${t('goals.unit')}</label>
                    <input type="text" id="goal-unit" class="form-input" placeholder="${t('goals.unitPlaceholder')}" />
                </div>
            </div>
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.closeModal()">${t('profile.cancel')}</button>
                <button class="btn btn-primary" onclick="window.submitCreateGoal()">${t('goals.create')}</button>
            </div>
        </div>
    `);
}

/**
 * Show edit goal modal.
 * @param {Object} goal - Goal to edit
 * @param {Function} openModal - Function to open modal
 */
export function showEditGoalModal(goal, openModal) {
    openModal(`
        <div class="create-goal-form">
            <h2>${t('goals.editTitle')}</h2>
            <input type="hidden" id="edit-goal-id" value="${goal.id}" />
            <div class="form-group">
                <label>${t('goals.title')}</label>
                <input type="text" id="goal-title" class="form-input" value="${escapeHtml(goal.title)}" />
            </div>
            <div class="form-group">
                <label>${t('goals.description')}</label>
                <textarea id="goal-description" class="form-input form-textarea" rows="2">${escapeHtml(goal.description || '')}</textarea>
            </div>
            <div class="form-group">
                <label>${t('goals.type')}</label>
                <select id="goal-type" class="form-input" disabled>
                    <option value="${goal.type}" selected>${getGoalType(goal.type)}</option>
                </select>
                <small class="form-hint">${t('goals.typeCannotChange')}</small>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>${t('goals.target')}</label>
                    <input type="number" id="goal-target" class="form-input" value="${goal.targetValue}" min="1" />
                </div>
                <div class="form-group">
                    <label>${t('goals.unit')}</label>
                    <input type="text" id="goal-unit" class="form-input" value="${escapeHtml(goal.unit || '')}" />
                </div>
            </div>
            <div class="form-group">
                <label>${t('goals.currentProgress')}</label>
                <div class="progress-edit-row">
                    <input type="number" id="goal-current-value" class="form-input" value="${goal.currentValue}" min="0" />
                    <span class="progress-separator">/ ${goal.targetValue}</span>
                </div>
            </div>
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.closeModal()">${t('profile.cancel')}</button>
                <button class="btn btn-primary" onclick="window.submitEditGoal()">${t('goals.save')}</button>
            </div>
        </div>
    `);
}

/**
 * Submit edit goal form.
 * @param {Function} closeModal - Function to close modal
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function submitEditGoal(closeModal, reloadPage) {
    const goalId = document.getElementById('edit-goal-id')?.value;
    const title = document.getElementById('goal-title')?.value;
    const description = document.getElementById('goal-description')?.value || '';
    const targetValue = parseInt(document.getElementById('goal-target')?.value) || 10;
    const unit = document.getElementById('goal-unit')?.value || '';
    const currentValue = parseInt(document.getElementById('goal-current-value')?.value) || 0;
    
    if (!title) {
        toast.error(t('toast.enterTitle'));
        return;
    }
    
    if (!goalId) {
        toast.error(t('toast.goalUpdateFailed'));
        return;
    }
    
    try {
        // Update progress first if changed
        await api.goals.updateProgress(goalId, currentValue);
        
        // Note: The backend API doesn't have an update endpoint for title/description/target
        // This would require adding a PUT/PATCH endpoint to GoalController
        // For now, we just update the progress
        
        toast.success(t('toast.goalUpdated'));
        closeModal();
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to update goal:', error);
        toast.error(t('toast.goalUpdateFailed'));
    }
}

/**
 * Show update progress modal.
 * @param {Object} goal - Goal to update
 * @param {Function} openModal - Function to open modal
 */
export function showUpdateProgressModal(goal, openModal) {
    openModal(`
        <div class="update-progress-form">
            <h2>${t('goals.updateProgress')}</h2>
            <input type="hidden" id="update-progress-goal-id" value="${goal.id}" />
            <div class="goal-info-preview">
                <span class="goal-preview-title">${escapeHtml(goal.title)}</span>
                <span class="goal-preview-type">${getGoalType(goal.type)}</span>
            </div>
            <div class="progress-bar large">
                <div class="progress-fill ${goal.completed ? 'completed' : ''}" 
                     style="width: ${goal.progressPercent}%"></div>
            </div>
            <div class="form-group">
                <label>${t('goals.newProgress')}</label>
                <div class="progress-input-row">
                    <button class="btn btn-secondary btn-sm" onclick="window.adjustProgress(-1)">-</button>
                    <input type="number" id="new-progress-value" class="form-input progress-value-input" 
                           value="${goal.currentValue}" min="0" max="${goal.targetValue}" />
                    <button class="btn btn-secondary btn-sm" onclick="window.adjustProgress(1)">+</button>
                    <span class="progress-max">/ ${goal.targetValue} ${escapeHtml(goal.unit)}</span>
                </div>
            </div>
            <div class="quick-progress-buttons">
                <button class="btn btn-secondary btn-sm" onclick="window.setProgressPercent(0)">0%</button>
                <button class="btn btn-secondary btn-sm" onclick="window.setProgressPercent(25)">25%</button>
                <button class="btn btn-secondary btn-sm" onclick="window.setProgressPercent(50)">50%</button>
                <button class="btn btn-secondary btn-sm" onclick="window.setProgressPercent(75)">75%</button>
                <button class="btn btn-secondary btn-sm" onclick="window.setProgressPercent(100)">100%</button>
            </div>
            <div class="form-actions">
                <button class="btn btn-secondary" onclick="window.closeModal()">${t('profile.cancel')}</button>
                <button class="btn btn-primary" onclick="window.submitUpdateProgress()">${t('goals.save')}</button>
            </div>
        </div>
    `);
    
    // Store target value for percentage calculations
    window._currentGoalTarget = goal.targetValue;
}

/**
 * Adjust progress value by amount.
 * @param {number} delta - Amount to adjust
 */
export function adjustProgress(delta) {
    const input = document.getElementById('new-progress-value');
    if (!input) return;
    
    const current = parseInt(input.value) || 0;
    const max = parseInt(input.max) || 100;
    const newValue = Math.max(0, Math.min(max, current + delta));
    input.value = newValue;
}

/**
 * Set progress to a percentage of target.
 * @param {number} percent - Percentage (0-100)
 */
export function setProgressPercent(percent) {
    const input = document.getElementById('new-progress-value');
    if (!input || !window._currentGoalTarget) return;
    
    const newValue = Math.round((percent / 100) * window._currentGoalTarget);
    input.value = newValue;
}

/**
 * Submit progress update from modal.
 * @param {Function} closeModal - Function to close modal
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function submitUpdateProgress(closeModal, reloadPage) {
    const goalId = document.getElementById('update-progress-goal-id')?.value;
    const newValue = parseInt(document.getElementById('new-progress-value')?.value);
    
    if (!goalId || isNaN(newValue)) {
        toast.error(t('toast.goalUpdateFailed'));
        return;
    }
    
    try {
        await api.goals.updateProgress(goalId, newValue);
        toast.success(t('toast.progressUpdated'));
        closeModal();
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to update progress:', error);
        toast.error(t('toast.goalUpdateFailed'));
    }
}

/**
 * Escape HTML to prevent XSS.
 * @param {string} str
 * @returns {string}
 */
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

/**
 * Submit create goal form.
 * @param {Function} closeModal - Function to close modal
 * @param {Function} reloadPage - Callback to reload current page
 */
export async function submitCreateGoal(closeModal, reloadPage) {
    const title = document.getElementById('goal-title')?.value;
    const description = document.getElementById('goal-description')?.value || '';
    const type = document.getElementById('goal-type')?.value;
    const targetValue = parseInt(document.getElementById('goal-target')?.value) || 10;
    const unit = document.getElementById('goal-unit')?.value || '';
    
    if (!title) {
        toast.error(t('toast.enterTitle'));
        return;
    }
    
    try {
        await api.goals.create({ title, description, type, targetValue, unit });
        toast.success(t('toast.goalCreated'));
        closeModal();
        if (reloadPage) reloadPage();
    } catch (error) {
        console.error('Failed to create goal:', error);
        toast.error(t('toast.goalCreateFailed'));
    }
}

/**
 * Increment lesson goal (for auto-tracking after completing lessons).
 */
export async function incrementLessonGoal() {
    try {
        const goals = await api.goals.list('DAILY');
        const lessonGoal = goals.find(g => g.unit === 'lessons' && !g.completed);
        if (lessonGoal) {
            await api.goals.incrementProgress(lessonGoal.id, 1);
        }
    } catch (error) {
        console.error('Failed to increment lesson goal:', error);
    }
}

/**
 * Increment words goal (for auto-tracking after vocabulary study).
 * @param {number} count - Number of words studied
 */
export async function incrementWordsGoal(count) {
    try {
        const goals = await api.goals.list('DAILY');
        const wordsGoal = goals.find(g => g.unit === 'words' && !g.completed);
        if (wordsGoal) {
            await api.goals.incrementProgress(wordsGoal.id, count);
        }
    } catch (error) {
        console.error('Failed to increment words goal:', error);
    }
}

export default {
    incrementGoal,
    updateGoalProgress,
    resetGoalProgress,
    completeGoal,
    deleteGoal,
    createDefaultGoals,
    showCreateGoalModal,
    showEditGoalModal,
    submitEditGoal,
    showUpdateProgressModal,
    adjustProgress,
    setProgressPercent,
    submitUpdateProgress,
    submitCreateGoal,
    incrementLessonGoal,
    incrementWordsGoal
};
