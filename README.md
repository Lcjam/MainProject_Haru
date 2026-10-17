# Haru

Haru는 GateWay, CoreService, AssistService, Vite/React 프론트로 구성된 마이크로서비스 프로젝트다.

로컬 실행 절차와 포트, JDK 17·MySQL·Redis·Node.js 사전 조건, 환경변수 로드 위치는 [LOCAL_DEV.md](LOCAL_DEV.md)를 참고한다.

핵심 로컬 주소:

- 프론트: `http://localhost:3000`
- REST 게이트웨이: `http://localhost:8080/api`
- WebSocket: `ws://localhost:8080/ws`
- CoreService: `http://localhost:8081`
- AssistService: `http://localhost:8082`

FastAPI는 기본 실행 범위에 포함하지 않는다. DB 작업은 [`sql/README.md`](sql/README.md)의 R02 실행기(`init`, 기존 스키마의 `adopt`, 이후 `migrate`)를 사용한다. 선택적 `seed-local.sql` 적용도 SQL 가이드에서 안내한다.
