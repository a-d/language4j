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
| Stable Diffusion | 7860 | Yes | Flashcard images |

## Resource Usage

With all services loaded:
- **VRAM**: ~18-22GB (can peak during concurrent use)
- **RAM**: ~16-20GB for models + OS/containers
- **Disk**: ~60-80GB for all model weights

## Testing

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