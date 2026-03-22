#!/bin/sh

# Create runtime config.js with environment variables
cat > /usr/share/nginx/html/js/config.js << EOF
// Runtime configuration - generated at container startup
window.APP_CONFIG = {
    API_URL: '${API_URL:-/api}',
    APP_NAME: 'Language Learning Platform'
};
EOF

# Execute the main command (nginx)
exec "$@"