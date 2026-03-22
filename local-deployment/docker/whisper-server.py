#!/usr/bin/env python3
"""
OpenAI-compatible Whisper API server for speech-to-text.

This server provides an OpenAI-compatible transcription endpoint using
faster-whisper for efficient inference with ROCm/CUDA acceleration.
"""

import os
import tempfile
from typing import Optional

from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
from faster_whisper import WhisperModel
import uvicorn

app = FastAPI(
    title="Whisper STT Server",
    description="OpenAI-compatible speech-to-text API using faster-whisper",
    version="1.0.0"
)

# Configuration from environment
MODEL_SIZE = os.getenv("WHISPER_MODEL", "large-v3")
DEVICE = os.getenv("WHISPER_DEVICE", "cuda")  # ROCm uses cuda interface
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "float16")

# Global model instance
model: Optional[WhisperModel] = None


@app.on_event("startup")
async def load_model():
    """Load Whisper model on server startup."""
    global model
    print(f"Loading Whisper model: {MODEL_SIZE}")
    print(f"Device: {DEVICE}, Compute type: {COMPUTE_TYPE}")
    
    model = WhisperModel(
        MODEL_SIZE,
        device=DEVICE,
        compute_type=COMPUTE_TYPE
    )
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
        # Transcribe with faster-whisper
        segments, info = model.transcribe(
            tmp_path,
            language=language,
            temperature=temperature,
            beam_size=5,
            vad_filter=True,
            initial_prompt=prompt,
        )
        
        # Collect all segments
        segment_list = list(segments)
        text = " ".join([segment.text.strip() for segment in segment_list])
        
        if response_format == "text":
            return text
        elif response_format == "verbose_json":
            return JSONResponse({
                "task": "transcribe",
                "language": info.language,
                "duration": info.duration,
                "text": text,
                "segments": [
                    {
                        "id": i,
                        "start": seg.start,
                        "end": seg.end,
                        "text": seg.text.strip(),
                        "tokens": list(seg.tokens) if seg.tokens else [],
                        "avg_logprob": seg.avg_logprob,
                        "no_speech_prob": seg.no_speech_prob,
                    }
                    for i, seg in enumerate(segment_list)
                ]
            })
        else:  # json (default)
            return JSONResponse({
                "text": text,
                "language": info.language,
                "duration": info.duration,
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
        "compute_type": COMPUTE_TYPE
    }


@app.get("/")
async def root():
    """Root endpoint with API info."""
    return {
        "name": "Whisper STT Server",
        "version": "1.0.0",
        "model": MODEL_SIZE,
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