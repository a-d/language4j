#!/bin/bash
#
# Demo Data Collection Script
# ===========================
# Collects sample data from a running backend for offline demo mode.
#
# Usage:
#   BACKEND_URL=http://localhost:8080 ./collect-demo-data.sh
#
# Environment Variables:
#   BACKEND_URL  - Backend API URL (required)
#   NATIVE_LANG  - Native language code (default: en)
#   TARGET_LANG  - Target language code (default: de)
#   OUTPUT_DIR   - Output directory (default: frontend/demo-data)
#   DELAY_MS     - Delay between requests in ms (default: 1000)
#   TOPICS       - Comma-separated list of topics to collect (default: all)
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration from environment variables
BACKEND_URL="${BACKEND_URL:-}"
NATIVE_LANG="${NATIVE_LANG:-en}"
TARGET_LANG="${TARGET_LANG:-de}"
OUTPUT_DIR="${OUTPUT_DIR:-frontend/demo-data}"
DELAY_MS="${DELAY_MS:-1000}"

# Calculate delay in seconds for sleep command
DELAY_SEC=$(echo "scale=2; $DELAY_MS / 1000" | bc)

# Default topics
DEFAULT_TOPICS=(
    "greetings"
    "food"
    "travel"
    "family"
    "shopping"
    "weather"
    "work"
    "hobbies"
    "health"
    "home"
    "time"
    "colors"
    "animals"
    "clothing"
    "technology"
)

# Parse TOPICS environment variable or use defaults
if [ -n "$TOPICS" ]; then
    IFS=',' read -ra TOPICS_ARRAY <<< "$TOPICS"
else
    TOPICS_ARRAY=("${DEFAULT_TOPICS[@]}")
fi

# Topics that should have roleplay scenarios
SCENARIO_TOPICS=("greetings" "food" "travel" "shopping" "work" "health" "home")

# Counters
TOTAL_REQUESTS=0
SUCCESSFUL_REQUESTS=0
FAILED_REQUESTS=0

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_progress() {
    echo -e "${BLUE}[${1}/${2}]${NC} $3"
}

# Check prerequisites
check_prerequisites() {
    if [ -z "$BACKEND_URL" ]; then
        log_error "BACKEND_URL environment variable is required"
        echo "Usage: BACKEND_URL=http://localhost:8080 $0"
        exit 1
    fi

    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        log_warn "jq is not installed - JSON formatting will be skipped"
        JQ_AVAILABLE=false
    else
        JQ_AVAILABLE=true
    fi
}

# Test backend connection
test_connection() {
    log_info "Testing connection to $BACKEND_URL..."
    
    if curl -s --connect-timeout 5 "$BACKEND_URL/api/v1/users/me" > /dev/null 2>&1; then
        log_success "Backend is reachable"
        return 0
    else
        log_error "Cannot connect to backend at $BACKEND_URL"
        exit 1
    fi
}

# Create output directory structure
create_directories() {
    log_info "Creating output directories in $OUTPUT_DIR..."
    
    mkdir -p "$OUTPUT_DIR"/{i18n,content/lessons,content/vocabulary,content/flashcards,content/scenarios,content/visual-cards,exercises/text-completion,exercises/drag-drop,exercises/translation,chat}
    
    log_success "Directories created"
}

# Make API request and save response
# Args: $1=method, $2=endpoint, $3=output_file, $4=body (optional)
api_request() {
    local method="$1"
    local endpoint="$2"
    local output_file="$3"
    local body="$4"
    local temp_file=$(mktemp)
    
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    
    local curl_args=(-s -w "%{http_code}" -o "$temp_file")
    curl_args+=(-H "Content-Type: application/json")
    # Note: X-User-Id header not sent - backend uses default user context
    
    if [ "$method" = "POST" ]; then
        curl_args+=(-X POST -d "$body")
    fi
    
    local http_code
    http_code=$(curl "${curl_args[@]}" "$BACKEND_URL$endpoint")
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        # Format JSON if jq is available
        if [ "$JQ_AVAILABLE" = true ]; then
            jq '.' "$temp_file" > "$output_file" 2>/dev/null || mv "$temp_file" "$output_file"
        else
            mv "$temp_file" "$output_file"
        fi
        SUCCESSFUL_REQUESTS=$((SUCCESSFUL_REQUESTS + 1))
        rm -f "$temp_file"
        return 0
    else
        log_warn "Request failed with HTTP $http_code: $endpoint"
        FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
        rm -f "$temp_file"
        return 1
    fi
}

# Collect i18n translations
collect_translations() {
    log_info "Collecting translations..."
    
    api_request "GET" "/api/v1/i18n/languages/en" "$OUTPUT_DIR/i18n/en.json" && \
        log_success "English translations collected"
    
    sleep "$DELAY_SEC"
    
    api_request "GET" "/api/v1/i18n/languages/de" "$OUTPUT_DIR/i18n/de.json" && \
        log_success "German translations collected"
    
    sleep "$DELAY_SEC"
}

# Create or get demo user
setup_demo_user() {
    log_info "Setting up demo user..."
    
    # Create demo user JSON
    cat > "$OUTPUT_DIR/user.json" << EOF
{
    "id": "demo-user",
    "displayName": "Demo User",
    "nativeLanguage": "$NATIVE_LANG",
    "targetLanguage": "$TARGET_LANG",
    "skillLevel": "A1",
    "assessmentCompleted": true,
    "createdAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF
    
    log_success "Demo user created"
}

# Create sample goals
create_sample_goals() {
    log_info "Creating sample goals..."
    
    cat > "$OUTPUT_DIR/goals.json" << EOF
[
    {
        "id": "goal-daily-words",
        "type": "DAILY",
        "title": "Learn new words",
        "description": "Learn 10 new vocabulary words today",
        "targetValue": 10,
        "currentValue": 3,
        "unit": "words",
        "startDate": "$(date +%Y-%m-%d)",
        "endDate": "$(date +%Y-%m-%d)",
        "completed": false,
        "progressPercent": 30
    },
    {
        "id": "goal-daily-exercises",
        "type": "DAILY",
        "title": "Complete exercises",
        "description": "Complete 5 exercises today",
        "targetValue": 5,
        "currentValue": 2,
        "unit": "exercises",
        "startDate": "$(date +%Y-%m-%d)",
        "endDate": "$(date +%Y-%m-%d)",
        "completed": false,
        "progressPercent": 40
    },
    {
        "id": "goal-weekly-lessons",
        "type": "WEEKLY",
        "title": "Study lessons",
        "description": "Complete 3 lessons this week",
        "targetValue": 3,
        "currentValue": 1,
        "unit": "lessons",
        "startDate": "$(date -d 'last monday' +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)",
        "endDate": "$(date -d 'next sunday' +%Y-%m-%d 2>/dev/null || date +%Y-%m-%d)",
        "completed": false,
        "progressPercent": 33
    }
]
EOF
    
    log_success "Sample goals created"
}

# Collect content for a topic
collect_topic_content() {
    local topic="$1"
    local topic_num="$2"
    local total_topics="$3"
    
    log_progress "$topic_num" "$total_topics" "Collecting content for: $topic"
    
    # Lesson
    echo -n "  Lesson... "
    if api_request "POST" "/api/v1/content/lessons/generate" \
        "$OUTPUT_DIR/content/lessons/$topic.json" \
        "{\"topic\": \"$topic\"}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Vocabulary
    echo -n "  Vocabulary... "
    if api_request "POST" "/api/v1/content/vocabulary/generate" \
        "$OUTPUT_DIR/content/vocabulary/$topic.json" \
        "{\"topic\": \"$topic\", \"wordCount\": 15}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Flashcards
    echo -n "  Flashcards... "
    if api_request "POST" "/api/v1/content/flashcards/generate" \
        "$OUTPUT_DIR/content/flashcards/$topic.json" \
        "{\"topic\": \"$topic\", \"cardCount\": 10}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Text Completion exercises
    echo -n "  Text Completion... "
    if api_request "POST" "/api/v1/exercises/generate" \
        "$OUTPUT_DIR/exercises/text-completion/$topic.json" \
        "{\"type\": \"TEXT_COMPLETION\", \"topic\": \"$topic\", \"count\": 5}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Drag-Drop exercises
    echo -n "  Drag-Drop... "
    if api_request "POST" "/api/v1/exercises/generate" \
        "$OUTPUT_DIR/exercises/drag-drop/$topic.json" \
        "{\"type\": \"DRAG_DROP\", \"topic\": \"$topic\", \"count\": 5}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Translation exercises
    echo -n "  Translation... "
    if api_request "POST" "/api/v1/exercises/generate" \
        "$OUTPUT_DIR/exercises/translation/$topic.json" \
        "{\"type\": \"TRANSLATION\", \"topic\": \"$topic\", \"count\": 5}"; then
        echo "✓"
    else
        echo "✗"
    fi
    sleep "$DELAY_SEC"
    
    # Scenario (only for certain topics)
    if [[ " ${SCENARIO_TOPICS[*]} " =~ " ${topic} " ]]; then
        echo -n "  Scenario... "
        if api_request "POST" "/api/v1/content/scenarios/generate" \
            "$OUTPUT_DIR/content/scenarios/$topic.json" \
            "{\"scenario\": \"$topic\"}"; then
            echo "✓"
        else
            echo "✗"
        fi
        sleep "$DELAY_SEC"
    fi
    
    # Visual Cards (with embedded base64 images)
    if [ "${COLLECT_VISUAL_CARDS:-false}" = "true" ]; then
        echo -n "  Visual Cards... "
        if collect_visual_cards "$topic"; then
            echo "✓"
        else
            echo "✗"
        fi
        sleep "$DELAY_SEC"
    fi
}

# Download image and convert to base64 data URL
# Args: $1=image_url
# Returns: base64 data URL or empty string on failure
download_image_as_base64() {
    local url="$1"
    local temp_file=$(mktemp)
    
    # Download image
    if curl -s -L -o "$temp_file" "$url" 2>/dev/null; then
        # Detect content type
        local content_type="image/png"
        if file "$temp_file" | grep -q "JPEG"; then
            content_type="image/jpeg"
        elif file "$temp_file" | grep -q "WebP"; then
            content_type="image/webp"
        fi
        
        # Convert to base64
        local base64_data
        if command -v base64 &> /dev/null; then
            base64_data=$(base64 -w 0 "$temp_file" 2>/dev/null || base64 "$temp_file" 2>/dev/null)
        fi
        
        rm -f "$temp_file"
        
        if [ -n "$base64_data" ]; then
            echo "data:$content_type;base64,$base64_data"
            return 0
        fi
    fi
    
    rm -f "$temp_file"
    return 1
}

# Collect visual cards for a topic with embedded base64 images
# Args: $1=topic
collect_visual_cards() {
    local topic="$1"
    local output_file="$OUTPUT_DIR/content/visual-cards/$topic.json"
    local vocab_file="$OUTPUT_DIR/content/vocabulary/$topic.json"
    local temp_file=$(mktemp)
    local temp_output=$(mktemp)
    
    # Check if vocabulary file exists (needed to get words for images)
    if [ ! -f "$vocab_file" ]; then
        log_warn "Visual cards: No vocabulary file for $topic, skipping"
        return 1
    fi
    
    if [ "$JQ_AVAILABLE" != true ]; then
        log_warn "Visual cards: jq is required for visual cards collection"
        return 1
    fi
    
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    
    # Extract first 3 words from vocabulary to generate images for
    # The vocabulary file has structure: { content: "<escaped JSON string>", type: "vocabulary" }
    # We need to parse the nested JSON string in the content field
    local words_json
    
    # Try to extract words - the content field contains a JSON string that needs parsing
    # First try: content is a string containing JSON with "vocabulary" array
    words_json=$(jq -r '.content | fromjson | [.vocabulary[:3][] | {word: .word, context: (.example // "")}]' "$vocab_file" 2>/dev/null)
    
    # If that fails, try: direct .words array (older format)
    if [ -z "$words_json" ] || [ "$words_json" = "null" ] || [ "$words_json" = "[]" ]; then
        words_json=$(jq -r '[.words[:3][] | {word: .word, context: (.example // "")}]' "$vocab_file" 2>/dev/null)
    fi
    
    # If still empty, try: content is direct JSON object with vocabulary
    if [ -z "$words_json" ] || [ "$words_json" = "null" ] || [ "$words_json" = "[]" ]; then
        words_json=$(jq -r '[.content.vocabulary[:3][] | {word: .word, context: (.example // "")}]' "$vocab_file" 2>/dev/null)
    fi
    
    if [ -z "$words_json" ] || [ "$words_json" = "null" ] || [ "$words_json" = "[]" ]; then
        log_warn "Visual cards: Could not extract words from vocabulary for $topic (tried all formats)"
        FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
        return 1
    fi
    
    echo "    Found words: $(echo "$words_json" | jq -r '[.[].word] | join(", ")' 2>/dev/null)"
    
    # Generate visual cards via API (batch endpoint expects array of {word, context})
    local curl_args=(-s -w "%{http_code}" -o "$temp_file")
    curl_args+=(-H "Content-Type: application/json")
    # Note: X-User-Id header not sent - backend uses default user context
    curl_args+=(-X POST -d "$words_json")
    
    local http_code
    http_code=$(curl "${curl_args[@]}" "$BACKEND_URL/api/v1/images/flashcard/batch")
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        # Parse response and download images
        # Process each card and embed images as base64
        local cards_count
        cards_count=$(jq 'length' "$temp_file" 2>/dev/null || echo "0")
        
        if [ "$cards_count" -eq 0 ]; then
            log_warn "Visual cards: Empty response for $topic"
            FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
            rm -f "$temp_file" "$temp_output"
            return 1
        fi
        
        echo "[" > "$temp_output"
        local first=true
        
        # Get words from original request to include in output
        local words_array
        words_array=$(echo "$words_json" | jq -r '.[].word' 2>/dev/null)
        
        for i in $(seq 0 $((cards_count - 1))); do
            local card
            card=$(jq ".[$i]" "$temp_file")
            
            # Get the word for this index from the original request
            local word
            word=$(echo "$words_json" | jq -r ".[$i].word" 2>/dev/null)
            
            # Extract image URL
            local image_url
            image_url=$(echo "$card" | jq -r '.url // .imageUrl // empty')
            
            local image_data=""
            if [ -n "$image_url" ] && [ "$image_url" != "null" ]; then
                echo -n "    Downloading image $((i+1))/$cards_count ($word)... "
                image_data=$(download_image_as_base64 "$image_url")
                if [ -n "$image_data" ]; then
                    echo "✓"
                else
                    echo "✗"
                fi
                sleep 0.5
            fi
            
            # Build card JSON with word and embedded image
            if [ "$first" = true ]; then
                first=false
            else
                echo "," >> "$temp_output"
            fi
            
            # Add word and imageData fields
            if [ -n "$image_data" ]; then
                echo "$card" | jq --arg word "$word" --arg imgData "$image_data" \
                    '. + {word: $word, imageData: $imgData}' >> "$temp_output"
            else
                echo "$card" | jq --arg word "$word" '. + {word: $word}' >> "$temp_output"
            fi
        done
        
        echo "]" >> "$temp_output"
        
        # Format final output
        jq '.' "$temp_output" > "$output_file" 2>/dev/null || mv "$temp_output" "$output_file"
        
        SUCCESSFUL_REQUESTS=$((SUCCESSFUL_REQUESTS + 1))
        rm -f "$temp_file" "$temp_output"
        return 0
    else
        log_warn "Visual cards request failed with HTTP $http_code"
        FAILED_REQUESTS=$((FAILED_REQUESTS + 1))
        rm -f "$temp_file" "$temp_output"
        return 1
    fi
}

# Collect chat data
collect_chat_data() {
    log_info "Collecting chat data..."
    
    # Topic suggestions for each category
    local categories=("VOCABULARY" "EXERCISE" "LESSON" "SCENARIO")
    
    for category in "${categories[@]}"; do
        echo -n "  Suggestions for $category... "
        local category_lower=$(echo "$category" | tr '[:upper:]' '[:lower:]')
        if api_request "POST" "/api/v1/chat/topics/suggestions" \
            "$OUTPUT_DIR/chat/suggestions-$category_lower.json" \
            "{\"category\": \"$category\", \"count\": 5, \"includeRandom\": true}"; then
            echo "✓"
        else
            echo "✗"
        fi
        sleep "$DELAY_SEC"
    done
    
    # Create a sample greeting message
    cat > "$OUTPUT_DIR/chat/greeting.json" << EOF
{
    "id": "greeting-1",
    "role": "ASSISTANT",
    "content": "Hello! 👋 I'm your language learning coach. I'm here to help you learn ${TARGET_LANG} in a fun and interactive way.\n\nWhat would you like to do today? You can:\n• Practice vocabulary\n• Do some exercises\n• Start a lesson\n• Try a roleplay scenario\n\nJust pick an activity below, or type your own question!",
    "createdAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF
    
    log_success "Chat data collected"
}

# Create index manifest
create_index() {
    log_info "Creating index manifest..."
    
    # Build topics array for JSON
    local topics_json="["
    local first=true
    for topic in "${TOPICS_ARRAY[@]}"; do
        if [ "$first" = true ]; then
            first=false
        else
            topics_json+=","
        fi
        topics_json+="\"$topic\""
    done
    topics_json+="]"
    
    # Determine if visual cards were collected
    local visual_cards_enabled="false"
    if [ "${COLLECT_VISUAL_CARDS:-false}" = "true" ]; then
        visual_cards_enabled="true"
    fi
    
    cat > "$OUTPUT_DIR/index.json" << EOF
{
    "version": "1.0.0",
    "generatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
    "nativeLanguage": "$NATIVE_LANG",
    "targetLanguage": "$TARGET_LANG",
    "topics": $topics_json,
    "categories": {
        "lessons": $topics_json,
        "vocabulary": $topics_json,
        "flashcards": $topics_json,
        "visualCards": $topics_json,
        "scenarios": ["greetings", "food", "travel", "shopping", "work", "health", "home"],
        "exercises": {
            "text-completion": $topics_json,
            "drag-drop": $topics_json,
            "translation": $topics_json
        }
    },
    "chatSuggestions": ["vocabulary", "exercise", "lesson", "scenario"],
    "features": {
        "lessons": true,
        "vocabulary": true,
        "flashcards": true,
        "exercises": {
            "textCompletion": true,
            "dragDrop": true,
            "translation": true,
            "listening": false,
            "speaking": false
        },
        "chat": true,
        "visualCards": $visual_cards_enabled,
        "audio": false,
        "progressTracking": false
    }
}
EOF
    
    log_success "Index manifest created"
}

# Create config file
create_config() {
    log_info "Creating config file..."
    
    cat > "$OUTPUT_DIR/config.json" << EOF
{
    "demoMode": true,
    "nativeLanguage": "$NATIVE_LANG",
    "targetLanguage": "$TARGET_LANG",
    "skillLevel": "A1",
    "disabledFeatures": [
        "listening",
        "speaking",
        "visualCards",
        "progressTracking",
        "userManagement"
    ]
}
EOF
    
    log_success "Config file created"
}

# Print summary
print_summary() {
    echo ""
    echo "========================================"
    echo "          Collection Summary"
    echo "========================================"
    echo -e "Total requests:      ${BLUE}$TOTAL_REQUESTS${NC}"
    echo -e "Successful:          ${GREEN}$SUCCESSFUL_REQUESTS${NC}"
    echo -e "Failed:              ${RED}$FAILED_REQUESTS${NC}"
    echo ""
    echo "Output directory:    $OUTPUT_DIR"
    echo "Topics collected:    ${#TOPICS_ARRAY[@]}"
    echo ""
    
    if [ $FAILED_REQUESTS -gt 0 ]; then
        log_warn "Some requests failed. Check the output for details."
    else
        log_success "All data collected successfully!"
    fi
    
    # Calculate approximate size
    if command -v du &> /dev/null; then
        local size=$(du -sh "$OUTPUT_DIR" 2>/dev/null | cut -f1)
        echo "Total size:          $size"
    fi
}

# Main execution
main() {
    echo ""
    echo "========================================"
    echo "     Demo Data Collection Script"
    echo "========================================"
    echo ""
    
    check_prerequisites
    test_connection
    create_directories
    setup_demo_user
    create_sample_goals
    collect_translations
    
    # Collect content for each topic
    local topic_count=0
    local total_topics=${#TOPICS_ARRAY[@]}
    
    for topic in "${TOPICS_ARRAY[@]}"; do
        topic_count=$((topic_count + 1))
        collect_topic_content "$topic" "$topic_count" "$total_topics"
    done
    
    collect_chat_data
    create_index
    create_config
    
    print_summary
}

# Run main function
main "$@"