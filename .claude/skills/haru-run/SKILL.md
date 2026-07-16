---
name: haru-run
description: Haru 전체 로컬 스택(GateWay 8080 / CoreService 8081 / AssistService 8082 / React 3000)을 기동하고 헬스체크한다. 서비스 실행/구동/띄우기/health check 요청 시 사용.
---

# Haru 로컬 스택 기동 & 헬스체크

루트는 `/Users/jamie/Desktop/Project/MainProject_Haru`. 모든 명령은 절대경로 기준.

## 사전 조건 (없으면 먼저 해결)
- **JDK 17 toolchain**: `~/.gradle/gradle.properties`에 `org.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` 등록돼 있어야 함. 없으면 `brew install openjdk@17` 후 등록. (build.gradle이 Java 17 고정, 머신엔 21만 있어 미등록 시 빌드 실패)
- **MySQL** `haru_db` 기동(3306), **Redis** 기동(6379). `redis-cli ping`→PONG.
- 프론트 `vite-react-teamsketch/node_modules` 설치(`npm install`).
- `CoreService/.env`, `AssistService/.env` 존재(DB 자격증명).

## 기동 (백그라운드, 직접 제어용)
`run-local.sh`는 프론트를 포그라운드로 잡으므로, 헬스체크하려면 각각 백그라운드로 띄운다:
```bash
cd /Users/jamie/Desktop/Project/MainProject_Haru && mkdir -p logs
( cd GateWay && ./gradlew bootRun --console=plain ) > logs/gateway.log 2>&1 &
( cd CoreService && ./gradlew bootRun --console=plain ) > logs/core.log 2>&1 &
( cd AssistService && ./gradlew bootRun --console=plain ) > logs/assist.log 2>&1 &
( cd vite-react-teamsketch && npm run dev ) > logs/front.log 2>&1 &
```
Spring 서비스는 기동에 수십 초. 포트 LISTEN 폴링:
```bash
for p in 8080 8081 8082 3000; do lsof -nP -iTCP:$p -sTCP:LISTEN 2>/dev/null | grep -q LISTEN && echo "$p UP" || echo "$p ..."; done
```

## 헬스체크
```bash
# 게이트웨이→Core→DB 왕복 (정상=400 JSON: 잘못된 이메일 또는 비밀번호)
curl -4 -s -X POST -H "Content-Type: application/json" -d '{}' http://127.0.0.1:8080/api/core/auth/login
# 보호 라우트 (정상=401, JWT 필터)
curl -4 -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/core/
# 프론트 (정상=200, id="root")
curl -4 -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:3000/
```
게이트웨이 로그에 `현재 활성화된 프로필: dev`, `Core URI: http://localhost:8081`가 보이면 라우팅 정상.

## 종료
```bash
lsof -ti :8080 -ti :8081 -ti :8082 -ti :3000 | xargs kill -9 2>/dev/null
```
Redis/MySQL은 시스템 데몬이라 보통 그대로 둔다.

## 트러블슈팅
- 기동 실패 시 `logs/<svc>.log` tail. `Caused by` / `BUILD FAILED` 검색.
- 포트 충돌: `lsof -ti :<port> | xargs kill -9`.
- 로그 끝 `bootRun FAILED`는 kill로 종료해서 나는 정상 신호(DB 오류 아님).
