"""
Haru AI 챗봇 (TinyLlama) FastAPI 서버.

체인: Front → Gateway(8080) /api/assist/tinylamanaver/chat → AssistService(8082)
      → LlamaServiceImpl.processWithLlama → Gateway /api/fastapi/chat → (여기) 8001

모델은 llama.cpp(gguf) 기반. Apple Silicon 에서는 Metal 백엔드가 자동 사용된다.
경로/포트 등은 환경변수로 외부화한다(소스 하드코딩 금지):
  LLAMA_MODEL_PATH  (기본: models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf)
  LLAMA_HOST        (기본: 0.0.0.0)
  LLAMA_PORT        (기본: 8001)
  LLAMA_N_CTX       (기본: 2048)
  LLAMA_N_GPU_LAYERS(기본: -1 = 가능한 만큼 GPU 오프로드)
"""
import logging
import os
from datetime import datetime
from typing import Dict, List

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from llama_cpp import Llama
from pydantic import BaseModel

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler("llama_server.log"),
        logging.StreamHandler(),
    ],
)
logger = logging.getLogger(__name__)

app = FastAPI()

# CORS 설정 (프론트/게이트웨이 진입점 허용)
origins = [
    "http://localhost:8080",
    "http://localhost:3000",
    "http://gateway-container:8080",
    "http://core-container:8081",
    "http://assist-container:8082",
    "https://sunbee.world",
    "*",  # 개발 중에는 모든 origin 허용
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"],
)

# 모델 설정 — 윈도우 절대경로 하드코딩 제거, 환경변수로 외부화
MODEL_PATH = os.getenv(
    "LLAMA_MODEL_PATH",
    os.path.join(os.path.dirname(__file__), "models", "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"),
)
N_CTX = int(os.getenv("LLAMA_N_CTX", "2048"))
N_GPU_LAYERS = int(os.getenv("LLAMA_N_GPU_LAYERS", "-1"))

logger.info("모델 로딩 중... path=%s", MODEL_PATH)
if not os.path.exists(MODEL_PATH):
    raise FileNotFoundError(
        f"모델 파일을 찾을 수 없습니다: {MODEL_PATH}. "
        "LLAMA_MODEL_PATH 환경변수로 .gguf 경로를 지정하거나 models/ 에 모델을 내려받으세요."
    )
llm = Llama(
    model_path=MODEL_PATH,
    n_ctx=N_CTX,
    n_gpu_layers=N_GPU_LAYERS,
    chat_format="zephyr",  # TinyLlama-1.1B-Chat-v1.0 은 zephyr(<|user|>/<|assistant|>) 포맷
    verbose=False,
)
logger.info("모델 로딩 완료!")

SYSTEM_PROMPT = """
You are the AI customer support agent for the Haru app. Your name is Luffy.

Haru is a "market where hobbies become talents." It provides a space where anyone can
share, trade, and experience their hobbies or skills with others. Be friendly, concise,
and professional. Consider the conversation context for consistency. Offer step-by-step
guidance when explaining features. Redirect payment/personal-data/security inquiries to
dedicated support. If unsure, honestly say "I'm not sure" instead of guessing.

Core features: hobby-based talent market (list & trade products), real-time chat &
notifications, location-based discovery (nearby filter), community groups.

Navigation: Main (/), bottom menu = Market / Chat / Notification / Location / My Page.
Accounts: login (/login), signup (/signup), password recovery, delete account
(/delete-account). Marketplace: register product (/product/register), product list &
filtering, details (/product-details), favorites (/mypage). Chat: 1:1 (/chat/:email),
AI chatbot (/servicechat). Location: set my location (/my-location), distance filter.
Payments: card registration (/ocr-upload, /registered-card) — note: payment is currently
not implemented. Notifications & support: notification center (/notification), customer
center (/cs-list), 1:1 inquiry (/inquiry-history).

FAQ:
- Register a product: Market tab > + button 'Register Product' > fill the form.
- Chat with a user: click 'Apply' on the product detail page, or go to the chat page.
- Set location: bottom-menu Location icon > Set My Location.
- No chat notifications: check Settings > Notification Preferences.
- See my listed products: My Page > Product Management.
""".strip()


class ChatRequest(BaseModel):
    message: str
    history: list = []
    sessionId: str  # AssistService 가 사용자 이메일을 세션 ID로 전달


@app.get("/api/fastapi/health")
def health() -> Dict[str, str]:
    return {"status": "ok", "model": os.path.basename(MODEL_PATH)}


@app.post("/api/fastapi/chat")
async def chat(request: ChatRequest) -> Dict[str, str]:
    try:
        logger.info(
            "채팅 요청 | time=%s session=%s msg=%r history=%d",
            datetime.now(), request.sessionId, request.message, len(request.history),
        )

        # 메시지 구성: 시스템 + 최근 히스토리(최대 4턴) + 현재 메시지
        messages: List[Dict[str, str]] = [{"role": "system", "content": SYSTEM_PROMPT}]
        for msg in request.history[-4:]:
            if msg.get("user"):
                messages.append({"role": "user", "content": msg["user"]})
            if msg.get("assistant"):
                messages.append({"role": "assistant", "content": msg["assistant"]})
        messages.append({"role": "user", "content": request.message})

        result = llm.create_chat_completion(
            messages=messages,
            temperature=0.7,
            top_p=0.95,
            repeat_penalty=1.15,
            max_tokens=512,
        )
        response = result["choices"][0]["message"]["content"].strip()

        logger.info("응답 생성 완료 | session=%s len=%d", request.sessionId, len(response))
        return {"response": response}

    except Exception as e:
        error_msg = f"에러 발생 - 세션 ID: {request.sessionId}, 에러: {str(e)}"
        logger.error(error_msg)
        raise HTTPException(status_code=500, detail=error_msg)


if __name__ == "__main__":
    host = os.getenv("LLAMA_HOST", "0.0.0.0")
    port = int(os.getenv("LLAMA_PORT", "8001"))
    logger.info("=== LLaMA(gguf) 서버 시작 === model=%s host=%s port=%d", MODEL_PATH, host, port)
    uvicorn.run(app, host=host, port=port, log_level="info")
