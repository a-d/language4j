# Local AI Deployment for Language Learning Platform

This directory contains my personal local AI deployment setup, specifically configured for:

- **Windows 11 with WSL2**
- **AMD Radeon RX 7900 XTX** (24GB VRAM)
- **48GB System RAM**
- **ROCm** for GPU acceleration

## ⚠️ Personal Configuration

This setup is tailored for my specific hardware and environment. If you're deploying the Language Learning Platform, refer to the main project's deployment documentation for generic instructions.

## Directory Structure

```
local-deployment/
├── docker/
│   ├── docker-compose.yml      # Main compose file for AI services
│   ├── Dockerfile.whisper      # Whisper STT with ROCm
│   ├── Dockerfile.piper        # Piper TTS (CPU-based)
│   ├── Dockerfile.sd           # Stable Diffusion with ROCm
│   ├── whisper-server.py       # OpenAI-compatible STT API
│   ├── piper-server.py         # OpenAI-compatible TTS API
│   └── sd-server.py            # OpenAI-compatible image API
├── .env.example                # Environment configuration template
├── SETUP.md                    # Detailed setup instructions
└── README.md                   # This file
```

## Quick Start

### Prerequisites

1. WSL2 with Ubuntu 22.04+
2. Docker Desktop with WSL2 backend
3. ROCm installed in WSL2 (see SETUP.md)
4. Required CLI tools in WSL2:
   ```bash
   sudo apt-get install -y jq curl bc
   ```

### Start AI Services

```bash
cd local-deployment/docker

# Build containers
docker-compose build

# Start all services
docker-compose up -d

# Pull the LLM model (first time only)
docker exec -it language-learning-ollama ollama pull qwen2.5:32b-instruct-q5_K_M
```

### Configure the Main Application

Copy `.env.example` to the project root as `.env`:

```bash
cp .env.example ../../.env
```

Then start the main application:

```bash
cd ../..
docker-compose up -d
```

## Services Overview

| Service | Port | GPU | Description |
|---------|------|-----|-------------|
| Ollama | 11434 | Yes | LLM for content generation |
| Whisper | 9000 | Yes | Speech-to-text |
| Piper | 9001 | No | Text-to-speech (French) |
| Stable Diffusion | 7860 | Yes | Flashcard images (lazy loading) |

### Lazy Loading & Auto-Restart (Whisper & Stable Diffusion)

Both Whisper and Stable Diffusion services use **lazy loading with auto-restart** to avoid high CPU usage when idle:

**Startup:**
- Containers start with **~0% CPU** (PyTorch not imported)
- Models load on **first request** (~10-30s cold start)

**After use:**
- After **5 minutes of inactivity** (configurable via `WHISPER_IDLE_TIMEOUT` / `SD_IDLE_TIMEOUT`):
  1. Model is unloaded from GPU
  2. **Container automatically exits**
  3. Docker restarts the container fresh
  4. Back to ~0% CPU usage

**Why auto-restart?**
PyTorch/ROCm creates background threads that poll the GPU even after model unload. The only way to stop this CPU usage is to restart the process. Docker's `restart: unless-stopped` policy handles this automatically.

**Health endpoint fields:**
- `model_status`: `loaded` or `unloaded`
- `torch_imported`: `true` or `false` (false = 0% CPU)
- `idle_seconds`: seconds since last use (when loaded)

## Resource Usage

With all services loaded:
- **VRAM**: ~18-22GB (can peak during concurrent use)
- **RAM**: ~16-20GB for models + OS/containers
- **Disk**: ~60-80GB for all model weights

## Testing

### Automated Test Script

The `test-services.sh` script provides comprehensive testing of all AI services:

```bash
# Standard test (health checks + functional tests, skips slow image generation)
./test-services.sh

# Quick test (health checks only)
./test-services.sh --quick

# Full test (includes image generation - can be slow on first run)
./test-services.sh --full
```

**What the script tests:**

| Service | Health Check | Functional Test |
|---------|--------------|-----------------|
| **Ollama** | API reachable, models listed | Text generation |
| **Whisper** | Model loaded, GPU status | Audio transcription |
| **Piper** | Voice loaded | Text-to-speech |
| **Stable Diffusion** | Model loaded | Image generation (--full only) |

Generated test files are saved to `test-output/`.

### Manual Testing

```bash
# Test Ollama
curl http://localhost:11434/api/tags

# Test Whisper
curl http://localhost:9000/health

# Test Piper
curl http://localhost:9001/health

# Test Stable Diffusion
curl http://localhost:7860/health
```

## Documentation

See `SETUP.md` for detailed setup instructions including:
- ROCm installation for AMD GPUs
- Model selection and configuration
- Performance tuning
- Troubleshooting common issues