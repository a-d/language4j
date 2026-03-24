/**
 * Dashboard Page Module
 * Handles dashboard-specific logic: daily goals, recent activity.
 */

import { api } from '../api/client.js';
import { toast } from '../services/toast.js';
import { t, getGoalType } from '../services/i18n.js';

// Store goals for modal access
let cachedDailyGoals = [];

/**
 * Loads all dashboard data.
 * @param {Function} showLoading
 * @param {Function} hideLoading
 */
export async function loadDashboardData(showLoading, hideLoading) {
    showLoading();
    try {
        await loadDailyGoals();
        await loadRecentActivity();
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Load daily goals from API and render them.
 */
async function loadDailyGoals() {
    const container = document.getElementById('daily-goals');
    if (!container) return;
    
    try {
        const goals = await api.goals.list('DAILY');
        cachedDailyGoals = goals;
        
        if (goals.length === 0) {
            container.innerHTML = `
                <div class="empty-goals-container">
                    <p class="empty-state">${t('dashboard.noGoals')}</p>
                    <div class="empty-goals-actions">
                        <button class="btn btn-primary btn-sm" onclick="window.createDefaultGoals()">${t('dashboard.createDefaultGoals')}</button>
                        <button class="btn btn-secondary btn-sm" onclick="window.showCreateDailyGoalModal()">${t('goals.createCustom')}</button>
                    </div>
                </div>
            `;
            return;
        }
        
        const completedCount = goals.filter(g => g.completed).length;
        const totalCount = goals.length;
        
        container.innerHTML = `
            <div class="goals-summary">
                <span class="goals-summary-text">${completedCount} / ${totalCount} ${t('goals.completed')}</span>
                <button class="btn btn-sm btn-secondary" onclick="window.showCreateDailyGoalModal()">+ ${t('goals.addGoal')}</button>
            </div>
            ${goals.map(goal => renderGoalCard(goal)).join('')}
        `;
    } catch (error) {
        console.error('Failed to load daily goals:', error);
        container.innerHTML = `<p class="empty-state">${t('toast.backendUnavailable')}</p>`;
    }
}

/**
 * Render a single goal card.
 * @param {Object} goal
 * @returns {string} HTML string
 */
function renderGoalCard(goal) {
    const isCompleted = goal.completed;
    const progressClass = isCompleted ? 'completed' : (goal.progressPercent >= 75 ? 'high' : '');
    
    return `
        <div class="goal-card ${isCompleted ? 'goal-completed' : ''}" data-goal-id="${goal.id}">
            <div class="goal-card-header">
                <span class="goal-card-title">${escapeHtml(goal.title)}</span>
                <div class="goal-card-badges">
                    ${isCompleted ? `<span class="goal-badge completed">✓ ${t('goals.done')}</span>` : ''}
                    <span class="goal-card-type">${getGoalType(goal.type)}</span>
                </div>
            </div>
            ${goal.description ? `<p class="goal-card-description">${escapeHtml(goal.description)}</p>` : ''}
            <div class="goal-progress" onclick="window.showUpdateProgressModal('${goal.id}')" title="${t('goals.clickToUpdate')}">
                <div class="progress-bar clickable">
                    <div class="progress-fill ${progressClass}" 
                         style="width: ${goal.progressPercent}%"></div>
                </div>
                <div class="progress-text">
                    <span>${goal.currentValue} / ${goal.targetValue} ${escapeHtml(goal.unit)}</span>
                    <span>${goal.progressPercent}%</span>
                </div>
            </div>
            <div class="goal-actions">
                <button class="btn btn-sm" onclick="window.incrementGoal('${goal.id}')" title="${t('goals.increment')}">+1</button>
                <button class="btn btn-sm" onclick="window.showUpdateProgressModal('${goal.id}')" title="${t('goals.updateProgress')}">📊</button>
                ${!isCompleted ? `<button class="btn btn-sm btn-success" onclick="window.completeGoal('${goal.id}')" title="${t('goals.markComplete')}">✓</button>` : ''}
                <button class="btn btn-sm btn-icon" onclick="window.showGoalMenu('${goal.id}', event)" title="${t('goals.moreActions')}">⋮</button>
            </div>
        </div>
    `;
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
 * Get a cached goal by ID.
 * @param {string} goalId
 * @returns {Object|undefined}
 */
export function getCachedGoal(goalId) {
    return cachedDailyGoals.find(g => g.id === goalId);
}

/**
 * Load recent activity placeholder.
 */
async function loadRecentActivity() {
    const container = document.getElementById('recent-activity');
    if (!container) return;
    
    // Show completed goals as recent activity
    const completedGoals = cachedDailyGoals.filter(g => g.completed);
    
    if (completedGoals.length > 0) {
        container.innerHTML = `
            ${completedGoals.map(goal => `
                <div class="activity-item">
                    <span class="activity-icon">✅</span>
                    <div class="activity-content">
                        <span class="activity-title">${t('activity.completedGoal')}: ${escapeHtml(goal.title)}</span>
                        <span class="activity-time">${t('activity.today')}</span>
                    </div>
                </div>
            `).join('')}
        `;
    } else {
        container.innerHTML = `
            <p class="empty-state">${t('dashboard.activityComingSoon')}</p>
            <small>${t('dashboard.activityHint')}</small>
        `;
    }
}

export default { loadDashboardData, getCachedGoal };
