# Subagents

136개의 역할별 서브에이전트. codex `.codex/agents/*.toml`에서 마이그레이션됨.
`Agent` 툴의 `subagent_type`으로 선택해 사용한다. 의존 없는 호출은 한 응답에 묶어 병렬 실행 가능.

**모델 매핑** (codex reasoning effort 기준): high→`opus`, medium→`sonnet`.
**read-only 에이전트**는 `Read, Grep, Glob, Bash, WebSearch, WebFetch`만 가짐(파일 수정 불가). 나머지는 전체 툴 상속.

## 01. Core Development
애플리케이션 아키텍처, 크로스레이어 구현, UI, 프로토콜별 개발.

- `api-designer` — 구현 전 API 계약 설계/검토
- `backend-developer` — 경로가 명확해진 백엔드 변경 담당
- `code-mapper` — 동작 뒤의 파일·진입점·상태 전이 추적
- `electron-pro` — Electron main/renderer, 패키징, 데스크톱 통합
- `frontend-developer` — 이슈 파악 후 프론트엔드 변경 담당
- `fullstack-developer` — 백엔드+프론트 경계의 end-to-end 기능
- `graphql-architect` — GraphQL 스키마·리졸버·페더레이션
- `microservices-architect` — 서비스 경계와 분산 계약 검토
- `mobile-developer` — 모바일 전용 플로우 구현/디버그
- `ui-designer` — 구현 가능한 구체적 UI 방향 산출
- `ui-fixer` — 재현·증거 확보 후 최소 UI 수정
- `websocket-engineer` — 실시간 연결·프로토콜·이벤트 전달

## 02. Language Specialists
생태계별 구현·디버깅·아키텍처 가이드.

- `angular-architect`, `cpp-pro`, `csharp-developer`, `django-developer`, `dotnet-core-expert`, `dotnet-framework-4.8-expert`, `elixir-expert`, `erlang-expert`, `flutter-expert`, `golang-pro`, `java-architect`, `javascript-pro`, `kotlin-specialist`, `laravel-specialist`, `nextjs-developer`, `php-pro`, `powershell-5.1-expert`, `powershell-7-expert`, `python-pro`, `rails-expert`, `react-specialist`, `rust-engineer`, `sql-pro`, `spring-boot-engineer`, `swift-expert`, `typescript-pro`, `vue-expert`

> 이 프로젝트 핵심: `spring-boot-engineer`, `typescript-pro`, `react-specialist`, `javascript-pro`, `sql-pro`

## 03. Infrastructure
배포, 컨테이너, 오케스트레이션, IaC.

- `azure-infra-engineer`, `cloud-architect`, `database-administrator`, `deployment-engineer`, `devops-engineer`, `devops-incident-responder`, `docker-expert`, `incident-responder`, `kubernetes-specialist`, `network-engineer`, `platform-engineer`, `security-engineer`, `sre-engineer`, `terraform-engineer`, `terragrunt-expert`, `windows-infra-admin`

> 이 프로젝트 핵심: `docker-expert`, `deployment-engineer`

## 04. Quality & Security
read-heavy 리뷰·검증 에이전트.

- `accessibility-tester`, `ad-security-reviewer`, `architect-reviewer`, `chaos-engineer`, `code-reviewer`, `browser-debugger`, `compliance-auditor`, `debugger`, `error-detective`, `penetration-tester`, `performance-engineer`, `powershell-security-hardening`, `qa-expert`, `reviewer`, `security-auditor`, `test-automator`

> `browser-debugger`는 codex에서 MCP 서버(chrome_devtools)에 의존 — 해당 에이전트 본문의 마이그레이션 노트 참고.

## 05. Data & AI
데이터 파이프라인, LLM 통합, DB 동작.

- `ai-engineer`, `data-analyst`, `data-engineer`, `data-scientist`, `database-optimizer`, `llm-architect`, `machine-learning-engineer`, `ml-engineer`, `mlops-engineer`, `nlp-engineer`, `postgres-pro`, `prompt-engineer`

## 06. Developer Experience
빌드, 툴링, 문서, MCP 통합, 리팩터.

- `build-engineer`, `cli-developer`, `dependency-manager`, `documentation-engineer`, `dx-optimizer`, `git-workflow-manager`, `legacy-modernizer`, `mcp-developer`, `powershell-module-architect`, `powershell-ui-architect`, `refactoring-specialist`, `slack-expert`, `tooling-engineer`

## 07. Specialized Domains
구현/검증 경계가 명확한 도메인 에이전트.

- `api-documenter`, `blockchain-developer`, `embedded-systems`, `fintech-engineer`, `game-developer`, `iot-engineer`, `m365-admin`, `mobile-app-developer`, `payment-integration`, `quant-analyst`, `risk-manager`, `seo-specialist`

## 08. Business & Product
요구사항, UX, 엔지니어링 인접 작성 업무.

- `business-analyst`, `content-marketer`, `customer-success-manager`, `legal-advisor`, `product-manager`, `project-manager`, `sales-engineer`, `scrum-master`, `technical-writer`, `ux-researcher`, `wordpress-master`

## 09. Meta & Orchestration
멀티에이전트 워크플로 계획·조정.

- `agent-installer`, `agent-organizer`, `context-manager`, `error-coordinator`, `it-ops-orchestrator`, `knowledge-synthesizer`, `multi-agent-coordinator`, `performance-monitor`, `task-distributor`, `workflow-orchestrator`

> 병렬 작업 분배 시 `agent-organizer` / `multi-agent-coordinator` / `task-distributor` 참고.

## 10. Research & Analysis
검색·검증·비교·종합 중심 read-heavy 에이전트.

- `competitive-analyst`, `data-researcher`, `docs-researcher`, `market-researcher`, `research-analyst`, `search-specialist`, `trend-analyst`

> `docs-researcher`는 codex에서 MCP 서버(openaiDeveloperDocs)에 의존 — 해당 에이전트 본문의 마이그레이션 노트 참고.
