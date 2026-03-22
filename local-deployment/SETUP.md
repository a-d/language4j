# Local AI Deployment Guide

This guide covers deploying all AI services locally on a Windows machine with WSL2, Docker, and an AMD Radeon RX 7900 XTX GPU for the Language Learning Platform.

## Table of Contents

1. [Hardware & Software Requirements](#hardware--software-requirements)
2. [ROCm Setup for AMD GPUs](#rocm-setup-for-amd-gpus)
3. [LLM Deployment (Ollama)](#llm-deployment-ollama)
4. [Speech-to-Text (Whisper)](#speech-to-text-whisper)
5. [Text-to-Speech (Piper)](#text-to-speech-piper)
6. [Image Generation (Stable Diffusion)](#image-generation-stable-diffusion)
7. [Docker Compose Configuration](#docker-compose-configuration)
8. [Application Configuration](#application-configuration)
9. [Performance Tuning](#performance-tuning)
10. [Troubleshooting](#troubleshooting)

---

## Hardware & Software Requirements

### Your Hardware Profile

| Component | Specification |
|-----------|---------------|
| GPU | AMD Radeon RX 7900 XTX |
| VRAM | 24 GB GDDR6 |
| System RAM | 48 GB |
| OS | Windows 11 with WSL2 |

This configuration is excellent for local AI deployment:
- 24GB VRAM can run 70B parameter models (quantized) or multiple smaller models concurrently
- 48GB system RAM allows for efficient model loading and CPU offloading when needed

### Software Prerequisites

1. **WSL2 with Ubuntu 22.04+**
   ```powershell
   # In Windows PowerShell (Admin)
   wsl --install -d Ubuntu-22.04
   wsl --set-default-version 2
   ```

2. **Docker Desktop for Windows**
   - Download from [docker.com](https://www.docker.com/products/docker-desktop/)
   - Enable WSL2 backend in settings
   - Enable "Use WSL 2 based engine"

3. **AMD GPU Driver** (Windows)
   - Install latest Adrenalin drivers from [AMD](https://www.amd.com/en/support)
   - Ensure DirectML and ROCm support are enabled

---

## ROCm Setup for AMD GPUs

ROCm (Radeon Open Compute) is AMD's platform for GPU computing, required for AI acceleration.

### WSL2 ROCm Installation

```bash
# In WSL2 Ubuntu terminal

# 1. Add ROCm repository
sudo mkdir --parents --mode=0755 /etc/apt/keyrings
wget https://repo.radeon.com/rocm/rocm.gpg.key -O - | \
    gpg --dearmor | sudo tee /etc/apt/keyrings/rocm.gpg > /dev/null

echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/rocm.gpg] https://repo.radeon.com/rocm/apt/6.0 jammy main" | \
    sudo tee /etc/apt/sources.list.d/rocm.list

# 2. Update and install ROCm
sudo apt update
sudo apt install -y rocm-dev rocm-libs

# 3. Add user to render and video groups
sudo usermod -a -G render,video $USER

# 4. Set environment variables (add to ~/.bashrc)
echo 'export PATH=$PATH:/opt/rocm/bin' >> ~/.bashrc
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc  # For RX 7900 XTX (gfx1100)
source ~/.bashrc

# 5. Verify installation
rocminfo
rocm-smi
```

### Verify GPU Access

```bash
# Should show your RX 7900 XTX
rocm-smi --showproductname

# Check compute capability
rocminfo | grep -i "gfx"
```

**Expected output for RX 7900 XTX**: `gfx1100`

---

## LLM Deployment (Ollama)

Ollama is recommended for its simplicity, excellent ROCm support, and OpenAI-compatible API.

### Why Ollama over vLLM?

| Feature | Ollama | vLLM |
|---------|--------|------|
| Setup complexity | Simple | Complex |
| ROCm support | Excellent | Experimental |
| Model management | Built-in | Manual |
| OpenAI compatibility | Full | Full |
| Memory efficiency | Good | Better (PagedAttention) |
| Use case fit | General purpose | High-throughput servers |

For a personal language learning application, Ollama's simplicity and reliability outweigh vLLM's throughput advantages.

### Recommended Models for Language Learning

Given your 24GB VRAM and French language focus:

| Model | Size | VRAM | Strengths |
|-------|------|------|-----------|
| `llama3.1:70b-instruct-q4_K_M` | ~40GB disk | ~20GB VRAM | Best reasoning, instruction following |
| `qwen2.5:32b-instruct-q5_K_M` | ~23GB disk | ~18GB VRAM | Excellent multilingual, including French |
| `mistral-large:123b-instruct-q2_K` | ~50GB disk | ~22GB VRAM | Strong European language support |
| `mixtral:8x7b-instruct-v0.1-q5_K_M` | ~30GB disk | ~16GB VRAM | Good balance, fast inference |
| `command-r:35b-v01-q4_K_M` | ~20GB disk | ~14GB VRAM | Designed for multilingual/RAG |

**Recommended primary model**: `qwen2.5:32b-instruct-q5_K_M` or `llama3.1:70b-instruct-q4_K_M`

### Pull and Test Models

```bash
# Pull recommended model
ollama pull qwen2.5:32b-instruct-q5_K_M

# Alternative: Llama 3.1 70B (quantized)
ollama pull llama3.1:70b-instruct-q4_K_M

# Test the model
ollama run qwen2.5:32b-instruct-q5_K_M "Translate 'Hello, how are you?' to French"
```

### Ollama API Endpoints

Ollama provides an OpenAI-compatible API:

| Endpoint | Description |
|----------|-------------|
| `POST /api/generate` | Text generation (native) |
| `POST /api/chat` | Chat completion (native) |
| `POST /v1/chat/completions` | OpenAI-compatible chat |
| `POST /v1/completions` | OpenAI-compatible completion |
| `GET /api/tags` | List available models |

---

## Speech-to-Text (Whisper)

For speech recognition, we use `faster-whisper` with ROCm acceleration.

### Model Selection

| Model | Size | VRAM | Quality | Speed |
|-------|------|------|---------|-------|
| `tiny` | 75MB | ~1GB | Basic | Fastest |
| `base` | 142MB | ~1GB | Good | Fast |
| `small` | 466MB | ~2GB | Better | Medium |
| `medium` | 1.5GB | ~5GB | Great | Slower |
| `large-v3` | 3GB | ~10GB | Best | Slowest |

**Recommended**: `large-v3` for accuracy (you have plenty of VRAM)

### Whisper API Endpoints

The custom Whisper server provides OpenAI-compatible endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/audio/transcriptions` | POST | Transcribe audio file |
| `/health` | GET | Health check |

---

## Text-to-Speech (Piper)

Piper is a fast, lightweight neural TTS engine perfect for language learning.

### Why Piper?

- Extremely fast inference (real-time on CPU, faster on GPU)
- High-quality French voices available
- Low resource usage
- OpenAI-compatible API wrapper available

### French Voice Models

| Voice | Quality | Style |
|-------|---------|-------|
| `fr_FR-siwis-medium` | Medium | Neutral female |
| `fr_FR-siwis-low` | Lower | Faster, lighter |
| `fr_FR-upmc-medium` | Medium | Male voice |
| `fr_FR-gilles-low` | Lower | Male voice |

**Recommended**: `fr_FR-siwis-medium` for best quality/speed balance

### Piper API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/audio/speech` | POST | Generate speech from text |
| `/v1/voices` | GET | List available voices |
| `/health` | GET | Health check |

---

## Image Generation (Stable Diffusion)

For flashcard image generation, we use Stable Diffusion with ROCm.

### Model Options

| Model | VRAM | Quality | Speed | Use Case |
|-------|------|---------|-------|----------|
| SDXL 1.0 | ~8GB | Excellent | Medium | High-quality images |
| SDXL Turbo | ~8GB | Good | Fast | Quick generation |
| SD 1.5 | ~4GB | Good | Fast | Lightweight option |
| SDXL Lightning | ~8GB | Great | Very Fast | Best speed/quality |

**Recommended**: SDXL Turbo for flashcard generation (speed matters)

### Stable Diffusion API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/images/generations` | POST | Generate image from prompt |
| `/v1/models` | GET | List available models |
| `/health` | GET | Health check |

---

## Docker Compose Configuration

The `docker/docker-compose.yml` file orchestrates all AI services.

### Starting Services

```bash
cd local-deployment/docker

# Build all images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Post-Startup: Pull LLM Model

After starting Ollama, pull the recommended model:

```bash
docker exec -it language-learning-ollama ollama pull qwen2.5:32b-instruct-q5_K_M
```

---

## Application Configuration

### Environment Variables

Copy `.env.example` to `.env` at the project root and configure:

```bash
cp .env.example ../../.env
```

Key settings:

```bash
# LLM Configuration (Ollama)
LLM_PROVIDER=ollama
LLM_BASE_URL=http://localhost:11434
LLM_MODEL=qwen2.5:32b-instruct-q5_K_M

# Speech-to-Text (Local Whisper)
STT_BASE_URL=http://localhost:9000

# Text-to-Speech (Local Piper)
TTS_BASE_URL=http://localhost:9001

# Image Generation (Local Stable Diffusion)
IMAGE_BASE_URL=http://localhost:7860
```

---

## Performance Tuning

### VRAM Allocation Strategy

With 24GB VRAM, you can run multiple models but need to manage memory:

| Service | Estimated VRAM | Priority |
|---------|---------------|----------|
| Ollama (LLM) | 14-20GB | High (always loaded) |
| Whisper | 2-10GB | Medium (load on demand) |
| Stable Diffusion | 6-8GB | Low (load on demand) |
| **Total if all loaded** | 22-38GB | — |

### Memory Management Strategies

1. **Sequential Loading** (Recommended for your setup):
   - Keep LLM always loaded
   - Load Whisper/SD on demand
   - Ollama can offload to CPU RAM when needed

2. **Ollama Memory Settings**:
   ```bash
   # In docker-compose, set memory limits
   OLLAMA_MAX_VRAM=18G  # Reserve 6GB for other models
   ```

### GPU Optimization for ROCm

```bash
# Add to container environment
export HSA_OVERRIDE_GFX_VERSION=11.0.0
export PYTORCH_HIP_ALLOC_CONF=garbage_collection_threshold:0.6,max_split_size_mb:128
export HIP_VISIBLE_DEVICES=0
```

---

## Troubleshooting

### Common Issues

#### 1. ROCm Not Detecting GPU

```bash
# Check if GPU is visible
rocm-smi

# If not, check permissions
ls -la /dev/kfd /dev/dri

# Add user to groups
sudo usermod -a -G video,render $USER
# Log out and back in
```

#### 2. VRAM Out of Memory

```bash
# Check current VRAM usage
rocm-smi --showmeminfo vram

# Reduce model size or use quantization
ollama pull llama3.1:70b-instruct-q4_K_M  # q4 instead of q5

# Or use smaller model
ollama pull qwen2.5:14b-instruct-q5_K_M
```

#### 3. Slow Generation

```bash
# Check if GPU is being used
watch -n 1 rocm-smi

# If GPU utilization is low, check:
# 1. ROCm version compatibility
# 2. HSA_OVERRIDE_GFX_VERSION setting
# 3. Model is actually loaded on GPU
```

#### 4. Docker GPU Access Issues

```bash
# Verify Docker can see GPU
docker run --rm -it --device=/dev/kfd --device=/dev/dri rocm/pytorch:rocm6.0_ubuntu22.04_py3.10_pytorch_2.1.1 rocm-smi

# If permission denied, check group membership in container
docker run --rm -it --device=/dev/kfd --device=/dev/dri --group-add video --group-add render rocm/pytorch:rocm6.0_ubuntu22.04_py3.10_pytorch_2.1.1 rocm-smi
```

#### 5. Model Download Failures

```bash
# Manual model download for Ollama
ollama pull qwen2.5:32b-instruct-q5_K_M

# For Whisper, pre-download in container
docker exec -it language-learning-whisper python -c "from faster_whisper import WhisperModel; WhisperModel('large-v3')"

# For Stable Diffusion
docker exec -it language-learning-sd python -c "from diffusers import AutoPipelineForText2Image; AutoPipelineForText2Image.from_pretrained('stabilityai/sdxl-turbo')"
```

### Verification Commands

```bash
# Test Ollama
curl http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:32b-instruct-q5_K_M",
  "prompt": "Bonjour, comment allez-vous?",
  "stream": false
}'

# Test Whisper (upload an audio file)
curl -X POST http://localhost:9000/v1/audio/transcriptions \
  -F "file=@test.wav" \
  -F "language=fr"

# Test Piper TTS
curl -X POST http://localhost:9001/v1/audio/speech \
  -H "Content-Type: application/json" \
  -d '{"input": "Bonjour, comment allez-vous?", "voice": "alloy"}' \
  --output speech.wav

# Test Stable Diffusion
curl -X POST http://localhost:7860/v1/images/generations \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A simple illustration of a French croissant", "size": "512x512"}'
```

---

## Quick Start

1. **Install ROCm** (see [ROCm Setup](#rocm-setup-for-amd-gpus))

2. **Build and start services**:
   ```bash
   cd local-deployment/docker
   docker-compose build
   docker-compose up -d
   ```

3. **Pull the LLM model**:
   ```bash
   docker exec -it language-learning-ollama ollama pull qwen2.5:32b-instruct-q5_K_M
   ```

4. **Configure the application**:
   ```bash
   cp .env.example ../../.env
   ```

5. **Start the full application**:
   ```bash
   cd ../..
   docker-compose up -d
   ```

6. **Verify all services**:
   ```bash
   curl http://localhost:11434/api/tags  # Ollama
   curl http://localhost:9000/health     # Whisper
   curl http://localhost:9001/health     # Piper
   curl http://localhost:7860/health     # Stable Diffusion
   curl http://localhost:8080/actuator/health  # Backend
   ```

---

## Resource Summary

| Service | Container | Port | GPU Required |
|---------|-----------|------|--------------|
| Ollama (LLM) | language-learning-ollama | 11434 | Yes |
| Whisper (STT) | language-learning-whisper | 9000 | Yes |
| Piper (TTS) | language-learning-piper | 9001 | No (CPU) |
| Stable Diffusion | language-learning-sd | 7860 | Yes |