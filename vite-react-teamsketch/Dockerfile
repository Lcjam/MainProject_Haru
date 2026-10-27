# Node 빌드 스테이지
FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# 값을 넘긴 경우에만 빌드 환경변수로 노출되어 .env.production 값을 덮어쓴다 (미지정 시 .env.production 의 localhost 값).
ARG VITE_API_URL
ARG VITE_WS_URL
RUN npm run build

# Nginx 서비스 스테이지
FROM nginx:alpine

# nginx 설정 선택 (nginx/nginx.${ENV}.conf). 현재는 compose 하나뿐이다.
ARG ENV=compose

# nginx 설정 파일 복사 (환경별 설정)
COPY nginx/nginx.${ENV}.conf /etc/nginx/nginx.conf

# 빌드된 파일들을 Nginx로 복사
COPY --from=build /app/dist /usr/share/nginx/html/

# 정적 파일 권한 설정
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

EXPOSE 80

# 시작 스크립트 추가
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"] 

