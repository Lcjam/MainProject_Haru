#!/bin/sh

# upstream 서버들이 준비될 때까지 대기
echo "Waiting for upstream servers..."
sleep 10

# nginx 설정 테스트
nginx -t

# nginx 시작
exec nginx -g 'daemon off;' 