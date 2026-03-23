#!/bin/bash
# =============================================================================
# Language Learning Platform - Local AI Services Test Script
# =============================================================================
# Tests all local AI services and provides a comprehensive status report.
#
# Usage:
#   ./test-services.sh           # Run all tests
#   ./test-services.sh --quick   # Health checks only
#   ./test-services.sh --full    # Full tests with audio/image generation
#
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Service endpoints
OLLAMA_URL="http://localhost:11434"
WHISPER_URL="http://localhost:9000"
PIPER_URL="http://localhost:9001"
SD_URL="http://localhost:7860"

# Test mode
TEST_MODE="${1:-standard}"

# Output directory for generated files
OUTPUT_DIR="./test-output"

# =============================================================================
# Dependency Check
# =============================================================================

check_dependencies() {
    local missing=()
    
    if ! command -v curl &> /dev/null; then
        missing+=("curl")
    fi
    
    if ! command -v jq &> /dev/null; then
        missing+=("jq")
    fi
    
    if ! command -v bc &> /dev/null; then
        missing+=("bc")
    fi
    
    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}Error: Missing required dependencies: ${missing[*]}${NC}"
        echo ""
        echo "Install them with:"
        echo "  sudo apt-get update && sudo apt-get install -y ${missing[*]}"
        echo ""
        exit 1
    fi
}

# Check dependencies before running
check_dependencies

# =============================================================================
# Helper Functions
# =============================================================================

print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

print_subheader() {
    echo ""
    echo -e "${CYAN}--- $1 ---${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}!${NC} $1"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

# Check if a service is reachable
check_service() {
    local url=$1
    local name=$2
    local timeout=${3:-5}
    
    if curl -s --connect-timeout "$timeout" "$url" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Pretty print JSON (uses jq if available, otherwise cat)
pretty_json() {
    if command -v jq &> /dev/null; then
        jq -C '.' 2>/dev/null || cat
    else
        cat
    fi
}

# =============================================================================
# Service Health Checks
# =============================================================================

check_ollama_health() {
    print_subheader "Ollama (LLM) - $OLLAMA_URL"
    
    if ! check_service "$OLLAMA_URL/api/tags" "Ollama"; then
        print_error "Ollama is not reachable"
        return 1
    fi
    
    print_success "Ollama is running"
    
    # Get available models
    local models=$(curl -s "$OLLAMA_URL/api/tags" 2>/dev/null)
    if [ -n "$models" ]; then
        local model_count=$(echo "$models" | jq -r '.models | length' 2>/dev/null || echo "0")
        if [ "$model_count" -gt 0 ]; then
            print_success "Found $model_count model(s):"
            echo "$models" | jq -r '.models[].name' 2>/dev/null | while read -r model; do
                echo "       - $model"
            done
        else
            print_warning "No models installed. Run:"
            echo "       docker exec -it language-learning-ollama ollama pull qwen2.5:32b-instruct-q5_K_M"
        fi
    fi
    
    return 0
}

check_whisper_health() {
    print_subheader "Whisper (Speech-to-Text) - $WHISPER_URL"
    
    if ! check_service "$WHISPER_URL/health" "Whisper" 10; then
        print_error "Whisper is not reachable"
        return 1
    fi
    
    local health=$(curl -s "$WHISPER_URL/health" 2>/dev/null)
    local status=$(echo "$health" | jq -r '.status' 2>/dev/null || echo "unknown")
    local model=$(echo "$health" | jq -r '.model' 2>/dev/null || echo "unknown")
    local cuda=$(echo "$health" | jq -r '.cuda_available' 2>/dev/null || echo "unknown")
    local device=$(echo "$health" | jq -r '.cuda_device // "N/A"' 2>/dev/null || echo "N/A")
    
    if [ "$status" = "healthy" ]; then
        print_success "Whisper is healthy"
    else
        print_warning "Whisper status: $status"
    fi
    
    print_info "Model: $model"
    print_info "CUDA available: $cuda"
    if [ "$cuda" = "true" ]; then
        print_info "GPU: $device"
    fi
    
    return 0
}

check_piper_health() {
    print_subheader "Piper (Text-to-Speech) - $PIPER_URL"
    
    if ! check_service "$PIPER_URL/health" "Piper"; then
        print_error "Piper is not reachable"
        return 1
    fi
    
    local health=$(curl -s "$PIPER_URL/health" 2>/dev/null)
    local status=$(echo "$health" | jq -r '.status' 2>/dev/null || echo "unknown")
    local voice=$(echo "$health" | jq -r '.voice' 2>/dev/null || echo "unknown")
    local sample_rate=$(echo "$health" | jq -r '.sample_rate // "N/A"' 2>/dev/null || echo "N/A")
    
    if [ "$status" = "healthy" ]; then
        print_success "Piper is healthy"
    else
        print_warning "Piper status: $status"
    fi
    
    print_info "Voice: $(basename "$voice")"
    print_info "Sample rate: ${sample_rate}Hz"
    
    return 0
}

check_sd_health() {
    print_subheader "Stable Diffusion (Image Generation) - $SD_URL"
    
    if ! check_service "$SD_URL/health" "Stable Diffusion" 10; then
        print_error "Stable Diffusion is not reachable"
        return 1
    fi
    
    local health=$(curl -s "$SD_URL/health" 2>/dev/null)
    local status=$(echo "$health" | jq -r '.status' 2>/dev/null || echo "unknown")
    local model=$(echo "$health" | jq -r '.model' 2>/dev/null || echo "unknown")
    local device=$(echo "$health" | jq -r '.device' 2>/dev/null || echo "unknown")
    local float16=$(echo "$health" | jq -r '.float16' 2>/dev/null || echo "unknown")
    
    if [ "$status" = "healthy" ]; then
        print_success "Stable Diffusion is healthy"
    else
        print_warning "Stable Diffusion status: $status"
    fi
    
    print_info "Model: $model"
    print_info "Device: $device"
    print_info "Float16: $float16"
    
    return 0
}

# =============================================================================
# Functional Tests
# =============================================================================

test_ollama_generation() {
    print_subheader "Testing Ollama Text Generation"
    
    # Get first available model
    local model=$(curl -s "$OLLAMA_URL/api/tags" 2>/dev/null | jq -r '.models[0].name' 2>/dev/null)
    
    if [ -z "$model" ] || [ "$model" = "null" ]; then
        print_warning "No models available to test"
        return 1
    fi
    
    print_info "Testing with model: $model"
    
    local start_time=$(date +%s.%N)
    local response=$(curl -s --max-time 60 -X POST "$OLLAMA_URL/api/generate" \
        -H "Content-Type: application/json" \
        -d "{\"model\": \"$model\", \"prompt\": \"Say hello in French in one sentence.\", \"stream\": false}" 2>/dev/null)
    local end_time=$(date +%s.%N)
    
    if [ -n "$response" ]; then
        local generated=$(echo "$response" | jq -r '.response' 2>/dev/null || echo "")
        if [ -n "$generated" ] && [ "$generated" != "null" ]; then
            local duration=$(echo "$end_time - $start_time" | bc 2>/dev/null || echo "?")
            print_success "Generation successful (${duration}s)"
            echo "       Response: ${generated:0:100}..."
            return 0
        fi
    fi
    
    print_error "Generation failed"
    return 1
}

test_piper_tts() {
    print_subheader "Testing Piper Text-to-Speech"
    
    mkdir -p "$OUTPUT_DIR"
    local output_file="$OUTPUT_DIR/piper_test.wav"
    
    local start_time=$(date +%s.%N)
    local http_code=$(curl -s -w "%{http_code}" --max-time 30 -X POST "$PIPER_URL/v1/audio/speech" \
        -H "Content-Type: application/json" \
        -d '{"input": "Bonjour, comment allez-vous?", "voice": "alloy", "speed": 1.0}' \
        -o "$output_file" 2>/dev/null)
    local end_time=$(date +%s.%N)
    
    if [ "$http_code" = "200" ] && [ -f "$output_file" ] && [ -s "$output_file" ]; then
        local file_size=$(stat -f%z "$output_file" 2>/dev/null || stat -c%s "$output_file" 2>/dev/null || echo "0")
        local duration=$(echo "$end_time - $start_time" | bc 2>/dev/null || echo "?")
        print_success "TTS generation successful (${duration}s)"
        print_info "Output file: $output_file (${file_size} bytes)"
        return 0
    else
        print_error "TTS generation failed (HTTP $http_code)"
        return 1
    fi
}

test_whisper_stt() {
    print_subheader "Testing Whisper Speech-to-Text"
    
    local test_audio="$OUTPUT_DIR/piper_test.wav"
    
    if [ ! -f "$test_audio" ]; then
        print_warning "No test audio file available. Run TTS test first."
        return 1
    fi
    
    print_info "Transcribing: $test_audio"
    
    local start_time=$(date +%s.%N)
    local response=$(curl -s --max-time 120 -X POST "$WHISPER_URL/v1/audio/transcriptions" \
        -F "file=@$test_audio" \
        -F "language=fr" 2>/dev/null)
    local end_time=$(date +%s.%N)
    
    if [ -n "$response" ]; then
        local text=$(echo "$response" | jq -r '.text' 2>/dev/null || echo "")
        if [ -n "$text" ] && [ "$text" != "null" ]; then
            local duration=$(echo "$end_time - $start_time" | bc 2>/dev/null || echo "?")
            print_success "Transcription successful (${duration}s)"
            echo "       Transcribed: $text"
            return 0
        fi
    fi
    
    print_error "Transcription failed"
    echo "       Response: $response"
    return 1
}

test_sd_generation() {
    print_subheader "Testing Stable Diffusion Image Generation"
    
    mkdir -p "$OUTPUT_DIR"
    local output_file="$OUTPUT_DIR/sd_test.png"
    
    print_info "Generating image (this may take a while on first run)..."
    
    local start_time=$(date +%s.%N)
    local response=$(curl -s --max-time 300 -X POST "$SD_URL/v1/images/generations" \
        -H "Content-Type: application/json" \
        -d '{"prompt": "A simple red apple on a white background, minimalist", "size": "512x512", "response_format": "b64_json"}' 2>/dev/null)
    local end_time=$(date +%s.%N)
    
    if [ -n "$response" ]; then
        local b64_data=$(echo "$response" | jq -r '.data[0].b64_json' 2>/dev/null || echo "")
        if [ -n "$b64_data" ] && [ "$b64_data" != "null" ]; then
            echo "$b64_data" | base64 -d > "$output_file" 2>/dev/null
            if [ -f "$output_file" ] && [ -s "$output_file" ]; then
                local file_size=$(stat -f%z "$output_file" 2>/dev/null || stat -c%s "$output_file" 2>/dev/null || echo "0")
                local duration=$(echo "$end_time - $start_time" | bc 2>/dev/null || echo "?")
                print_success "Image generation successful (${duration}s)"
                print_info "Output file: $output_file (${file_size} bytes)"
                return 0
            fi
        fi
    fi
    
    print_error "Image generation failed"
    return 1
}

# =============================================================================
# Docker Status
# =============================================================================

check_docker_status() {
    print_subheader "Docker Container Status"
    
    if ! command -v docker &> /dev/null; then
        print_warning "Docker command not found"
        return 1
    fi
    
    local containers=$(docker ps --filter "name=language-learning" --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null)
    
    if [ -z "$containers" ]; then
        print_warning "No language-learning containers found running"
        echo "       Start with: cd local-deployment/docker && docker-compose up -d"
        return 1
    fi
    
    echo ""
    echo "  Container                    Status              Ports"
    echo "  ─────────────────────────────────────────────────────────────"
    echo "$containers" | while IFS=$'\t' read -r name status ports; do
        printf "  %-28s %-19s %s\n" "$name" "$status" "$ports"
    done
    echo ""
    
    return 0
}

# =============================================================================
# Summary Report
# =============================================================================

generate_summary() {
    local ollama_ok=$1
    local whisper_ok=$2
    local piper_ok=$3
    local sd_ok=$4
    
    print_header "Summary Report"
    
    echo ""
    echo "  Service              Status        Endpoint"
    echo "  ─────────────────────────────────────────────────────────────"
    
    if [ "$ollama_ok" = "0" ]; then
        printf "  %-20s ${GREEN}%-13s${NC} %s\n" "Ollama (LLM)" "✓ Ready" "$OLLAMA_URL"
    else
        printf "  %-20s ${RED}%-13s${NC} %s\n" "Ollama (LLM)" "✗ Error" "$OLLAMA_URL"
    fi
    
    if [ "$whisper_ok" = "0" ]; then
        printf "  %-20s ${GREEN}%-13s${NC} %s\n" "Whisper (STT)" "✓ Ready" "$WHISPER_URL"
    else
        printf "  %-20s ${RED}%-13s${NC} %s\n" "Whisper (STT)" "✗ Error" "$WHISPER_URL"
    fi
    
    if [ "$piper_ok" = "0" ]; then
        printf "  %-20s ${GREEN}%-13s${NC} %s\n" "Piper (TTS)" "✓ Ready" "$PIPER_URL"
    else
        printf "  %-20s ${RED}%-13s${NC} %s\n" "Piper (TTS)" "✗ Error" "$PIPER_URL"
    fi
    
    if [ "$sd_ok" = "0" ]; then
        printf "  %-20s ${GREEN}%-13s${NC} %s\n" "Stable Diffusion" "✓ Ready" "$SD_URL"
    else
        printf "  %-20s ${RED}%-13s${NC} %s\n" "Stable Diffusion" "✗ Error" "$SD_URL"
    fi
    
    echo ""
    
    if [ -d "$OUTPUT_DIR" ]; then
        local file_count=$(ls -1 "$OUTPUT_DIR" 2>/dev/null | wc -l)
        if [ "$file_count" -gt 0 ]; then
            print_info "Generated test files in: $OUTPUT_DIR"
            ls -la "$OUTPUT_DIR" 2>/dev/null | tail -n +2
        fi
    fi
    
    echo ""
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    print_header "Language Learning Platform - AI Services Test"
    echo ""
    echo "  Test mode: $TEST_MODE"
    echo "  Time: $(date)"
    
    # Track results
    local ollama_ok=1
    local whisper_ok=1
    local piper_ok=1
    local sd_ok=1
    
    # Check Docker status
    check_docker_status
    
    # Health checks
    print_header "Health Checks"
    
    check_ollama_health && ollama_ok=0
    check_whisper_health && whisper_ok=0
    check_piper_health && piper_ok=0
    check_sd_health && sd_ok=0
    
    # Functional tests (skip in quick mode)
    if [ "$TEST_MODE" != "--quick" ]; then
        print_header "Functional Tests"
        
        # Test Ollama if healthy and has models
        if [ "$ollama_ok" = "0" ]; then
            test_ollama_generation || true
        fi
        
        # Test Piper TTS
        if [ "$piper_ok" = "0" ]; then
            test_piper_tts || true
        fi
        
        # Test Whisper STT (requires audio from Piper test)
        if [ "$whisper_ok" = "0" ]; then
            test_whisper_stt || true
        fi
        
        # Test Stable Diffusion (only in full mode - slow)
        if [ "$TEST_MODE" = "--full" ] && [ "$sd_ok" = "0" ]; then
            test_sd_generation || true
        elif [ "$sd_ok" = "0" ]; then
            print_subheader "Stable Diffusion Image Generation"
            print_info "Skipped (use --full flag to test image generation)"
        fi
    fi
    
    # Generate summary
    generate_summary $ollama_ok $whisper_ok $piper_ok $sd_ok
    
    # Exit with appropriate code
    if [ "$ollama_ok" = "0" ] && [ "$whisper_ok" = "0" ] && [ "$piper_ok" = "0" ] && [ "$sd_ok" = "0" ]; then
        print_success "All services are operational!"
        exit 0
    else
        print_warning "Some services are not available"
        exit 1
    fi
}

# Run main function
main