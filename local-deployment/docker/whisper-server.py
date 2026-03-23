#!/usr/bin/env python3
"""
OpenAI-compatible Whisper API server for speech-to-text.

This server provides an OpenAI-compatible transcription endpoint using
the official openai-whisper library with PyTorch for ROCm acceleration.
"""

import os
import tempfile
import torch
import whisper
from typing import Optional

from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
import uvicorn

app = FastAPI(
    title="Whisper STT Server",
    description="OpenAI-compatible speech-to-text API using openai-whisper with ROCm",
    version="1.0.0"
)

# Configuration from environment
MODEL_SIZE = os.getenv("WHISPER_MODEL", "large-v3")
DEVICE = os.getenv("WHISPER_DEVICE", "cuda")

# Global model instance
model: Optional[whisper.Whisper] = None


@app.on_event("startup")
async def load_model():
    """Load Whisper model on server startup."""
    global model
    
    # Check device availability
    if DEVICE == "cuda" and not torch.cuda.is_available():
        print("WARNING: CUDA requested but not available, falling back to CPU")
        device = "cpu"
    else:
        device = DEVICE
    
    print(f"Loading Whisper model: {MODEL_SIZE}")
    print(f"Device: {device}")
    print(f"PyTorch version: {torch.__version__}")
    print(f"CUDA available: {torch.cuda.is_available()}")
    if torch.cuda.is_available():
        print(f"CUDA device: {torch.cuda.get_device_name(0)}")
    
    # Load model to specified device
    model = whisper.load_model(MODEL_SIZE, device=device)
    print("Model loaded successfully")


@app.post("/v1/audio/transcriptions")
async def transcribe(
    file: UploadFile = File(..., description="Audio file to transcribe"),
    model_name: str = Form(default="whisper-1", alias="model"),
    language: Optional[str] = Form(default=None, description="Language code (e.g., 'fr', 'en')"),
    response_format: str = Form(default="json", description="Response format: json, text, verbose_json"),
    temperature: float = Form(default=0.0, description="Sampling temperature"),
    prompt: Optional[str] = Form(default=None, description="Optional context prompt"),
):
    """
    OpenAI-compatible transcription endpoint.
    
    Transcribes audio to text using the Whisper model.
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    # Save uploaded file temporarily
    suffix = os.path.splitext(file.filename)[1] if file.filename else ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name
    
    try:
        # Build transcription options
        options = {
            "temperature": temperature,
            "beam_size": 5,
        }
        
        if language:
            options["language"] = language
        
        if prompt:
            options["initial_prompt"] = prompt
        
        # Transcribe with openai-whisper
        result = model.transcribe(tmp_path, **options)
        
        text = result["text"].strip()
        detected_language = result.get("language", language or "unknown")
        
        # Calculate duration from segments if available
        duration = 0.0
        if result.get("segments"):
            last_segment = result["segments"][-1]
            duration = last_segment.get("end", 0.0)
        
        if response_format == "text":
            return text
        elif response_format == "verbose_json":
            return JSONResponse({
                "task": "transcribe",
                "language": detected_language,
                "duration": duration,
                "text": text,
                "segments": [
                    {
                        "id": i,
                        "start": seg.get("start", 0.0),
                        "end": seg.get("end", 0.0),
                        "text": seg.get("text", "").strip(),
                        "tokens": seg.get("tokens", []),
                        "avg_logprob": seg.get("avg_logprob", 0.0),
                        "no_speech_prob": seg.get("no_speech_prob", 0.0),
                    }
                    for i, seg in enumerate(result.get("segments", []))
                ]
            })
        else:  # json (default)
            return JSONResponse({
                "text": text,
                "language": detected_language,
                "duration": duration,
            })
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Transcription failed: {str(e)}")
    finally:
        # Clean up temporary file
        try:
            os.unlink(tmp_path)
        except OSError:
            pass


@app.get("/health")
async def health():
    """Health check endpoint."""
    return {
        "status": "healthy" if model is not None else "loading",
        "model": MODEL_SIZE,
        "device": DEVICE,
        "cuda_available": torch.cuda.is_available(),
        "cuda_device": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
    }


@app.get("/")
async def root():
    """Root endpoint with API info."""
    return {
        "name": "Whisper STT Server",
        "version": "1.0.0",
        "model": MODEL_SIZE,
        "backend": "openai-whisper (PyTorch)",
        "endpoints": {
            "transcribe": "POST /v1/audio/transcriptions",
            "health": "GET /health"
        }
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=int(os.getenv("PORT", "9000")),
        log_level="info"
    )