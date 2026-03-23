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
   - Install Adrenalin drivers **26.3.1** or later from [AMD](https://www.amd.com/en/support)
   - Ensure DirectML and ROCm support are enabled

4. **Required CLI Tools** (in WSL2)
   ```bash
   # Install jq for JSON parsing (required by test-services.sh)
   sudo apt-get update && sudo apt-get install -y jq curl bc
   
   # Verify installation
   jq --version
   ```

---

## ROCm Setup for AMD GPUs

ROCm (Radeon Open Compute) is AMD's platform for GPU computing, required for AI acceleration.

### WSL2 ROCm Installation

```bash
# In WSL2 Ubuntu terminal

# 1. Clean up any previous ROCm installation (if applicable)
sudo amdgpu-install --uninstall -y
sudo apt purge amdgpu-install -y && sudo apt autoremove -y
sudo apt clean && sudo apt update

# 2. Install ROCm 7.2 for Ubuntu 22.04
wget https://repo.radeon.com/amdgpu-install/7.2/ubuntu/jammy/amdgpu-install_7.2.70200-1_all.deb
sudo apt install ./amdgpu-install_7.2.70200-1_all.deb
sudo amdgpu-install --usecase=wsl,rocm --no-dkms -y

# 3. Add user to render and video groups
sudo usermod -a -G render,video $USER

# 4. Set environment variables (add to ~/.bashrc)
# Add the following block at the end of ~/.bashrc:
cat >> ~/.bashrc << 'EOF'

# --- ROCm CONFIG ---
export LD_LIBRARY_PATH=/opt/rocm/lib:/opt/rocm/lib64:$LD_LIBRARY_PATH
export PATH=$PATH:/opt/rocm/bin

# Force RDNA3 compatibility (7900 XTX = gfx1100)
export HSA_OVERRIDE_GFX_VERSION=11.0.0

# Stability fix for WSL2
export HSA_ENABLE_SDMA=0

# CRITICAL: force system HIP lib, prevents PyTorch using its broken bundled one
export LD_PRELOAD=/opt/rocm/lib/libamdhip64.so
EOF

source ~/.bashrc

# 5. Verify installation
rocminfo
# Note: rocm-smi does NOT work in WSL2 - this is expected
```

### Verify GPU Access

```bash
# Check compute capability - verify gfx1100 detection
rocminfo | grep -A2 "gfx1100"
```

**Expected output for RX 7900 XTX**: You should see `gfx1100` in the output, indicating your RDNA3 GPU is properly detected.

> **Note**: `rocm-smi` does **not** work in WSL2 - this is expected behavior. Use `rocminfo` for verification instead.

---

## LLM Deployment (Ollama)

Ollama is recommended for its simplicity, excellent ROCm support, and OpenAI-compatible API.

### Installation Options

Ollama can be deployed in two ways:

#### Option A: Docker (Recommended for this project)

The `docker-compose.yml` includes Ollama with ROCm support. No separate installation needed—just start the containers:

```bash
cd local-deployment/docker
docker-compose up -d ollama
```

The image `ollama/ollama:rocm` is pre-configured for AMD GPUs.

#### Option B: Native Installation (Alternative)

If you prefer running Ollama natively on WSL2:

```bash
# In WSL2 Ubuntu terminal

# 1. Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 2. Verify installation
ollama --version

# 3. Start Ollama server with ROCm support
# Add to ~/.bashrc for persistence:
echo 'export HSA_OVERRIDE_GFX_VERSION=11.0.0' >> ~/.bashrc
source ~/.bashrc

# 4. Start the Ollama service
ollama serve

# In another terminal, test GPU detection:
ollama run qwen2.5:1.5b "Hello"
```

**Note**: If using native installation, comment out the `ollama` service in `docker-compose.yml` to avoid port conflicts.

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

**Recommended models by Docker memory limit**:
- **Default Docker (16-20GB)**: `qwen2.5:7b-instruct-q5_K_M` (~10GB RAM)
- **Increased Docker memory (32GB+)**: `qwen2.5:14b-instruct-q5_K_M` (~21GB RAM)
- **Max Docker memory (40GB+)**: `qwen2.5:32b-instruct-q5_K_M` (~37GB RAM)

> **Note**: Docker Desktop defaults to limited memory. To use larger models, increase Docker memory in Settings → Resources → Memory, or configure WSL memory in `~/.wslconfig`.

### Pull and Test Models

```bash
# Pull recommended model for default Docker settings
ollama pull qwen2.5:7b-instruct-q5_K_M

# For systems with increased Docker memory (32GB+)
ollama pull qwen2.5:14b-instruct-q5_K_M

# For systems with max Docker memory (40GB+)
ollama pull qwen2.5:32b-instruct-q5_K_M

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

> **WSL2 Note**: `rocm-smi` does **not** work in WSL2. Use `rocminfo` for GPU verification instead.

#### 1. ROCm Not Detecting GPU

```bash
# Check if GPU is visible using rocminfo
rocminfo | grep -A2 "gfx1100"

# If not found, check device permissions
ls -la /dev/dxg /dev/dri

# Add user to groups
sudo usermod -a -G video,render $USER
# Log out and back in (or restart WSL)
```

#### 2. VRAM Out of Memory

```bash
# Monitor GPU memory via Ollama logs or task manager on Windows
# In WSL2, rocm-smi memory commands don't work

# Reduce model size or use quantization
ollama pull llama3.1:70b-instruct-q4_K_M  # q4 instead of q5

# Or use smaller model
ollama pull qwen2.5:14b-instruct-q5_K_M
```

#### 3. Slow Generation

```bash
# Verify GPU is detected
rocminfo | grep -A2 "gfx1100"

# If GPU utilization seems low, check:
# 1. ROCm version compatibility
# 2. HSA_OVERRIDE_GFX_VERSION setting (should be 11.0.0)
# 3. LD_PRELOAD is set correctly
# 4. Model is actually loaded on GPU (check Ollama logs)
```

#### 4. Docker GPU Access Issues

```bash
# Verify Docker can see GPU using rocminfo
docker run --rm -it --device=/dev/dxg --device=/dev/dri rocm/pytorch:rocm6.0_ubuntu22.04_py3.10_pytorch_2.1.1 rocminfo | grep gfx

# If permission denied, check group membership in container
docker run --rm -it --device=/dev/dxg --device=/dev/dri --group-add video --group-add render rocm/pytorch:rocm6.0_ubuntu22.04_py3.10_pytorch_2.1.1 rocminfo | grep gfx
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
  -F "file=@piper_test.wav" \
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