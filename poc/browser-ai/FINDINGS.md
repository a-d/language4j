# Browser AI POC - Findings & Benchmarks

## Overview

This document records the findings from the Browser AI proof-of-concept testing. Results are gathered using the test pages in this directory.

**Test Date**: _______________  
**Tester**: _______________  
**Test Environment**: _______________

---

## 1. WebGPU Compatibility

### Browser Support Matrix

| Browser | Version | WebGPU | GPU Detected | Notes |
|---------|---------|--------|--------------|-------|
| Chrome | | ☐ Yes ☐ No | | |
| Edge | | ☐ Yes ☐ No | | |
| Firefox | | ☐ Yes ☐ No | | |
| Safari | | ☐ Yes ☐ No | | |
| Chrome Mobile | | ☐ Yes ☐ No | | |
| Safari iOS | | ☐ Yes ☐ No | | |

### Device Test Matrix

| Device | OS | Browser | WebGPU | RAM | GPU | Result |
|--------|-----|---------|--------|-----|-----|--------|
| | | | ☐ Yes ☐ No | | | |
| | | | ☐ Yes ☐ No | | | |
| | | | ☐ Yes ☐ No | | | |

---

## 2. LLM Model Benchmarks

### Model Download Performance

| Model | Size (MB) | Download Time | Cached? | Notes |
|-------|-----------|---------------|---------|-------|
| Qwen2.5-1.5B-Instruct | ~900 | | ☐ Yes ☐ No | |
| Qwen2.5-0.5B-Instruct | ~400 | | ☐ Yes ☐ No | |
| SmolLM2-1.7B-Instruct | ~1000 | | ☐ Yes ☐ No | |
| Phi-3.5-mini-instruct | ~2200 | | ☐ Yes ☐ No | |
| Gemma-2-2B-IT | ~1500 | | ☐ Yes ☐ No | |

### Model Loading Performance

| Model | Cold Load (s) | Warm Load (s) | Memory Usage | Notes |
|-------|---------------|---------------|--------------|-------|
| Qwen2.5-1.5B-Instruct | | | | |
| Qwen2.5-0.5B-Instruct | | | | |
| SmolLM2-1.7B-Instruct | | | | |

### Inference Performance

**Test Device**: _______________  
**GPU**: _______________

| Model | Prompt Type | Tokens Gen | Time (ms) | Tokens/sec | First Token (ms) |
|-------|-------------|------------|-----------|------------|------------------|
| Qwen2.5-1.5B | Vocabulary | | | | |
| Qwen2.5-1.5B | Exercise | | | | |
| Qwen2.5-1.5B | Translation | | | | |
| Qwen2.5-1.5B | Conversation | | | | |
| Qwen2.5-1.5B | Grammar | | | | |
| Qwen2.5-1.5B | Evaluation | | | | |
| Qwen2.5-0.5B | Vocabulary | | | | |
| Qwen2.5-0.5B | Exercise | | | | |

---

## 3. Output Quality Assessment

### JSON Format Compliance

| Model | Prompt Type | Valid JSON? | Follows Schema? | Notes |
|-------|-------------|-------------|-----------------|-------|
| Qwen2.5-1.5B | Vocabulary | ☐ Yes ☐ No | ☐ Yes ☐ No | |
| Qwen2.5-1.5B | Exercise | ☐ Yes ☐ No | ☐ Yes ☐ No | |
| Qwen2.5-1.5B | Translation | ☐ Yes ☐ No | ☐ Yes ☐ No | |
| Qwen2.5-1.5B | Conversation | ☐ Yes ☐ No | ☐ Yes ☐ No | |
| Qwen2.5-1.5B | Grammar | ☐ Yes ☐ No | ☐ Yes ☐ No | |
| Qwen2.5-1.5B | Evaluation | ☐ Yes ☐ No | ☐ Yes ☐ No | |

### Content Quality (1-5 scale)

| Model | Prompt Type | Accuracy | Relevance | Completeness | Language Quality | Overall |
|-------|-------------|----------|-----------|--------------|------------------|---------|
| Qwen2.5-1.5B | Vocabulary | /5 | /5 | /5 | /5 | /5 |
| Qwen2.5-1.5B | Exercise | /5 | /5 | /5 | /5 | /5 |
| Qwen2.5-1.5B | Translation | /5 | /5 | /5 | /5 | /5 |
| Qwen2.5-1.5B | Conversation | /5 | /5 | /5 | /5 | /5 |

### Sample Outputs

#### Vocabulary Generation

**Prompt**: (Vocabulary template)

**Model**: Qwen2.5-1.5B

**Output**:
```json
(paste output here)
```

**Assessment**:
- Valid JSON: ☐ Yes ☐ No
- All required fields: ☐ Yes ☐ No
- French language correct: ☐ Yes ☐ No
- Pronunciations accurate: ☐ Yes ☐ No
- Example sentences natural: ☐ Yes ☐ No

---

#### Exercise Generation

**Prompt**: (Exercise template)

**Model**: Qwen2.5-1.5B

**Output**:
```json
(paste output here)
```

**Assessment**:
- Valid JSON: ☐ Yes ☐ No
- Exercises appropriate for A2: ☐ Yes ☐ No
- Answers correct: ☐ Yes ☐ No
- Hints helpful: ☐ Yes ☐ No

---

## 4. Comparison: Browser vs Backend

### Response Quality Comparison

| Task | Backend (GPT-4/Large) | Browser (Qwen 1.5B) | Notes |
|------|----------------------|---------------------|-------|
| Vocabulary | /5 | /5 | |
| Exercises | /5 | /5 | |
| Translation | /5 | /5 | |
| Conversation | /5 | /5 | |
| Grammar | /5 | /5 | |
| Evaluation | /5 | /5 | |

### Latency Comparison

| Task | Backend (ms) | Browser (ms) | Network Latency | Browser Advantage? |
|------|--------------|--------------|-----------------|-------------------|
| Vocabulary | | | | ☐ Yes ☐ No |
| Exercises | | | | ☐ Yes ☐ No |
| Short response | | | | ☐ Yes ☐ No |

---

## 5. Memory & Resource Usage

### Browser Memory Impact

| Model | Initial Memory | After Load | During Inference | After Unload |
|-------|----------------|------------|------------------|--------------|
| Qwen2.5-1.5B | | | | |
| Qwen2.5-0.5B | | | | |

### Tab Performance

| Scenario | Response Time | Memory | Notes |
|----------|---------------|--------|-------|
| Single tab, model loaded | | | |
| Multiple tabs, model loaded | | | |
| Background tab inference | | | |

---

## 6. Error Handling

### Error Scenarios Tested

| Scenario | Behavior | Recovery | Notes |
|----------|----------|----------|-------|
| Network disconnect during download | | | |
| Tab close during inference | | | |
| GPU memory exhaustion | | | |
| Invalid prompt format | | | |
| Very long prompt | | | |

---

## 7. User Experience Observations

### First-Run Experience

- Time to first usable state: _______________
- User feedback during download: ☐ Clear ☐ Confusing
- Progress indication: ☐ Accurate ☐ Misleading
- Error messages: ☐ Helpful ☐ Unhelpful

### Ongoing Usage

- Model load time (cached): _______________
- Perceived responsiveness: ☐ Good ☐ Acceptable ☐ Poor
- Streaming quality: ☐ Smooth ☐ Choppy
- Memory management: ☐ Stable ☐ Problematic

---

## 8. Recommendations

### Go / No-Go Decision

☑️ **CONDITIONAL** - Proceed with modifications (see below)

### Reasoning

Browser-based AI is viable for language learning content generation with the right model choice. The key findings:

1. **Qwen3 1.7B works well** - Good balance of size (~1GB), speed, and quality
2. **Firefox has VRAM limits** - Larger models (1.5B+) may OOM in Firefox; smaller models work better
3. **Chrome has better WebGPU** - More reliable for larger models
4. **Pure translation models (Opus-MT) can't replace instruction LLMs** - They don't support prompts or JSON output

---

### Recommended Model

**Primary**: **Qwen3-1.7B** (`Qwen3-1.7B-q4f16_1-MLC`)  
**Rationale**: Best balance of quality, size (~1GB), and browser compatibility. Handles JSON output well for vocabulary, exercises, and grammar explanations. Works in both Chrome and Firefox with sufficient VRAM.

**Fallback (Low Memory)**: **Qwen2.5-0.5B** (`Qwen2.5-0.5B-Instruct-q4f16_1-MLC`)  
**Rationale**: Only ~400MB, works on devices with limited VRAM. Quality is lower but acceptable for basic vocabulary and simple exercises.

**Fallback (No WebGPU)**: **Backend API**  
**Rationale**: Fall back to existing Spring AI backend when browser doesn't support WebGPU or device lacks resources.

---

### Prompt Modifications Required

| Task | Modification Needed | Notes |
|------|---------------------|-------|
| Vocabulary | ☐ None ☐ Minor ☐ Major | |
| Exercises | ☐ None ☐ Minor ☐ Major | |
| Translation | ☐ None ☐ Minor ☐ Major | |
| Conversation | ☐ None ☐ Minor ☐ Major | |
| Grammar | ☐ None ☐ Minor ☐ Major | |
| Evaluation | ☐ None ☐ Minor ☐ Major | |

---

### Implementation Priorities

Based on findings, recommended implementation order:

1. _______________
2. _______________
3. _______________

---

### Risk Mitigation

| Risk | Mitigation Strategy |
|------|---------------------|
| WebGPU not available | |
| Model quality insufficient | |
| Memory constraints on mobile | |
| Slow inference on low-end devices | |

---

## 9. Mobile LLM Assessment

### Decision: ❌ NO-GO for Client-Side Offline Mobile LLM

**We will NOT implement client-side offline LLM on mobile devices.**

### Reasoning

The language learning use case (German ↔ French with English prompts) is particularly challenging for small models:

1. **Trilingual Context Requirement**
   - English system prompts and instructions
   - German source/target content
   - French source/target content
   - Small models (<2GB) lack sufficient multilingual training data to handle this well

2. **Model Size vs Quality Trade-off**
   | Model Size | Download | Quality for DE↔FR | Viable on Mobile? |
   |------------|----------|-------------------|-------------------|
   | 0.5B | ~400MB | ❌ Poor | ⚠️ Maybe |
   | 1.5-2B | ~1GB | ⚠️ Marginal | ❌ No (VRAM) |
   | 3B+ | ~2GB+ | ✅ Acceptable | ❌ No |
   | 7B+ | ~4GB+ | ✅ Good | ❌ No |

3. **Mobile WebGPU Limitations**
   - Mobile Safari: No WebGPU support
   - Mobile Chrome: Limited WebGPU, insufficient VRAM for instruction-following models
   - Most phones have 4-6GB total RAM shared between system and apps

4. **Quality Degradation**
   - Small models produce incorrect grammar explanations
   - German/French gender agreement errors
   - JSON output often malformed
   - Not acceptable for language learning where accuracy is critical

### Supported Platforms for Browser LLM

| Platform | LLM Support | Notes |
|----------|-------------|-------|
| Desktop Chrome/Edge | ✅ Yes | Primary target, Qwen3 1.7B |
| Desktop Firefox | ⚠️ Limited | VRAM constraints, smaller models only |
| Desktop Safari | ⚠️ Limited | WebGPU partial support |
| Mobile Chrome | ❌ No | Insufficient resources |
| Mobile Safari | ❌ No | No WebGPU |
| Mobile Apps | ❌ No | Out of scope |

### Alternative Approach for Mobile

Instead of client-side LLM, mobile users will:

1. **Use backend API** when online (primary)
2. **Access pre-cached content** when offline:
   - Pre-generated vocabulary lists
   - Pre-generated exercises
   - Cached lesson content
3. **Use Opus-MT** for quick translations (small footprint, works in WASM)

### Future Considerations

Re-evaluate in 12-18 months when:
- WebGPU support improves on mobile
- Smaller instruction-following models improve (e.g., sub-1B with better multilingual)
- Phone hardware advances (more VRAM, better GPU)
- Apple/Google native ML frameworks mature

---

## 10. Additional Notes

(Any other observations, edge cases, or considerations)

---

## 11. Translation Models Research

### Model Categories for Translation

| Category | Models | Size | Prompts | JSON Output | Best Use Case |
|----------|--------|------|---------|-------------|---------------|
| **Pure Translation** | Helsinki-NLP/Opus-MT | ~75MB/pair | ❌ No | ❌ No | Quick word/sentence translation |
| **Multi-language Translation** | Facebook/NLLB-200 | 600MB-1.3GB | ❌ No | ❌ No | 200 languages in one model |
| **Instruction-Following LLM** | Qwen2.5, Phi-3, etc. | 400MB-2GB | ✅ Yes | ✅ Yes | Structured content generation |

### Recommended Approach for Language Learning

**For structured content generation (vocabulary, exercises, explanations):**
- Use **WebLLM with Qwen3 1.7B** (recommended primary model)
- Supports system prompts and JSON output
- Required for language learning content that needs structure
- Fallback: Qwen2.5-0.5B for low-memory devices

**For quick translation features (tooltip, word lookup):**
- Use **Helsinki-NLP/Opus-MT** via Transformers.js
- Very fast (~100-500ms)
- Small footprint (~75MB per language pair)
- No prompt support - text-to-text only

### Helsinki-NLP/Opus-MT Details

**Characteristics:**
- **Type**: Sequence-to-sequence translation model
- **Input**: Plain text in source language
- **Output**: Plain text in target language (no structure possible)
- **Languages**: 1,000+ language pairs available
- **Size**: ~75MB per language pair
- **Speed**: Very fast (typically <500ms)
- **Browser Support**: WebGPU and WASM via Transformers.js

**Limitations:**
- ❌ No system prompts
- ❌ No JSON output formatting
- ❌ No explanations or context
- ❌ Separate model needed for each language pair

**Model IDs (Xenova/Transformers.js versions):**
- `Xenova/opus-mt-en-de` (English → German)
- `Xenova/opus-mt-de-en` (German → English)
- `Xenova/opus-mt-en-fr` (English → French)
- `Xenova/opus-mt-fr-en` (French → English)
- `Xenova/opus-mt-en-es` (English → Spanish)
- Many more at huggingface.co/Helsinki-NLP

### Facebook/NLLB-200 Details

**Characteristics:**
- **Type**: Multi-language translation model
- **Languages**: 200 languages in a single model
- **Variants**:
  - `nllb-200-distilled-600M` (~600MB) - Recommended for browser
  - `nllb-200-1.3B` (~1.3GB) - Better quality, heavier
- **Browser Support**: Transformers.js compatible

**Limitations:**
- ❌ No system prompts
- ❌ No JSON output formatting
- ⚠️ Larger download for first use
- ⚠️ Requires 4GB+ RAM

### Hardware Requirements Comparison

| Model | Download Size | RAM Needed | GPU/VRAM | Inference Speed |
|-------|---------------|-----------|----------|-----------------|
| Opus-MT (single pair) | 75-150MB | 2GB | Optional | Fast (100-500ms) |
| NLLB-200-distilled-600M | ~600MB | 4GB | Recommended | Medium (1-3s) |
| **WebLLM Qwen3-1.7B ⭐** | **~1GB** | **4GB** | **Required** | **Medium (2-4s)** |
| WebLLM Qwen2.5-1.5B | ~900MB | 4GB | Required | Slow (2-5s) |
| WebLLM Phi-3.5-mini | ~2.2GB | 8GB | Required | Slow (3-7s) |

### Browser Compatibility

| Browser | WebGPU | WASM | Recommended Model |
|---------|--------|------|-------------------|
| Chrome 113+ | ✅ | ✅ | Opus-MT / NLLB / WebLLM |
| Edge 113+ | ✅ | ✅ | Opus-MT / NLLB / WebLLM |
| Firefox 119+ | ✅ | ✅ | Opus-MT / NLLB / WebLLM |
| Safari 17+ | ⚠️ Partial | ✅ | Opus-MT (WASM) |
| Mobile Chrome | ⚠️ Limited | ✅ | Opus-MT only |
| Mobile Safari | ❌ | ✅ | Opus-MT only |

### Hybrid Architecture Recommendation

```
┌─────────────────────────────────────────────────────────────┐
│                    Browser AI Architecture                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────┐     ┌─────────────────────┐        │
│  │    WebLLM Engine    │     │   Transformers.js   │        │
│  │   (Qwen3 1.7B) ⭐    │     │    (Opus-MT)        │        │
│  └──────────┬──────────┘     └──────────┬──────────┘        │
│             │                           │                   │
│             ▼                           ▼                   │
│  ┌─────────────────────┐     ┌─────────────────────┐        │
│  │ Content Generation  │     │  Quick Translation  │        │
│  │ - Vocabulary lists  │     │  - Word tooltips    │        │
│  │ - Exercises         │     │  - Phrase lookup    │        │
│  │ - Grammar explain   │     │  - Real-time hints  │        │
│  │ - Chat responses    │     │                     │        │
│  │ - JSON structured   │     │  Plain text only    │        │
│  └─────────────────────┘     └─────────────────────┘        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 12. Attachments

- [ ] Screenshots of test results
- [ ] Console logs
- [ ] Sample outputs saved
- [ ] Performance trace files
