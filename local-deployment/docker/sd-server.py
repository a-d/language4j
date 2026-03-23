#!/usr/bin/env python3
"""
OpenAI-compatible Stable Diffusion API server.

This server provides an OpenAI-compatible image generation endpoint using
Stable Diffusion with ROCm/CUDA acceleration via the Diffusers library.
"""

import os
import io
import base64
import time
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

# Global pipeline instance
pipe = None


def diagnose_gpu():
    """Print GPU diagnostic information."""
    import torch
    import os
    
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
    print(f"  PyTorch version: {torch.__version__}")
    print(f"  CUDA available: {torch.cuda.is_available()}")
    print(f"  CUDA version: {torch.version.cuda if hasattr(torch.version, 'cuda') else 'N/A'}")
    print(f"  HIP version: {torch.version.hip if hasattr(torch.version, 'hip') else 'N/A'}")
    
    # Device info
    print("\n--- Device Info ---")
    if torch.cuda.is_available():
        print(f"  Device count: {torch.cuda.device_count()}")
        for i in range(torch.cuda.device_count()):
            print(f"  Device {i}: {torch.cuda.get_device_name(i)}")
            props = torch.cuda.get_device_properties(i)
            print(f"    Total memory: {props.total_memory / 1024**3:.1f} GB")
    else:
        print("  No CUDA/HIP devices available!")
        print("\n--- Possible causes ---")
        print("  1. ROCm not properly installed in container")
        print("  2. /dev/dxg not mounted (check docker-compose devices)")
        print("  3. LD_PRELOAD not set correctly")
        print("  4. HSA_OVERRIDE_GFX_VERSION mismatch with your GPU")
    
    print("=" * 60)
    return torch.cuda.is_available()


def load_model():
    """Load Stable Diffusion model."""
    global pipe
    
    # Run diagnostics first
    gpu_available = diagnose_gpu()
    
    try:
        import torch
        from diffusers import AutoPipelineForText2Image
        
        print(f"\nLoading Stable Diffusion model: {MODEL_ID}")
        print(f"Device: {DEVICE}, Float16: {USE_FLOAT16}")
        
        if not gpu_available and DEVICE == "cuda":
            print("WARNING: CUDA/HIP not available but DEVICE is set to 'cuda'")
            print("Model loading may fail. Consider setting SD_DEVICE=cpu as fallback.")
        
        # Determine torch dtype
        dtype = torch.float16 if USE_FLOAT16 else torch.float32
        
        # Load pipeline
        pipe = AutoPipelineForText2Image.from_pretrained(
            MODEL_ID,
            torch_dtype=dtype,
            variant="fp16" if USE_FLOAT16 else None
        )
        pipe.to(DEVICE)
        
        # Enable memory optimizations
        pipe.enable_attention_slicing()
        
        # Try to enable xformers if available
        try:
            pipe.enable_xformers_memory_efficient_attention()
            print("xformers memory efficient attention enabled")
        except Exception:
            print("xformers not available, using default attention")
        
        print("Model loaded successfully")
        return True
        
    except Exception as e:
        print(f"Failed to load model: {e}")
        return False


@app.on_event("startup")
async def startup():
    """Load model on server startup."""
    if not load_model():
        print("Warning: Model not loaded. Image generation will not work.")


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


def get_generation_params(request: ImageGenerationRequest) -> dict:
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
    """
    if pipe is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    if not request.prompt:
        raise HTTPException(status_code=400, detail="Prompt is required")
    
    if len(request.prompt) > 4000:
        raise HTTPException(status_code=400, detail="Prompt too long (max 4000 characters)")
    
    try:
        import torch
        
        # Get generation parameters
        params = get_generation_params(request)
        
        # Generate images
        images = []
        for _ in range(request.n):
            with torch.no_grad():
                result = pipe(
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
    return {
        "status": "healthy" if pipe is not None else "loading",
        "model": MODEL_ID,
        "device": DEVICE,
        "float16": USE_FLOAT16
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
        "endpoints": {
            "generate": "POST /v1/images/generations",
            "models": "GET /v1/models",
            "health": "GET /health"
        }
    }


if __name__ == "__main__":
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=int(os.getenv("PORT", "7860")),
        log_level="info"
    )