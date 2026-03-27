/**
 * Toast Notification Service
 * ==========================
 * Displays toast notifications to the user.
 */

const TOAST_DURATION = 3000;

/**
 * Show a toast notification
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) {
        console.warn('Toast container not found');
        return;
    }
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    
    container.appendChild(toast);
    
    // Auto-remove after duration
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.2s ease forwards';
        setTimeout(() => {
            toast.remove();
        }, 200);
    }, TOAST_DURATION);
}

/**
 * Show a toast notification (exported for direct use)
 */
export { showToast };

/**
 * Toast service with typed methods
 */
export const toast = {
    info: (message) => showToast(message, 'info'),
    success: (message) => showToast(message, 'success'),
    error: (message) => showToast(message, 'error'),
    warning: (message) => showToast(message, 'warning')
};

// Add slideOut animation
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);