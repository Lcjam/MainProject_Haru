# 문서

| 문서 | 내용 |
|---|---|
| [아키텍처](architecture.md) | 서비스 구성, 요청 흐름, 인증·인가, 실시간 메시징, 거래 정합성, 알려진 제약 |
| [API 레퍼런스](api.md) | 서비스별 REST 엔드포인트, STOMP 목적지, 응답 형식 |
| [데이터베이스](database.md) | 스키마 기준, 도메인별 테이블, 마이그레이션 러너, 시드 데이터 |
| [개발 가이드](development.md) | Docker Compose·로컬 실행, 환경변수, 테스트, CI, 트러블슈팅 |

## 이력

| 문서 | 내용 |
|---|---|
| [개선 이력](history/changelog.md) | 기준선 이후 리팩토링·보안·DB·테스트 작업을 시간순으로 정리 |
| [백엔드 하드닝 기록](history/backend-hardening.md) | 인증·인가·정합성 보강의 변경 전후 비교 |
| [트러블슈팅: 공개 경로 불일치](history/troubleshooting-gateway-whitelist.md) | 게이트웨이와 CoreService 공개 경로가 어긋나 생긴 장애의 원인 분석 |

## 문서 관리 규칙

- 공개 문서는 GitHub에서 바로 읽히도록 **Markdown**으로 씁니다.
- 저장소의 `.gitignore`는 `*.md`를 기본으로 제외합니다. 새 공개 문서를 추가하면 `.gitignore`의 "공개 문서" 목록에 **파일 경로를 한 줄** 추가해야 합니다.
- 포트폴리오·면접 준비 자료 같은 개인 문서는 `docs/private/`에 둡니다. 이 디렉터리는 원격 저장소에 올라가지 않습니다.
