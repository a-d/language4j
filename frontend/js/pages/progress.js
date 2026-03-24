/**
 * Progress Page Module
 * Handles progress tracking and long-term goals display.
 */

import { api } from '../api/client.js';
import { t, getGoalType } from '../services/i18n.js';

// Store goals for modal access
let cachedAllGoals = [];

/**
 * Load progress page data.
 * @param {Function} showLoading
 * @param {Function} hideLoading
 */
export async function loadProgressData(showLoading, hideLoading) {
    showLoading();
    
    try {
        let allGoals = [];
        try {
            allGoals = await api.goals.list();
            cachedAllGoals = allGoals;
        } catch (error) {
            console.error('Failed to load goals from API:', error);
        }
        
        const completedGoals = allGoals.filter(g => g.completed).length;
        const totalProgress = allGoals.reduce((sum, g) => sum + g.currentValue, 0);
        
        // Update stats
        const totalExercisesEl = document.getElementById('total-exercises');
        const avgScoreEl = document.getElementById('avg-score');
        const timeSpentEl = document.getElementById('time-spent');
        const streakEl = document.getElementById('current-streak');
        
        if (totalExercisesEl) totalExercisesEl.textContent = totalProgress.toString();
        if (avgScoreEl) avgScoreEl.textContent = allGoals.length > 0 
            ? Math.round(allGoals.reduce((sum, g) => sum + g.progressPercent, 0) / allGoals.length) + '%'
            : '0%';
        if (timeSpentEl) timeSpentEl.textContent = '--';
        if (streakEl) streakEl.textContent = completedGoals.toString();
        
        // Render all goals by type
        const container = document.getElementById('goals-list');
        if (!container) return;
        
        // Group goals by type
        const goalsByType = {
            DAILY: allGoals.filter(g => g.type === 'DAILY'),
            WEEKLY: allGoals.filter(g => g.type === 'WEEKLY'),
            MONTHLY: allGoals.filter(g => g.type === 'MONTHLY'),
            YEARLY: allGoals.filter(g => g.type === 'YEARLY')
        };
        
        // Check if any long-term goals exist
        const hasLongTermGoals = goalsByType.WEEKLY.length > 0 || 
                                  goalsByType.MONTHLY.length > 0 || 
                                  goalsByType.YEARLY.length > 0;
        
        if (!hasLongTermGoals && goalsByType.DAILY.length === 0) {
            container.innerHTML = `
                <div class="empty-goals-container">
                    <p class="empty-state">${t('progress.noGoals')}</p>
                    <div class="empty-goals-actions">
                        <button class="btn btn-primary" onclick="window.showCreateGoalModal()">${t('progress.createGoal')}</button>
                    </div>
                </div>
            `;
            return;
        }
        
        // Render goals by type with filter tabs
        container.innerHTML = `
            <div class="goals-filter-tabs">
                <button class="filter-tab active" data-filter="all" onclick="window.filterGoals('all')">${t('goals.allGoals')}</button>
                <button class="filter-tab" data-filter="DAILY" onclick="window.filterGoals('DAILY')">${t('goals.typeDaily')} (${goalsByType.DAILY.length})</button>
                <button class="filter-tab" data-filter="WEEKLY" onclick="window.filterGoals('WEEKLY')">${t('goals.typeWeekly')} (${goalsByType.WEEKLY.length})</button>
                <button class="filter-tab" data-filter="MONTHLY" onclick="window.filterGoals('MONTHLY')">${t('goals.typeMonthly')} (${goalsByType.MONTHLY.length})</button>
                <button class="filter-tab" data-filter="YEARLY" onclick="window.filterGoals('YEARLY')">${t('goals.typeYearly')} (${goalsByType.YEARLY.length})</button>
                <button class="btn btn-sm btn-primary filter-add-btn" onclick="window.showCreateGoalModal()">+ ${t('goals.addGoal')}</button>
            </div>
            <div class="goals-cards-container" id="filtered-goals">
                ${renderGoalsByType(allGoals)}
            </div>
        `;
    } catch (error) {
        console.error('Failed to load progress:', error);
    } finally {
        hideLoading();
    }
}

/**
 * Render goals grouped or filtered.
 * @param {Array} goals
 * @param {string} filter - Optional filter type
 * @returns {string} HTML string
 */
function renderGoalsByType(goals, filter = 'all') {
    const filteredGoals = filter === 'all' 
        ? goals 
        : goals.filter(g => g.type === filter);
    
    if (filteredGoals.length === 0) {
        return `<p class="empty-state">${t('progress.noGoalsInCategory')}</p>`;
    }
    
    return filteredGoals.map(goal => renderGoalCard(goal)).join('');
}

/**
 * Render a single goal card with full actions.
 * @param {Object} goal
 * @returns {string} HTML string
 */
function renderGoalCard(goal) {
    const isCompleted = goal.completed;
    const progressClass = isCompleted ? 'completed' : (goal.progressPercent >= 75 ? 'high' : '');
    
    return `
        <div class="goal-card ${isCompleted ? 'goal-completed' : ''}" data-goal-id="${goal.id}" data-goal-type="${goal.type}">
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
                <button class="btn btn-sm" onclick="window.resetGoalProgress('${goal.id}')" title="${t('goals.reset')}">↺</button>
                <button class="btn btn-sm btn-danger" onclick="window.deleteGoal('${goal.id}')" title="${t('goals.delete')}">🗑</button>
            </div>
        </div>
    `;
}

/**
 * Filter goals by type.
 * @param {string} filter - Goal type or 'all'
 */
export function filterGoals(filter) {
    // Update active tab
    document.querySelectorAll('.filter-tab').forEach(tab => {
        tab.classList.toggle('active', tab.dataset.filter === filter);
    });
    
    // Re-render goals
    const container = document.getElementById('filtered-goals');
    if (container) {
        container.innerHTML = renderGoalsByType(cachedAllGoals, filter);
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
 * Get a cached goal by ID.
 * @param {string} goalId
 * @returns {Object|undefined}
 */
export function getCachedGoal(goalId) {
    return cachedAllGoals.find(g => g.id === goalId);
}

export default { loadProgressData, filterGoals, getCachedGoal };
