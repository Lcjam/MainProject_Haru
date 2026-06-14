from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
from fastapi.middleware.cors import CORSMiddleware
from llama_cpp import Llama
import logging
import os
from typing import List, Dict

# 기본 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 모델 경로는 환경변수로 외부화(소스 하드코딩 금지). 미설정 시 명확히 실패.
model_path = os.getenv("DEEPSEEK_MODEL_PATH")
if not model_path:
    raise RuntimeError("DEEPSEEK_MODEL_PATH 환경변수에 .gguf 모델 경로를 지정하세요.")
llm = Llama(
    model_path=model_path,
    n_ctx=2048,
    n_gpu_layers=-1,
    n_batch=512,
    n_threads=8
)

# 요청 모델 정의
class ChatRequest(BaseModel):
    message: str
    history: List[Dict[str, str]] = []

@app.post("/chat")
async def chat(request: ChatRequest):
    try:
        logger.info(f"받은 메시지: {request.message}")
        logger.info(f"대화 기록: {request.history}")
        
        # 대화 기록을 포함한 프롬프트 생성
        prompt = ""
        for conv in request.history:
            prompt += f"Human: {conv['user']}\nAssistant: {conv['assistant']}\n\n"
        prompt += f"Human: {request.message}\n\nAssistant:"
        
        response = llm.create_completion(
            prompt=prompt,
            temperature=0.7,
            max_tokens=2048,
            stop=["Human:", "\n\nHuman:"]
        )
        
        logger.info(f"Raw 응답: {response}")
        return {"response": response['choices'][0]['text'].strip()}
        
    except Exception as e:
        logger.error(f"에러 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)