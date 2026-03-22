/**
 * Simple Router Service
 * =====================
 * Handles hash-based routing for the SPA.
 */

/**
 * Router service
 */
export const router = {
    /**
     * Navigate to a route
     */
    navigate(route) {
        window.location.hash = route;
    },
    
    /**
     * Get current route
     */
    getCurrentRoute() {
        return window.location.hash.slice(1) || 'dashboard';
    },
    
    /**
     * Add route change listener
     */
    onRouteChange(callback) {
        window.addEventListener('hashchange', () => {
            callback(this.getCurrentRoute());
        });
    }
};