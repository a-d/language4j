#!/usr/bin/env python3
"""
OpenAI-compatible Whisper API server for speech-to-text.

This server provides an OpenAI-compatible transcription endpoint using
the official openai-whisper library with PyTorch for ROCm acceleration.

Features fully lazy loading - PyTorch is only imported when transcribing
to avoid high CPU usage from ROCm polling when idle.
"""

import os
import gc
import tempfile
import time
import threading
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
IDLE_TIMEOUT = int(os.getenv("WHISPER_IDLE_TIMEOUT", "300"))  # Unload after 5 min idle

# Global state - PyTorch and model are loaded lazily
_torch_imported = False
_model = None
_model_lock = threading.Lock()
_last_used = 0.0
_unload_timer = None


def _import_torch():
    """Import torch lazily to avoid CPU usage when idle."""
    global _torch_imported
    if not _torch_imported:
        import torch
        _torch_imported = True
        return torch
    import torch
    return torch


def _load_model():
    """Load Whisper model (called lazily on first request)."""
    global _model, _last_used
    
    # Import torch lazily
    torch = _import_torch()
    
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
    
    try:
        # Import whisper lazily
        import whisper
        
        # Load model to specified device
        _model = whisper.load_model(MODEL_SIZE, device=device)
        _last_used = time.time()
        print("Model loaded successfully")
        return True
    except Exception as e:
        print(f"Failed to load model: {e}")
        import traceback
        traceback.print_exc()
        return False


def _unload_model():
    """Unload model from GPU to free resources and stop CPU polling."""
    global _model, _unload_timer
    
    with _model_lock:
        if _model is not None:
            print("Unloading Whisper model due to idle timeout...")
            
            # Import torch only if we have a model to unload
            torch = _import_torch()
            
            # Move to CPU first, then delete
            try:
                _model.to("cpu")
            except Exception:
                pass
            
            del _model
            _model = None
            
            # Force garbage collection and clear CUDA cache
            gc.collect()
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
                torch.cuda.synchronize()
            
            print("Model unloaded successfully")
        
        _unload_timer = None


def _schedule_unload():
    """Schedule model unload after idle timeout."""
    global _unload_timer
    
    # Cancel existing timer
    if _unload_timer is not None:
        _unload_timer.cancel()
    
    if IDLE_TIMEOUT > 0:
        _unload_timer = threading.Timer(IDLE_TIMEOUT, _unload_model)
        _unload_timer.daemon = True
        _unload_timer.start()


def _ensure_model_loaded():
    """Ensure model is loaded, loading it if necessary. Returns True if ready."""
    global _model, _last_used
    
    with _model_lock:
        if _model is None:
            if not _load_model():
                return False
        
        _last_used = time.time()
        _schedule_unload()
        return True


@app.on_event("startup")
async def startup():
    """Server startup - model is loaded lazily on first request."""
    print(f"Whisper server started (lazy loading enabled)")
    print(f"Model will be loaded on first request and unloaded after {IDLE_TIMEOUT}s idle")
    print(f"Configured model: {MODEL_SIZE}")
    print("NOTE: PyTorch is NOT imported at startup to avoid CPU polling")


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
    Model is loaded lazily on first request.
    """
    # Ensure model is loaded (lazy loading)
    if not _ensure_model_loaded():
        raise HTTPException(status_code=503, detail="Failed to load model")
    
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
        
        # Transcribe with openai-whisper (with lock to prevent concurrent access issues)
        with _model_lock:
            result = _model.transcribe(tmp_path, **options)
        
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
    model_status = "loaded" if _model is not None else "unloaded"
    idle_time = time.time() - _last_used if _last_used > 0 else 0
    
    return {
        "status": "healthy",
        "model_status": model_status,
        "torch_imported": _torch_imported,
        "model": MODEL_SIZE,
        "device": DEVICE,
        "idle_timeout": IDLE_TIMEOUT,
        "idle_seconds": int(idle_time) if model_status == "loaded" else None,
    }


@app.get("/")
async def root():
    """Root endpoint with API info."""
    return {
        "name": "Whisper STT Server",
        "version": "1.0.0",
        "model": MODEL_SIZE,
        "model_loaded": _model is not None,
        "torch_imported": _torch_imported,
        "idle_timeout": IDLE_TIMEOUT,
        "backend": "openai-whisper (PyTorch)",
        "endpoints": {
            "transcribe": "POST /v1/audio/transcriptions",
            "health": "GET /health"
        },
        "notes": "Model AND PyTorch load lazily on first request and unload after idle timeout"
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=int(os.getenv("PORT", "9000")),
        log_level="info"
    )