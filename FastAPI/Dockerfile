FROM python:3.9-slim

WORKDIR /app

# 시스템 패키지 설치 레이어 (llama-cpp-python 빌드에 cmake 필요)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    curl \
    && rm -rf /var/lib/apt/lists/*

# requirements.txt 복사 및 설치 레이어 (llama-cpp-python 포함)
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt

# 소스 코드 복사 레이어
# 모델(.gguf)은 이미지에 포함하지 않는다. LLAMA_MODEL_PATH 로 마운트/지정할 것.
COPY . .

EXPOSE 8001

CMD ["uvicorn", "llamaServer:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "1"]
