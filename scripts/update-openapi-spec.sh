#!/bin/bash
# ==============================================================================
# OpenAPI Specification Update Script
# ==============================================================================
# This script generates/updates the OpenAPI specification file using Maven.
# It starts a temporary H2-backed Spring Boot instance, downloads the spec,
# and stops the instance. No external backend required.
#
# Usage:
#   ./scripts/update-openapi-spec.sh
#
# Or directly with Maven:
#   cd backend && mvn verify -Pgenerate-openapi -DskipTests -pl api-module -am
#
# Output:
#   docs/openapi.yaml
# ==============================================================================

# Don't exit on error - we handle errors manually
set +e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory (for relative paths)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_ROOT/docs"

# Handle --help
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    sed -n '2,16p' "$0"
    exit 0
fi

# Ensure docs directory exists
mkdir -p "$DOCS_DIR"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  OpenAPI Specification Update${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

echo -e "${YELLOW}Generating OpenAPI spec using Maven...${NC}"
echo "This will start a temporary backend instance with H2 database."
echo ""

cd "$PROJECT_ROOT/backend"

# Run Maven with the generate-openapi profile
# The springdoc-openapi-maven-plugin uses Java's HttpURLConnection internally
if mvn verify -Pgenerate-openapi -DskipTests -pl api-module -am -q; then
    if [ -f "$DOCS_DIR/openapi.yaml" ]; then
        echo ""
        echo -e "${GREEN}✓ OpenAPI spec generated successfully!${NC}"
        echo -e "  Location: ${BLUE}docs/openapi.yaml${NC}"
    else
        echo ""
        echo -e "${RED}✗ Maven completed but spec file not found${NC}"
        exit 1
    fi
else
    echo ""
    echo -e "${RED}✗ Maven build failed${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${GREEN}Done!${NC}"
echo -e "${BLUE}================================================${NC}"