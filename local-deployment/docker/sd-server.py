#!/usr/bin/env python3
"""
OpenAI-compatible Stable Diffusion API server.

This server provides an OpenAI-compatible image generation endpoint using
Stable Diffusion with ROCm/CUDA acceleration via the Diffusers library.

Features fully lazy loading - PyTorch is only imported when generating images
to avoid high CPU usage from ROCm polling when idle.
"""

import os
import io
import base64
import time
import threading
from typing import Optional, List

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
import uvicorn

app = FastAPI(
    title="Stable Diffusion Server",
    description="OpenAI-compatible image generation API using Stable Diffusion",
    version="1.0.0"
)

# Configuration from environment
MODEL_ID = os.getenv("SD_MODEL", "stabilityai/sdxl-turbo")
DEVICE = os.getenv("SD_DEVICE", "cuda")  # ROCm uses cuda interface
USE_FLOAT16 = os.getenv("SD_FLOAT16", "true").lower() == "true"
IDLE_TIMEOUT = int(os.getenv("SD_IDLE_TIMEOUT", "300"))  # Unload after 5 min idle

# Global state - PyTorch and model are loaded lazily
_torch_imported = False
_pipe = None
_pipe_lock = threading.Lock()
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


def _diagnose_gpu(torch_module):
    """Print GPU diagnostic information."""
    print("=" * 60)
    print("GPU DIAGNOSTIC INFORMATION")
    print("=" * 60)
    
    # Environment variables
    print("\n--- Environment Variables ---")
    env_vars = [
        "HSA_OVERRIDE_GFX_VERSION",
        "HSA_ENABLE_SDMA", 
        "HIP_VISIBLE_DEVICES",
        "PYTORCH_HIP_ALLOC_CONF",
        "LD_PRELOAD",
        "ROCM_PATH",
    ]
    for var in env_vars:
        print(f"  {var}: {os.getenv(var, 'NOT SET')}")
    
    # PyTorch info
    print("\n--- PyTorch Info ---")
    print(f"  PyTorch version: {torch_module.__version__}")
    print(f"  CUDA available: {torch_module.cuda.is_available()}")
    print(f"  CUDA version: {torch_module.version.cuda if hasattr(torch_module.version, 'cuda') else 'N/A'}")
    print(f"  HIP version: {torch_module.version.hip if hasattr(torch_module.version, 'hip') else 'N/A'}")
    
    # Device info
    print("\n--- Device Info ---")
    if torch_module.cuda.is_available():
        print(f"  Device count: {torch_module.cuda.device_count()}")
        for i in range(torch_module.cuda.device_count()):
            print(f"  Device {i}: {torch_module.cuda.get_device_name(i)}")
            props = torch_module.cuda.get_device_properties(i)
            print(f"    Total memory: {props.total_memory / 1024**3:.1f} GB")
    else:
        print("  No CUDA/HIP devices available!")
        print("\n--- Possible causes ---")
        print("  1. ROCm not properly installed in container")
        print("  2. /dev/dxg not mounted (check docker-compose devices)")
        print("  3. LD_PRELOAD not set correctly")
        print("  4. HSA_OVERRIDE_GFX_VERSION mismatch with your GPU")
    
    print("=" * 60)
    return torch_module.cuda.is_available()


def _load_model():
    """Load Stable Diffusion model (called lazily on first request)."""
    global _pipe, _last_used
    
    # Import torch lazily
    torch = _import_torch()
    
    # Run diagnostics
    gpu_available = _diagnose_gpu(torch)
    
    try:
        from diffusers import AutoPipelineForText2Image
        
        print(f"\nLoading Stable Diffusion model: {MODEL_ID}")
        print(f"Device: {DEVICE}, Float16: {USE_FLOAT16}")
        
        if not gpu_available and DEVICE == "cuda":
            print("WARNING: CUDA/HIP not available but DEVICE is set to 'cuda'")
            print("Model loading may fail. Consider setting SD_DEVICE=cpu as fallback.")
        
        # Determine torch dtype
        dtype = torch.float16 if USE_FLOAT16 else torch.float32
        
        # Load pipeline
        _pipe = AutoPipelineForText2Image.from_pretrained(
            MODEL_ID,
            torch_dtype=dtype,
            variant="fp16" if USE_FLOAT16 else None
        )
        _pipe.to(DEVICE)
        
        # Enable memory optimizations
        _pipe.enable_attention_slicing()
        
        # Try to enable xformers if available
        try:
            _pipe.enable_xformers_memory_efficient_attention()
            print("xformers memory efficient attention enabled")
        except Exception:
            print("xformers not available, using default attention")
        
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
    global _pipe, _unload_timer, _torch_imported
    
    with _pipe_lock:
        if _pipe is not None:
            import gc
            
            print("Unloading Stable Diffusion model due to idle timeout...")
            
            # Import torch only if we have a model to unload
            torch = _import_torch()
            
            # Move to CPU first, then delete
            try:
                _pipe.to("cpu")
            except Exception:
                pass
            
            del _pipe
            _pipe = None
            
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
    global _pipe, _last_used
    
    with _pipe_lock:
        if _pipe is None:
            if not _load_model():
                return False
        
        _last_used = time.time()
        _schedule_unload()
        return True


@app.on_event("startup")
async def startup():
    """Server startup - model is loaded lazily on first request."""
    print(f"Stable Diffusion server started (lazy loading enabled)")
    print(f"Model will be loaded on first request and unloaded after {IDLE_TIMEOUT}s idle")
    print(f"Configured model: {MODEL_ID}")
    print("NOTE: PyTorch is NOT imported at startup to avoid CPU polling")


class ImageGenerationRequest(BaseModel):
    """OpenAI-compatible image generation request model."""
    model: str = Field(default="dall-e-3", description="Model to use (ignored, uses configured SD model)")
    prompt: str = Field(..., description="Text prompt for image generation")
    n: int = Field(default=1, ge=1, le=4, description="Number of images to generate")
    size: str = Field(default="1024x1024", description="Image size (e.g., 512x512, 1024x1024)")
    quality: str = Field(default="standard", description="Quality: standard or hd")
    style: str = Field(default="natural", description="Style: vivid or natural")
    response_format: str = Field(default="url", description="Response format: url or b64_json")


class ImageData(BaseModel):
    """Image data in response."""
    url: Optional[str] = None
    b64_json: Optional[str] = None
    revised_prompt: Optional[str] = None


class ImageGenerationResponse(BaseModel):
    """OpenAI-compatible image generation response."""
    created: int
    data: List[ImageData]


def _get_generation_params(request: ImageGenerationRequest) -> dict:
    """Get generation parameters based on model and request."""
    
    # Parse size
    try:
        width, height = map(int, request.size.split("x"))
    except ValueError:
        width, height = 1024, 1024
    
    # Clamp size to valid ranges
    width = min(max(width, 256), 1024)
    height = min(max(height, 256), 1024)
    
    # Determine parameters based on model type
    model_lower = MODEL_ID.lower()
    
    if "turbo" in model_lower:
        # SDXL Turbo: very fast, 4 steps, no CFG
        return {
            "num_inference_steps": 4,
            "guidance_scale": 0.0,
            "width": width,
            "height": height,
        }
    elif "lightning" in model_lower:
        # SDXL Lightning: fast, 4-8 steps, low CFG
        return {
            "num_inference_steps": 6,
            "guidance_scale": 2.0,
            "width": width,
            "height": height,
        }
    elif "sdxl" in model_lower:
        # Standard SDXL: higher quality, more steps
        steps = 30 if request.quality == "hd" else 20
        return {
            "num_inference_steps": steps,
            "guidance_scale": 7.5,
            "width": width,
            "height": height,
        }
    else:
        # SD 1.5 or other models
        steps = 30 if request.quality == "hd" else 20
        return {
            "num_inference_steps": steps,
            "guidance_scale": 7.5,
            "width": min(width, 512),
            "height": min(height, 512),
        }


@app.post("/v1/images/generations", response_model=ImageGenerationResponse)
async def generate_image(request: ImageGenerationRequest):
    """
    OpenAI-compatible image generation endpoint.
    
    Generates images from text prompts using Stable Diffusion.
    Model is loaded lazily on first request.
    """
    if not request.prompt:
        raise HTTPException(status_code=400, detail="Prompt is required")
    
    if len(request.prompt) > 4000:
        raise HTTPException(status_code=400, detail="Prompt too long (max 4000 characters)")
    
    # Ensure model is loaded (lazy loading)
    if not _ensure_model_loaded():
        raise HTTPException(status_code=503, detail="Failed to load model")
    
    try:
        # Import torch lazily
        torch = _import_torch()
        
        # Get generation parameters
        params = _get_generation_params(request)
        
        # Generate images (with lock to prevent concurrent access issues)
        images = []
        with _pipe_lock:
            for _ in range(request.n):
                with torch.no_grad():
                    result = _pipe(
                        prompt=request.prompt,
                        **params
                    )
                    images.append(result.images[0])
        
        # Convert to response format
        data = []
        for image in images:
            buffer = io.BytesIO()
            image.save(buffer, format="PNG")
            buffer.seek(0)
            b64_data = base64.b64encode(buffer.read()).decode()
            
            if request.response_format == "b64_json":
                data.append(ImageData(
                    b64_json=b64_data,
                    revised_prompt=request.prompt
                ))
            else:
                # Return as data URL
                data.append(ImageData(
                    url=f"data:image/png;base64,{b64_data}",
                    revised_prompt=request.prompt
                ))
        
        return ImageGenerationResponse(
            created=int(time.time()),
            data=data
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Image generation failed: {str(e)}")


@app.post("/v1/images/edits")
async def edit_image():
    """Image editing endpoint (not implemented)."""
    raise HTTPException(
        status_code=501,
        detail="Image editing is not implemented. Use /v1/images/generations instead."
    )


@app.post("/v1/images/variations")
async def create_variation():
    """Image variation endpoint (not implemented)."""
    raise HTTPException(
        status_code=501,
        detail="Image variations not implemented. Use /v1/images/generations instead."
    )


@app.get("/health")
async def health():
    """Health check endpoint."""
    model_status = "loaded" if _pipe is not None else "unloaded"
    idle_time = time.time() - _last_used if _last_used > 0 else 0
    
    return {
        "status": "healthy",
        "model_status": model_status,
        "torch_imported": _torch_imported,
        "model": MODEL_ID,
        "device": DEVICE,
        "float16": USE_FLOAT16,
        "idle_timeout": IDLE_TIMEOUT,
        "idle_seconds": int(idle_time) if model_status == "loaded" else None
    }


@app.get("/v1/models")
async def list_models():
    """List available models (OpenAI compatibility)."""
    return {
        "data": [
            {
                "id": "dall-e-3",
                "object": "model",
                "created": int(time.time()),
                "owned_by": "local",
                "description": f"Local Stable Diffusion model: {MODEL_ID}"
            },
            {
                "id": "dall-e-2",
                "object": "model",
                "created": int(time.time()),
                "owned_by": "local",
                "description": f"Local Stable Diffusion model: {MODEL_ID}"
            }
        ]
    }


@app.get("/")
async def root():
    """Root endpoint with API info."""
    return {
        "name": "Stable Diffusion Server",
        "version": "1.0.0",
        "model": MODEL_ID,
        "model_loaded": _pipe is not None,
        "torch_imported": _torch_imported,
        "idle_timeout": IDLE_TIMEOUT,
        "endpoints": {
            "generate": "POST /v1/images/generations",
            "models": "GET /v1/models",
            "health": "GET /health"
        },
        "notes": "Model AND PyTorch load lazily on first request and unload after idle timeout"
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=int(os.getenv("PORT", "7860")),
        log_level="info"
    )