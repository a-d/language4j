#!/usr/bin/env python3
"""
OpenAI-compatible TTS API server using Piper.

This server provides an OpenAI-compatible text-to-speech endpoint using
Piper for fast, high-quality neural TTS synthesis.
"""

import os
import io
import wave
import subprocess
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
import uvicorn

app = FastAPI(
    title="Piper TTS Server",
    description="OpenAI-compatible text-to-speech API using Piper",
    version="1.0.0"
)

# Configuration from environment
VOICE_PATH = os.getenv("PIPER_VOICE", "/app/voices/fr_FR-siwis-medium.onnx")
DEFAULT_SPEAKER = int(os.getenv("PIPER_SPEAKER", "0"))

# Global voice instance
voice = None


def load_voice():
    """Load Piper voice model."""
    global voice
    try:
        import piper
        print(f"Loading Piper voice: {VOICE_PATH}")
        voice = piper.PiperVoice.load(VOICE_PATH)
        print(f"Voice loaded successfully. Sample rate: {voice.config.sample_rate}")
        return True
    except Exception as e:
        print(f"Failed to load voice: {e}")
        return False


@app.on_event("startup")
async def startup():
    """Load voice model on server startup."""
    if not load_voice():
        print("Warning: Voice not loaded. TTS will not work until voice is available.")


class TTSRequest(BaseModel):
    """OpenAI-compatible TTS request model."""
    model: str = "tts-1"
    input: str
    voice: str = "alloy"
    response_format: str = "mp3"
    speed: float = 1.0


# Voice mapping for OpenAI compatibility
# All voices map to the single loaded French voice
VOICE_MAP = {
    "alloy": "default",
    "echo": "default",
    "fable": "default",
    "onyx": "default",
    "nova": "default",
    "shimmer": "default",
}


def convert_to_mp3(wav_data: bytes) -> bytes:
    """Convert WAV to MP3 using ffmpeg if available."""
    try:
        process = subprocess.Popen(
            ["ffmpeg", "-i", "pipe:0", "-f", "mp3", "-ab", "192k", "pipe:1"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        mp3_data, _ = process.communicate(input=wav_data)
        if process.returncode == 0 and mp3_data:
            return mp3_data
    except (FileNotFoundError, subprocess.SubprocessError):
        pass
    # Return original WAV if conversion fails
    return wav_data


@app.post("/v1/audio/speech")
async def synthesize(request: TTSRequest):
    """
    OpenAI-compatible TTS endpoint.
    
    Converts text to speech using Piper TTS.
    """
    if voice is None:
        raise HTTPException(status_code=503, detail="Voice model not loaded")
    
    if not request.input:
        raise HTTPException(status_code=400, detail="Input text is required")
    
    if len(request.input) > 4096:
        raise HTTPException(status_code=400, detail="Input text too long (max 4096 characters)")
    
    try:
        # Calculate length scale from speed (inverse relationship)
        # speed=1.0 -> length_scale=1.0
        # speed=2.0 -> length_scale=0.5 (faster)
        # speed=0.5 -> length_scale=2.0 (slower)
        length_scale = 1.0 / max(0.25, min(4.0, request.speed))
        
        # Generate audio
        audio_buffer = io.BytesIO()
        
        with wave.open(audio_buffer, "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)  # 16-bit
            wav_file.setframerate(voice.config.sample_rate)
            
            # Synthesize audio
            for audio_bytes in voice.synthesize_stream_raw(
                request.input,
                length_scale=length_scale
            ):
                wav_file.writeframes(audio_bytes)
        
        audio_buffer.seek(0)
        wav_data = audio_buffer.read()
        
        # Determine response format and convert if needed
        if request.response_format == "mp3":
            audio_data = convert_to_mp3(wav_data)
            content_type = "audio/mpeg"
            filename = "speech.mp3"
        elif request.response_format == "opus":
            # Would need opus encoder, return WAV for now
            audio_data = wav_data
            content_type = "audio/wav"
            filename = "speech.wav"
        elif request.response_format == "aac":
            # Would need AAC encoder, return WAV for now
            audio_data = wav_data
            content_type = "audio/wav"
            filename = "speech.wav"
        elif request.response_format == "flac":
            # Would need FLAC encoder, return WAV for now
            audio_data = wav_data
            content_type = "audio/wav"
            filename = "speech.wav"
        else:  # wav, pcm
            audio_data = wav_data
            content_type = "audio/wav"
            filename = "speech.wav"
        
        return Response(
            content=audio_data,
            media_type=content_type,
            headers={"Content-Disposition": f"attachment; filename={filename}"}
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Speech synthesis failed: {str(e)}")


@app.get("/health")
async def health():
    """Health check endpoint."""
    return {
        "status": "healthy" if voice is not None else "loading",
        "voice": VOICE_PATH,
        "sample_rate": voice.config.sample_rate if voice else None
    }


@app.get("/v1/voices")
async def list_voices():
    """List available voices (OpenAI compatibility)."""
    return {
        "voices": [
            {"voice_id": "alloy", "name": "Alloy", "language": "fr"},
            {"voice_id": "echo", "name": "Echo", "language": "fr"},
            {"voice_id": "fable", "name": "Fable", "language": "fr"},
            {"voice_id": "onyx", "name": "Onyx", "language": "fr"},
            {"voice_id": "nova", "name": "Nova", "language": "fr"},
            {"voice_id": "shimmer", "name": "Shimmer", "language": "fr"},
        ]
    }


@app.get("/")
async def root():
    """Root endpoint with API info."""
    return {
        "name": "Piper TTS Server",
        "version": "1.0.0",
        "voice": VOICE_PATH,
        "endpoints": {
            "synthesize": "POST /v1/audio/speech",
            "voices": "GET /v1/voices",
            "health": "GET /health"
        }
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=int(os.getenv("PORT", "9001")),
        log_level="info"
    )