from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch
from typing import Dict
import uvicorn
from fastapi.middleware.cors import CORSMiddleware
import re
import asyncio
import logging
from datetime import datetime

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler('llama_server.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

app = FastAPI()

# CORS 설정
origins = [
    "http://localhost:8080",
    "http://localhost:3000",
    "http://gateway-container:8080",
    "http://core-container:8081",
    "http://assist-container:8082",
    "https://sunbee.world",
    "*"  # 개발 중에는 모든 origin 허용
]

logger.info("CORS 설정 초기화...")
logger.info(f"허용된 Origins: {origins}")

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"]
)

print("모델 로딩 중...")
# 오픈소스 모델 사용 (라이센스 제한 없음)
model_name = "TinyLlama/TinyLlama-1.1B-Chat-v1.0"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,
    device_map="auto",
)
print("모델 로딩 완료!")

class ChatRequest(BaseModel):
    message: str
    history: list = []
    sessionId: str  # sessionId 추가

# 세션별 대화 기록을 저장할 딕셔너리
session_histories = {}

@app.post("/api/fastapi/chat")
async def chat(request: ChatRequest) -> Dict[str, str]:
    try:
        # 요청 로깅 강화
        logger.info(f"""
=== 새로운 채팅 요청 ===
시간: {datetime.now()}
세션 ID: {request.sessionId}
메시지: {request.message}
히스토리 길이: {len(request.history)}
요청 헤더: {request.headers if hasattr(request, 'headers') else 'No headers'}
======================
""")
        
        # 세션별 히스토리 관리
        if request.sessionId not in session_histories:
            logger.info(f"새로운 세션 시작: {request.sessionId}")
            session_histories[request.sessionId] = []
        
        # 시스템 프롬프트
        system_prompt = """
            You are the AI customer support agent for the Haru app. Your name is Luffy.

            🧭 Haru Chatbot Operating Philosophy

            Haru is a "market where hobbies become talents."
            It provides a space where anyone can share, trade, and experience their hobbies or skills with others.
            We aim to provide friendly and professional service that enriches users' experiences and encourages exploration.

            🤖 Luffy's Customer Support Guidelines

            Provide accurate and concise answers.

            Ensure consistency by considering the conversation context.

            Offer step-by-step guidance when explaining features.

            Redirect inquiries related to payment, personal data, or security to dedicated support.

            If unsure, honestly say "I'm not sure" instead of guessing.

            Respond with empathy and sincerity when users face difficulties.

            📱 Haru Core Features Summary

            Hobby-based Talent Market: Anyone can list products based on their hobbies and trade with other users.

            Matching & Communication: Real-time connection through chat and notifications.

            Location-based Discovery: Filter products based on the user's current location.

            Community Features: Enable lasting user relationships via small groups and competitions.

            🗺️ Main Menu Navigation

            🏠 Main Navigation

            Main Page (/): Recommended and latest products

            Bottom Menu:

            Market, Chat, Notification, Location, My Page

            🔐 Account Management

            Login (/login), Sign Up (/signup), Password Recovery & Update

            Account Deletion (/delete-account)

            🛍️ Talent Marketplace

            Register Product (/product/register)

            Product List and Filtering

            Product Details (/product-details)

            Favorite Products (/mypage → Products of Interest)

            💬 Chat System

            1:1 Chat Room (/chat/:email)

            AI Chatbot (/servicechat)

            Notifications sent during chat, application, and location sharing

            📍 Location-Based Features

            Set My Location (/my-location)

            Filter by Distance ("Nearby" feature)

            💳 Payments & Transactions

            Card Registration and Management (/ocr-upload, /registered-card)

            View Payment and Transaction History

            Share location between seller and buyer

            🔔 Notifications & Customer Center

            Notification Center (/notification)

            Customer Center (/cs-list)

            1:1 Inquiry (/inquiry-history)

            ❓ Frequently Asked Questions (FAQ)

            Q. How do I register a product?
            → Go to [Market] tab > Click the + button 'Register Product' and fill out the form.

            Q. How do I chat with another user?
            → Click the 'Apply' button on the product detail page or go to the chat page.

            Q. How do I set my location?
            → Tap the 'Location' icon in the bottom menu > Set My Location

            Q. I’m not receiving chat notifications.
            → Check in [Settings] > Notification Preferences.

            Q. How do I make a payment?
            → Currently not implemented.

            Q. Where can I see my listed products?
            → [My Page] → Product Management

            💡 Feature Suggestions or Bug Reports

            If you have suggestions like “I wish this feature existed,”
            👉 Customer Center > 1:1 Inquiry
            👉 or AI Chatbot to leave your feedback!
            We’re always listening.

            🎯 What the Chatbot Does Best

            Explains how to use the app step-by-step

            Answers feature-related inquiries

            Provides guidance and empathy for bugs/errors

            Collects and summarizes user feedback
    
            """

        # 대화 히스토리를 포함한 프롬프트 구성
        full_prompt = f"<system>{system_prompt}</system>\n"

        print(f"\n=== 세션 {request.sessionId}의 대화 히스토리 시작 ===")
        print(f"히스토리 메시지 수: {len(request.history)}")

        # 이전 대화 내용 추가 (최근 4개 메시지만 사용)
        for i, msg in enumerate(request.history[-4:]):
            print(f"\n메시지 {i+1}:")
            print(f"사용자: {msg['user']}")
            if "assistant" in msg:
                print(f"어시스턴트: {msg['assistant']}")
            full_prompt += f"<user>{msg['user']}</user>\n"
            if "assistant" in msg:
                full_prompt += f"<assistant>{msg['assistant']}</assistant>\n"

        # 현재 메시지 추가
        print(f"\n현재 메시지: {request.message}")
        full_prompt += f"<user>{request.message}</user>\n<assistant>"

        print("\n=== 최종 프롬프트 ===")
        print(full_prompt)
        print("=====================\n")

        # 입력 메시지 토큰화
        inputs = tokenizer(full_prompt, return_tensors="pt").to(model.device)

        # 생성 파라미터 설정
        generation_config = {
            "max_length": 2048,
            "temperature": 0.7,
            "top_p": 0.95,
            "repetition_penalty": 1.15,
            "do_sample": True
        }

        # 🔹 비동기 실행으로 변경 (`asyncio.to_thread` 사용)
        outputs = await asyncio.to_thread(
            model.generate,
            **inputs,
            **generation_config,
            pad_token_id=tokenizer.eos_token_id
        )

        # 응답 디코딩 및 프롬프트 제거
        response = tokenizer.decode(outputs[0], skip_special_tokens=True)
        response = response.split("<assistant>")[-1].strip()
        # \n은 유지하고 다른 태그만 제거
        clean_response = re.sub(r"</?(?!br\b)[a-zA-Z0-9]+>", "", response).strip()
        print(clean_response)

        # 응답 로깅
        logger.info(f"""
=== 응답 생성 완료 ===
세션 ID: {request.sessionId}
응답 길이: {len(clean_response)}
======================
""")
        return {"response": clean_response}

    except Exception as e:
        error_msg = f"에러 발생 - 세션 ID: {request.sessionId}, 에러: {str(e)}"
        logger.error(error_msg)
        raise HTTPException(status_code=500, detail=error_msg)


if __name__ == "__main__":
    logger.info("=== LLaMA 서버 시작 ===")
    logger.info(f"모델: {model_name}")
    # 호스트를 0.0.0.0으로 설정하여 외부 접근 허용
    logger.info("서버 시작: host=0.0.0.0, port=8001")
    uvicorn.run(
        app, 
        host="0.0.0.0",  # 모든 IP에서 접근 가능하도록 설정
        port=8001,
        log_level="info"
    )