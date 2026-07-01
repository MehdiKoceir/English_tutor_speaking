import os
import httpx
import json
import asyncio
from typing import List, Optional
from fastapi import FastAPI, Header, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

app = FastAPI(title="English Tutor Ollama Proxy API", version="1.0.0")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Adjust in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration from Environment Variables
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "llama3.1:8b")
API_KEY = os.getenv("API_KEY", "my_secure_secret_tutor_key")

# Simple In-Memory Rate Limiter to prevent bot abuse without complex dependencies
rate_limit_store = {} # { api_key: [timestamps] }
LIMIT_WINDOW = 60 # seconds
LIMIT_REQUESTS = 30 # requests per minute

async def verify_api_key(x_api_key: Optional[str] = Header(None)):
    """Validates the X-API-Key header against the configured key."""
    if not x_api_key or x_api_key != API_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-API-Key header"
        )
    
    # Simple Rate Limiting Check
    import time
    now = time.time()
    user_requests = rate_limit_store.get(x_api_key, [])
    # Filter out requests older than the window
    user_requests = [t for t in user_requests if now - t < LIMIT_WINDOW]
    
    if len(user_requests) >= LIMIT_REQUESTS:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Rate limit exceeded. Maximum 30 requests per minute."
        )
        
    user_requests.append(now)
    rate_limit_store[x_api_key] = user_requests
    return x_api_key


# --- Pydantic Request Models ---
class ChatRequest(BaseModel):
    conversation_id: str
    message: str
    level: str # beginner, intermediate, advanced

class CorrectRequest(BaseModel):
    text: str


# --- Endpoints ---

@app.get("/health")
async def health_check():
    """Simple health check endpoint."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{OLLAMA_HOST}/api/tags")
            if response.status_code == 200:
                return {"status": "healthy", "ollama": "connected", "model": OLLAMA_MODEL}
    except Exception:
        pass
    return {"status": "unhealthy", "ollama": "disconnected"}


@app.post("/chat", dependencies=[Depends(verify_api_key)])
async def chat(request: ChatRequest):
    """
    Streams a tutor response token-by-token using SSE.
    Adapts system prompt vocabulary and style based on level.
    """
    system_prompt = f"""
You are a friendly, patient English tutor.
You are practicing English conversation with a student.
The student's English proficiency level is: {request.level}.

Guidelines:
- Adapt your vocabulary, sentence length, and syntax density to match the level ({request.level}):
  * beginner: Use short, simple sentences, easy vocabulary, and straightforward syntax.
  * intermediate: Use clear sentences, moderate vocabulary, and occasionally introduce common idioms.
  * advanced: Use normal, fluent English with professional vocabulary, natural idioms, and complex structures.
- Correct the user's mistakes gently if you spot any, and explain them briefly.
- Keep the conversation highly interactive: always end your response with an open-ended question to keep the conversation going.
- Keep responses concise (2 to 4 sentences) to keep it digestible.
"""

    async def event_generator():
        ollama_url = f"{OLLAMA_HOST}/api/chat"
        payload = {
            "model": OLLAMA_MODEL,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": request.message}
            ],
            "stream": True,
            "options": {
                "temperature": 0.7
            }
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                async with client.stream("POST", ollama_url, json=payload) as response:
                    if response.status_code != 200:
                        yield f"data: [Error: Ollama returned status {response.status_code}]\n\n"
                        return

                    async for line in response.aiter_lines():
                        if not line:
                            continue
                        try:
                            chunk = json.loads(line)
                            content = chunk.get("message", {}).get("content", "")
                            if content:
                                # Stream as Server-Sent Events (SSE) data chunk
                                yield f"data: {content}\n\n"
                        except json.JSONDecodeError:
                            continue
                    yield "data: [DONE]\n\n"
        except Exception as e:
            yield f"data: [Error: connection to Ollama failed: {str(e)}]\n\n"

    return StreamingResponse(event_generator(), mediaType="text/event-stream")


@app.post("/correct", dependencies=[Depends(verify_api_key)])
async def correct(request: CorrectRequest):
    """
    Analyzes sentence for errors and returns corrections as structured JSON.
    """
    prompt = f"""
Analyze the following English text for spelling, grammar, punctuation, or vocabulary mistakes:
"{request.text}"

Provide feedback in the following strict JSON format:
{{
  "corrected": "Write the fully corrected sentence here. If perfectly correct, repeat the input verbatim.",
  "explanations": [
    {{
      "original": "the exact segment that was incorrect",
      "fixed": "the corrected segment",
      "reason": "short explanation of why it was wrong and how to fix it"
    }}
  ]
}}

Ensure you only return valid, parseable JSON and nothing else.
"""

    ollama_url = f"{OLLAMA_HOST}/api/generate"
    payload = {
        "model": OLLAMA_MODEL,
        "prompt": prompt,
        "format": "json",
        "stream": False,
        "options": {
            "temperature": 0.0 # High determinism for correction
        }
    }

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(ollama_url, json=payload)
            if response.status_code != 200:
                raise HTTPException(status_code=500, detail="Ollama correction request failed")
            
            result = response.json()
            response_text = result.get("response", "{}")
            
            # Parse the structured JSON response
            return json.loads(response_text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to communicate with Ollama: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
