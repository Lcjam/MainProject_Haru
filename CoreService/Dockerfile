FROM openjdk:17-jdk-slim

# 필요한 패키지 설치 (Redis 포함)
RUN apt update && apt install -y curl redis-server procps net-tools

WORKDIR /app

COPY build/libs/*.jar app.jar
COPY src/main/resources/application.properties application.properties

# Redis 설정 디렉토리 생성
RUN mkdir -p /etc/redis
RUN mkdir -p /data/redis

# 기본 Redis 설정 생성 - 모든 인터페이스에서 접속 허용
RUN echo "bind 0.0.0.0\nport 6379\nprotected-mode no\ndaemonize yes\nsupervised auto\ndir /data/redis\nappendonly yes\nloglevel notice\nlogfile /var/log/redis/redis-server.log" > /etc/redis/redis.conf

# 보안 강화를 위해 로컬 접속만 허용하려면 다음과 같이 변경:
# RUN echo "bind 127.0.0.1\nport 6379\nprotected-mode yes\ndaemonize yes\nsupervised auto\ndir /data/redis\nappendonly yes\nloglevel notice\nlogfile /var/log/redis/redis-server.log" > /etc/redis/redis.conf
# 이 경우 외부에서 Redis에 직접 접근할 수 없으며, CoreService 내부에서만 접근 가능합니다.
# GitHub Actions 워크플로우에서도 -p 6379:6379 포트 매핑을 제거해야 합니다.

# Redis 로그 디렉토리 생성
RUN mkdir -p /var/log/redis && chmod 777 /var/log/redis

ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_REDIS_HOST=localhost
ENV SPRING_REDIS_PORT=6379

# Redis 데이터 디렉토리를 볼륨으로 설정
VOLUME ["/data/redis", "/var/log/redis"]

EXPOSE 8081
# 로컬 접속만 허용할 경우 아래 EXPOSE 명령을 제거하거나 주석 처리하세요
EXPOSE 6379

# 시작 스크립트 생성
RUN echo '#!/bin/bash\n\
echo "Redis 설정 확인:"\n\
cat /etc/redis/redis.conf\n\
\n\
echo "Redis 서버 시작 중..."\n\
service redis-server stop || true\n\
redis-server /etc/redis/redis.conf\n\
\n\
# Redis 프로세스 확인\n\
echo "Redis 프로세스 확인:"\n\
ps aux | grep redis\n\
\n\
# 네트워크 연결 확인\n\
echo "네트워크 연결 확인:"\n\
netstat -tulpn | grep 6379 || echo "포트 6379가 열려있지 않습니다."\n\
\n\
# Redis가 준비될 때까지 대기\n\
echo "Redis 서버 응답 대기 중..."\n\
max_attempts=30\n\
attempt=0\n\
while [ $attempt -lt $max_attempts ]; do\n\
  if redis-cli -h 127.0.0.1 -p 6379 ping | grep -q "PONG"; then\n\
    echo "Redis 서버 준비 완료!"\n\
    break\n\
  fi\n\
  attempt=$((attempt+1))\n\
  echo "Redis 서버 준비 대기 중... 시도 $attempt/$max_attempts"\n\
  sleep 2\n\
done\n\
\n\
if [ $attempt -eq $max_attempts ]; then\n\
  echo "Redis 서버 시작 실패! 로그 확인:"\n\
  cat /var/log/redis/redis-server.log\n\
  echo "Spring Boot 애플리케이션을 시작합니다만, Redis 연결에 문제가 있을 수 있습니다."\n\
fi\n\
\n\
# Spring Boot 애플리케이션 실행\n\
java -jar /app/app.jar\n\
' > /app/start.sh

RUN chmod +x /app/start.sh

# 시작 스크립트 실행
CMD ["/app/start.sh"] 