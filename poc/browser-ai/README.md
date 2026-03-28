# Browser-Based AI for Language Learning Platform

## Proof of Concept: Frontend-Only AI Operations

This directory contains the proof-of-concept implementation and documentation for running AI capabilities directly in the browser using WebAssembly and WebGPU, eliminating the need for a backend server for AI operations.

---

## Table of Contents

1. [Vision & Goals](#vision--goals)
2. [Architecture Overview](#architecture-overview)
3. [Operation Modes](#operation-modes)
4. [Technology Stack](#technology-stack)
5. [LLM Implementation](#llm-implementation-priority-1)
6. [Speech Services](#speech-services)
7. [Image Generation](#image-generation-deferred)
8. [Challenges & Mitigations](#challenges--mitigations)
9. [Opportunities](#opportunities)
10. [Open Questions](#open-questions)
11. [Implementation Roadmap](#implementation-roadmap)
12. [POC Structure](#poc-structure)

---

## Vision & Goals

### Vision
Enable the Language Learning Platform to run entirely in the browser without requiring a backend server for AI operations. Users can download models once and use the application offline, with optional cloud fallback for enhanced features.

### Goals

| Goal | Description |
|------|-------------|
| **Backend-Optional** | Full functionality without backend for core learning features |
| **Offline-First** | Work without internet after initial model download |
| **Privacy-Preserving** | All AI processing happens locally on user's device |
| **Cost-Efficient** | No API costs for users or platform operator |
| **Progressive Enhancement** | Degrade gracefully based on device capabilities |
| **Cross-Platform** | Support all devices (desktop, tablet, mobile) |

### Non-Goals (Current Scope)
- Browser-based image generation (deferred due to complexity)
- Real-time collaborative features
- Model training or fine-tuning in browser

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              User Interface                                  │
│                    (Existing Frontend - Vanilla JS)                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AI Service Abstraction Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ LlmService  │  │ TtsService  │  │ SttService  │  │ ImageService        │ │
│  │             │  │             │  │             │  │ (Cloud/Library Only)│ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼────────────────────┼────────────┘
          │                │                │                    │
          ▼                ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Provider Implementations                          │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        LOCAL (Browser)                                │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                   │   │
│  │  │   WebLLM    │  │ Web Speech  │  │Whisper WASM │                   │   │
│  │  │  (WebGPU)   │  │    API      │  │  (ONNX)     │                   │   │
│  │  ├─────────────┤  ├─────────────┤  ├─────────────┤                   │   │
│  │  │Transformers │  │ Piper WASM  │  │ Web Speech  │                   │   │
│  │  │   .js       │  │ (Optional)  │  │    API      │                   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REMOTE (Backend/Cloud)                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │   │
│  │  │ Backend API │  │ Backend API │  │ Backend API │  │ Backend API │  │   │
│  │  │   (LLM)     │  │   (TTS)     │  │   (STT)     │  │  (Images)   │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  │   │
│  │          │              │               │                │           │   │
│  │          └──────────────┴───────────────┴────────────────┘           │   │
│  │                              │                                        │   │
│  │                    Existing Spring Boot Backend                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                      EXTERNAL (Third-Party)                           │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │  Image Libraries: Unsplash, Pexels, Pixabay (for flashcards)    │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Storage Layer                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  IndexedDB  │  │   Cache API │  │localStorage │  │  OPFS (Origin      │ │
│  │  (Models)   │  │  (Assets)   │  │ (Settings)  │  │  Private FS)       │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Operation Modes

The application supports three operation modes, configurable by the user:

### Mode 1: Backend Mode (Current Default)
```
User ←→ Frontend ←→ Backend API ←→ AI Providers (OpenAI/Ollama/etc.)
```
- Full functionality with cloud AI providers
- Requires running backend server
- Lowest latency for powerful models
- API costs apply

### Mode 2: Browser Mode (Frontend-Only)
```
User ←→ Frontend ←→ Browser AI (WebLLM/Whisper WASM/Web Speech)
```
- No backend required
- Models run locally in browser
- Works offline after initial download
- Zero API costs
- Device capability dependent

### Mode 3: Hybrid Mode (Recommended Default)
```
User ←→ Frontend ←→ [Browser AI (primary) | Backend API (fallback)]
```
- Browser AI for supported operations
- Automatic fallback to backend when:
  - Device lacks capability (no WebGPU)
  - Model not downloaded yet
  - Complex operation requires larger model
  - User preference
- Best balance of privacy, cost, and quality

### Mode Selection Logic

```javascript
// Pseudo-code for provider selection
function selectProvider(capability, userPreference) {
    if (userPreference === 'backend-only') {
        return BackendProvider;
    }
    
    if (userPreference === 'browser-only') {
        if (BrowserProvider.isSupported(capability)) {
            return BrowserProvider;
        }
        throw new Error('Browser mode not supported for this capability');
    }
    
    // Hybrid mode (default)
    if (BrowserProvider.isReady(capability)) {
        return BrowserProvider;
    }
    if (BackendProvider.isAvailable()) {
        return BackendProvider;
    }
    
    // Prompt user to download model or configure backend
    return PromptUserForSetup;
}
```

---

## Technology Stack

### Core Technologies

| Technology | Purpose | Browser Support |
|------------|---------|-----------------|
| **WebGPU** | GPU acceleration for LLM | Chrome 113+, Edge 113+, Firefox (flag) |
| **WebAssembly** | Near-native performance | All modern browsers |
| **ONNX Runtime Web** | Model inference | All modern browsers |
| **IndexedDB** | Model storage (GBs) | All modern browsers |
| **Web Workers** | Background processing | All modern browsers |
| **OPFS** | Fast file access | Chrome 102+, Safari 15.2+, Firefox 111+ |

### Libraries & Frameworks

| Library | Version | Purpose | Size |
|---------|---------|---------|------|
| **@mlc-ai/web-llm** | 0.2.x | WebGPU LLM inference | ~500KB |
| **@xenova/transformers** | 2.x | ONNX model inference | ~1MB |
| **onnxruntime-web** | 1.x | ONNX backend | ~2MB |
| **idb** | 8.x | IndexedDB wrapper | ~10KB |

### Recommended Models (~1GB Target)

| Capability | Model | Size | Quality | Notes |
|------------|-------|------|---------|-------|
| **LLM** | Qwen2.5-1.5B-Instruct | ~900MB | Good | Best for language tasks |
| **LLM** | Phi-3.5-mini-instruct | ~2.2GB | Better | If user has more storage |
| **LLM** | SmolLM2-1.7B-Instruct | ~1GB | Good | HuggingFace optimized |
| **STT** | Whisper Tiny | ~39MB | Basic | Fast, good for short phrases |
| **STT** | Whisper Base | ~74MB | Good | Recommended default |
| **STT** | Whisper Small | ~244MB | Better | For accuracy priority |
| **TTS** | Piper (voice file) | ~50MB | Good | Per-language voice |

---

## LLM Implementation (Priority 1)

### Recommended Approach: WebLLM (Primary) + Transformers.js (Fallback)

#### WebLLM (WebGPU)
- **Pros**: Best performance, supports streaming, wide model selection
- **Cons**: Requires WebGPU (not all browsers/devices)
- **Use for**: Devices with WebGPU support

#### Transformers.js (WASM/WebGPU)
- **Pros**: Broader compatibility, HuggingFace ecosystem
- **Cons**: Slightly slower than WebLLM for large models
- **Use for**: Fallback when WebGPU unavailable

### LLM Service Interface

```javascript
// frontend/js/services/ai/llm-service.js

/**
 * @typedef {Object} LlmOptions
 * @property {number} [maxTokens=1024] - Maximum tokens to generate
 * @property {number} [temperature=0.7] - Randomness (0-2)
 * @property {boolean} [stream=false] - Enable streaming responses
 */

/**
 * @typedef {Object} LlmResponse
 * @property {string} text - Generated text
 * @property {number} tokensUsed - Tokens consumed
 * @property {number} inferenceTimeMs - Generation time
 */

/**
 * LLM Service Interface
 * All implementations must provide these methods
 */
class LlmService {
    /**
     * Generate text from a prompt
     * @param {string} systemPrompt - System instructions
     * @param {string} userPrompt - User message
     * @param {LlmOptions} [options] - Generation options
     * @returns {Promise<LlmResponse>}
     */
    async generate(systemPrompt, userPrompt, options = {}) { }
    
    /**
     * Generate with streaming response
     * @param {string} systemPrompt
     * @param {string} userPrompt
     * @param {function(string): void} onChunk - Callback for each chunk
     * @param {LlmOptions} [options]
     * @returns {Promise<LlmResponse>}
     */
    async generateStream(systemPrompt, userPrompt, onChunk, options = {}) { }
    
    /**
     * Check if the service is ready
     * @returns {Promise<boolean>}
     */
    async isReady() { }
    
    /**
     * Get model download progress
     * @returns {Promise<{downloaded: number, total: number, percentage: number}>}
     */
    async getDownloadProgress() { }
    
    /**
     * Initialize/download the model
     * @param {function(number): void} [onProgress] - Progress callback (0-100)
     * @returns {Promise<void>}
     */
    async initialize(onProgress) { }
    
    /**
     * Unload model from memory
     * @returns {Promise<void>}
     */
    async unload() { }
}
```

### Prompt Adaptation for Smaller Models

Smaller models (1-2B parameters) require more explicit prompting. Current backend prompts may need adaptation:

```javascript
// Current backend-style prompt (works with GPT-4, large models)
const backendPrompt = `Generate 5 fill-in-the-blank exercises for learning French...`;

// Adapted prompt for smaller models (more structured)
const browserPrompt = `Task: Create language exercises
Language: French
Type: Fill-in-the-blank
Count: 5
Topic: Past tense verbs

Format your response as JSON:
{"exercises": [{"sentence": "...", "blank": "...", "answer": "..."}]}

Begin:`;
```

### WebLLM Implementation Skeleton

```javascript
// frontend/js/services/ai/providers/webllm-provider.js

import * as webllm from '@anthropic-ai/web-llm';

class WebLlmProvider {
    constructor() {
        this.engine = null;
        this.modelId = 'Qwen2.5-1.5B-Instruct-q4f16_1-MLC';
        this.isLoaded = false;
    }
    
    async initialize(onProgress) {
        const initProgressCallback = (progress) => {
            onProgress?.(Math.round(progress.progress * 100));
        };
        
        this.engine = await webllm.CreateMLCEngine(
            this.modelId,
            { initProgressCallback }
        );
        this.isLoaded = true;
    }
    
    async generate(systemPrompt, userPrompt, options = {}) {
        if (!this.isLoaded) {
            throw new Error('Model not loaded. Call initialize() first.');
        }
        
        const messages = [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt }
        ];
        
        const response = await this.engine.chat.completions.create({
            messages,
            max_tokens: options.maxTokens || 1024,
            temperature: options.temperature || 0.7
        });
        
        return {
            text: response.choices[0].message.content,
            tokensUsed: response.usage.total_tokens,
            inferenceTimeMs: response.usage.extra?.inference_time_ms || 0
        };
    }
    
    async generateStream(systemPrompt, userPrompt, onChunk, options = {}) {
        if (!this.isLoaded) {
            throw new Error('Model not loaded. Call initialize() first.');
        }
        
        const messages = [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt }
        ];
        
        let fullText = '';
        const stream = await this.engine.chat.completions.create({
            messages,
            max_tokens: options.maxTokens || 1024,
            temperature: options.temperature || 0.7,
            stream: true
        });
        
        for await (const chunk of stream) {
            const delta = chunk.choices[0]?.delta?.content || '';
            fullText += delta;
            onChunk(delta);
        }
        
        return { text: fullText, tokensUsed: 0, inferenceTimeMs: 0 };
    }
    
    async isReady() {
        return this.isLoaded && this.engine !== null;
    }
    
    async unload() {
        if (this.engine) {
            await this.engine.unload();
            this.engine = null;
            this.isLoaded = false;
        }
    }
}
```

---

## Speech Services

### Text-to-Speech (TTS)

#### Strategy: Web Speech API (Primary) + Piper WASM (Enhanced)

**Web Speech API** - Native browser TTS
- Zero download, instant availability
- Quality varies by OS/browser
- Limited voice control
- Best for: Quick playback, mobile devices

**Piper WASM** - High-quality neural TTS
- ~50-150MB voice download per language
- Consistent quality across platforms
- Natural-sounding voices
- Best for: Desktop, quality-focused users

```javascript
// TTS Service Interface
class TtsService {
    async speak(text, languageCode, options = {}) { }
    async getAvailableVoices(languageCode) { }
    async isReady() { }
}
```

### Speech-to-Text (STT)

#### Strategy: Whisper WASM (Primary) + Web Speech API (Fallback)

**Whisper WASM**
- Excellent accuracy
- Multi-language support
- Works offline
- ~74MB (base model)

**Web Speech API**
- No download required
- Requires internet (usually)
- Limited language support
- Privacy concerns (cloud processing)

```javascript
// STT Service Interface
class SttService {
    async transcribe(audioBlob, languageHint) { }
    async startRealtimeTranscription(onPartialResult, languageHint) { }
    async stopRealtimeTranscription() { }
    async isReady() { }
}
```

---

## Image Generation (Deferred)

### Current Decision
Image generation in the browser is **deferred** due to:
- Large model sizes (2-4GB for SD Turbo)
- High GPU memory requirements
- Long generation times (10-30s)
- Limited mobile support

### Alternative Approaches

#### Option A: Third-Party Image Libraries (Recommended)
Use free image APIs with vocabulary search:
- **Unsplash** - High-quality photos, free API
- **Pexels** - Stock photos, free API
- **Pixabay** - Illustrations and photos, free API

```javascript
// Image search for flashcards
async function findFlashcardImage(word, targetLanguage) {
    // Translate word to English for better search results
    const searchTerm = await translateToEnglish(word, targetLanguage);
    
    // Search Unsplash
    const response = await fetch(
        `https://api.unsplash.com/search/photos?query=${searchTerm}&per_page=1`,
        { headers: { Authorization: `Client-ID ${UNSPLASH_ACCESS_KEY}` } }
    );
    
    const data = await response.json();
    return data.results[0]?.urls?.small;
}
```

#### Option B: Pre-cached Images
- Generate common vocabulary images at build time
- Ship with application or download on demand
- ~500 images × ~50KB = ~25MB for common vocabulary

#### Option C: Emoji/SVG Placeholders
- Use Unicode emoji for simple concepts
- Generate simple SVG illustrations
- Zero download, instant display

#### Option D: Backend-Only (Current)
- Keep image generation as backend feature
- Only available in Backend or Hybrid mode

---

## Challenges & Mitigations

### Challenge 1: Model Download Size (~1GB)
**Impact**: Long initial wait, bandwidth costs, storage use

**Mitigations**:
- Progressive download with clear progress indication
- Background download while user reads instructions
- Offer "lite" models for quick start (~300MB)
- Store in IndexedDB for persistence
- Implement model pruning/quantization

### Challenge 2: WebGPU Availability
**Impact**: Not all browsers/devices support WebGPU

**Current Support** (March 2026):
- ✅ Chrome 113+ (desktop & Android)
- ✅ Edge 113+
- ✅ Safari 18+ (macOS & iOS)
- ⚠️ Firefox (behind flag)

**Mitigations**:
- Feature detection at startup
- WASM fallback via Transformers.js
- Clear messaging about browser requirements
- Automatic fallback to backend mode

```javascript
function checkWebGPUSupport() {
    if (!navigator.gpu) {
        return { supported: false, reason: 'WebGPU not available' };
    }
    return { supported: true };
}
```

### Challenge 3: Memory Constraints
**Impact**: Mobile devices have limited RAM (2-4GB typical)

**Mitigations**:
- Use quantized models (q4 = 4-bit quantization)
- Unload models when not in use
- Monitor memory usage, warn user
- Smaller models for mobile (Qwen2-0.5B)

### Challenge 4: Inference Speed
**Impact**: Slower than cloud APIs, especially on CPU

**Typical Performance**:
| Device | Model | Tokens/sec |
|--------|-------|------------|
| RTX 3080 (WebGPU) | Qwen2.5-1.5B | ~50-80 |
| M1 Mac (WebGPU) | Qwen2.5-1.5B | ~30-50 |
| iPhone 15 (WebGPU) | Qwen2.5-1.5B | ~15-25 |
| Desktop CPU (WASM) | Qwen2.5-1.5B | ~5-15 |

**Mitigations**:
- Streaming responses (start showing immediately)
- Cache common generations
- Pre-generate content during idle time
- Lower max tokens for interactive use

### Challenge 5: Model Quality vs. Size
**Impact**: Smaller models may produce lower quality output

**Mitigations**:
- Use instruction-tuned models optimized for tasks
- Craft prompts specifically for smaller models
- Post-processing/validation of outputs
- Hybrid mode: use backend for complex tasks

### Challenge 6: First-Run Experience
**Impact**: User must wait for model download before first use

**Mitigations**:
- Offer "Try Now" with backend fallback
- Background download with progress
- Onboarding flow explaining benefits
- Download during tutorial/setup

---

## Opportunities

### Opportunity 1: Zero Operating Costs
- No API fees for AI operations
- Reduced server infrastructure needs
- Scale to unlimited users without cost increase

### Opportunity 2: Complete Privacy
- All data stays on user's device
- GDPR/privacy compliance simplified
- Attractive for enterprise/education

### Opportunity 3: Offline Learning
- Learn anywhere without internet
- Perfect for travel, commutes
- Developing regions with poor connectivity

### Opportunity 4: Instant Response (After Load)
- No network latency for inference
- Consistent performance regardless of location
- Better UX for interactive exercises

### Opportunity 5: User Empowerment
- Users control their AI models
- Can choose quality vs. performance
- No vendor lock-in concerns

### Opportunity 6: Edge AI Learning Platform
- Position as innovative, future-forward
- Academic/research interest
- Open source community engagement

---

## Open Questions

### Technical Questions

1. **Model Selection Strategy**
   - Should we support multiple model sizes (small/medium/large)?
   - How do we benchmark models for language learning specifically?
   - Should users be able to bring their own models?

2. **Storage Management**
   - How to handle multiple language models (1GB each)?
   - Should we offer cloud storage for models?
   - Quota warnings and cleanup strategies?

3. **Cross-Tab Coordination**
   - If user opens multiple tabs, share model or load separately?
   - Use SharedArrayBuffer for memory efficiency?
   - Service Worker for model management?

4. **Update Strategy**
   - How to update models without re-downloading everything?
   - Delta updates possible for quantized models?
   - Version management and migration?

### UX Questions

5. **Mode Discovery**
   - How do users learn about browser mode?
   - Default to hybrid or let user choose?
   - How to explain trade-offs simply?

6. **Progress Communication**
   - How to show model download progress?
   - What to do during long generation?
   - How to handle errors gracefully?

7. **Quality Expectations**
   - Will users accept slightly lower quality for privacy?
   - How to A/B test browser vs. backend quality?
   - Feedback mechanism for quality issues?

### Product Questions

8. **Feature Parity**
   - Which features require backend mode?
   - Is 100% parity achievable? Desirable?
   - How to communicate feature differences?

9. **Monetization Impact**
   - Does browser mode reduce premium tier value?
   - Can browser mode be a premium feature?
   - Partnership opportunities with model providers?

10. **Support Complexity**
    - How to debug issues across browsers/devices?
    - Documentation requirements?
    - Community support vs. direct support?

---

## Implementation Roadmap

### Phase 0: POC Validation (This Sprint)
- [ ] Create standalone WebLLM test page
- [ ] Benchmark Qwen2.5-1.5B for language learning prompts
- [ ] Test model loading/caching behavior
- [ ] Validate WebGPU detection and fallback
- [ ] Document findings and go/no-go decision

### Phase 1: Service Abstraction (2-3 weeks)
- [ ] Design provider interface contracts
- [ ] Implement `LlmService` with provider selection
- [ ] Implement WebLLM provider
- [ ] Implement backend provider (adapter to existing API)
- [ ] Add feature detection and capability reporting

### Phase 2: Model Management (2 weeks)
- [ ] Create `ModelManager` service
- [ ] Implement IndexedDB storage for models
- [ ] Add download progress tracking
- [ ] Implement lazy loading strategy
- [ ] Add storage quota management

### Phase 3: TTS Integration (1-2 weeks)
- [ ] Implement Web Speech API provider
- [ ] Evaluate Piper WASM integration
- [ ] Create TTS service with provider selection
- [ ] Integrate with listening exercises

### Phase 4: STT Integration (2 weeks)
- [ ] Implement Whisper WASM provider
- [ ] Create STT service with provider selection
- [ ] Integrate with pronunciation exercises
- [ ] Add real-time transcription support

### Phase 5: Settings & UI (1-2 weeks)
- [ ] Add AI mode selection to settings
- [ ] Create model management UI
- [ ] Add storage usage display
- [ ] Implement first-run setup flow

### Phase 6: Testing & Polish (2 weeks)
- [ ] Cross-browser testing
- [ ] Mobile device testing
- [ ] Performance optimization
- [ ] Error handling and fallbacks
- [ ] Documentation

---

## POC Structure

```
poc/browser-ai/
├── README.md                    # This document
├── FINDINGS.md                  # POC results and benchmarks (to be created)
├── webllm-test/
│   ├── index.html              # Standalone WebLLM test page
│   ├── webllm-test.js          # Test implementation
│   └── prompts.js              # Language learning prompts for testing
├── whisper-test/
│   ├── index.html              # Whisper WASM test page
│   └── whisper-test.js         # Test implementation
├── tts-test/
│   ├── index.html              # TTS comparison test page
│   └── tts-test.js             # Web Speech vs Piper comparison
└── benchmarks/
    ├── llm-benchmarks.json     # LLM performance data
    ├── stt-benchmarks.json     # STT accuracy data
    └── device-matrix.json      # Device compatibility matrix
```

---

## References

### WebLLM
- Repository: https://github.com/mlc-ai/web-llm
- Documentation: https://mlc.ai/web-llm/
- Model List: https://mlc.ai/web-llm/#702-models

### Transformers.js
- Repository: https://github.com/xenova/transformers.js
- Documentation: https://huggingface.co/docs/transformers.js
- Model Hub: https://huggingface.co/models?library=transformers.js

### Whisper Web
- Repository: https://github.com/xenova/whisper-web
- Demo: https://huggingface.co/spaces/Xenova/whisper-web

### Piper WASM
- Repository: https://github.com/rhasspy/piper
- WASM Build: https://github.com/nicholasgriffintn/piper-wasm

### Web Speech API
- MDN: https://developer.mozilla.org/en-US/docs/Web/API/Web_Speech_API
- SpeechSynthesis: https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis
- SpeechRecognition: https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognition

---

## Next Steps

1. **Review this document** and provide feedback on architecture decisions
2. **Run POC validation** with WebLLM test page
3. **Benchmark results** to validate model selection
4. **Go/No-Go decision** based on POC findings
5. **Begin Phase 1** implementation if approved