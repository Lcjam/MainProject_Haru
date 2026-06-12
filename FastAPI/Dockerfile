FROM python:3.9-slim

WORKDIR /app

# 시스템 패키지 설치 레이어
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# requirements.txt 복사 및 설치 레이어
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir -r requirements.txt

# 추가 패키지 설치 레이어
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-cache-dir \
    transformers \
    torch \
    sentencepiece \
    accelerate>=0.26.0

# 소스 코드 복사 레이어
COPY . .

EXPOSE 8001

CMD ["uvicorn", "llamaServer:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "1"]
