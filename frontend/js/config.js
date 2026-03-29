// Runtime configuration - will be overwritten in Docker
window.APP_CONFIG = {
    API_URL: 'http://localhost:9090/api',
    APP_NAME: 'Language Learning Platform',
    // Set to true for static deployments (GitHub Pages) to skip backend checks
    // When true, demo mode is enabled immediately on startup
    FORCE_DEMO_MODE: false
};
