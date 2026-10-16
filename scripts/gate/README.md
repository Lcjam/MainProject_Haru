# 품질 게이트 (H01)

리팩토링 중에도 사람이 매번 확인하지 않아도 품질이 낮은 변경은 main에 들어가지 못하게 한다.
로컬 훅·팀원 완료 조건·CI가 **같은 스크립트**를 쓴다. 규칙을 바꾸려면 이 디렉터리를 바꾸고, 그 PR에는 승인 라벨이 필요하다.

## 한 줄 사용법

```bash
scripts/gate/install-hooks.sh          # 최초 1회: pre-commit / pre-push 활성화
scripts/gate/gate.sh worktree          # 커밋 전 작업 트리 검사(팀원 완료 조건)
scripts/gate/gate.sh push              # 커밋된 브랜치 검사(pre-push와 동일)
scripts/gate/gate.sh full              # 전 모듈 + 통합 레인 + E2E(도커 필요)
GATE_E2E=1 scripts/gate/gate.sh push   # 계약·compose 변경 시 로컬에서 E2E까지
```

## 층별로 막는 것

| 층 | 언제 | 막는 것 | 근거 |
|---|---|---|---|
| `policy.py` | pre-commit, pre-push, CI `Policy gate` | 추가된 `System.out`/`printStackTrace`/`console.log`, Core의 `"Bearer "` 직접 처리, Java 호스트 하드코딩, 테스트 `@Disabled`/`.only`/`.skip`, **테스트 선언 순감**, 기존 마이그레이션 수정·`schema.sql` 수정·버전 건너뛰기, 시크릿·비밀 파일, 1MiB 초과 파일, 충돌 표식, 줄 끝 공백 | 변경분만 보므로 기존 부채는 막지 않고 새 부채만 막는다 |
| 게이트 약화 감지 | 같음 | 게이트 파일·`build.gradle`·`settings.gradle`·Gradle wrapper·ArchUnit 규칙 파일 변경, 테스트 단언 순감, 조건부 스킵 추가, ArchUnit·ESLint 동결 항목 증가(교체 포함), 커버리지 하한 하락, `ignoreFailures`·`continue-on-error`·`-x test` | `GATE_APPROVED=1`(로컬) 또는 PR 라벨 `gate-change-approved`가 있어야 통과 |
| `./gradlew check` | pre-push(바뀐 서비스), CI | 단위·HTTP 테스트, ArchUnit 규칙(동결), JaCoCo 전체 하한(래칫), 테스트 타임아웃, 0건 실행 | 서비스별 `build.gradle` |
| 통합 레인 | CI `integration`, 로컬은 `HARU_TEST_*` 있을 때 | 실제 MySQL 8.4·Redis 7에서 매퍼 SQL·트랜잭션·Redis 왕복, sql 마이그레이션 실행기, 격리 스키마 잔존 | `/haru-verify` 참조 |
| `npm run verify` | pre-push(프론트 변경), CI | ESLint(동결), `tsc`, Vitest 커버리지 하한, 빌드 | `vite-react-teamsketch/package.json` |
| `diff_coverage.py` | pre-push, CI `Diff coverage` | **이번에 바꾼 프로덕션 줄의 80% 이상이 테스트로 실행돼야 함**, 테스트를 한 번도 로드하지 않은 변경 파일, 리포트 누락 | 단위+통합 리포트를 합쳐 판단 |
| `mutation.py` | pre-push(Java 변경), CI `Mutation (changed lines)` | **바꾼 줄에 PIT 변이를 넣었을 때 테스트가 60% 이상 잡아야 함** — 단언 없이 호출만 하는 테스트를 차단 | `scripts/gate/mutation.md` |
| `selftest/run.sh` | CI `Policy gate`, 게이트 파일 변경 시 로컬 | 나쁜 변경 36종을 임시 worktree에 주입해 게이트가 실제로 막는지(그리고 정상 변경은 통과하는지) 확인 — 게이트 자체의 회귀 시험 | 알려진 한계: `"Bear"+"er "` 상수 분할(G02) |
| `e2e.sh` | CI `E2E compose + Newman`, 로컬 `GATE_E2E=1` | 빈 DB에서 compose 전 스택 기동→가입→Newman 4폴더(공유 오류 계약 포함) 실패 0, 최소 검사 수 | 개발 스택·볼륨·`.env`와 격리 |
| gitleaks | CI | 변경 커밋의 시크릿 | |

## 실패했을 때

- 로그: `build/gate/<단계>.log` (CI는 각 잡 로그와 아티팩트)
- diff 커버리지 실패: 표시된 파일·줄을 실행하는 테스트를 추가한다. 리팩토링이라면 **바꾸기 전에** 현재 동작을 고정하는 테스트를 먼저 넣는다.
- 동결 규칙: 기존 위반을 고치면 동결 저장소가 줄어든다(좋음). 새 위반을 동결 파일에 추가하는 것은 게이트 약화로 취급된다.
- 우회 금지: `--no-verify`, 테스트 삭제·비활성화, 하한 하향은 정책 위반이다. 정말 필요하면 이유를 PR 본문에 적고, 저장소 소유자 또는 팀장이 검토한 뒤 `gate-change-approved` 라벨을 붙인다. 팀원 에이전트는 라벨을 붙이지 않는다. 라벨을 붙이거나 떼면 CI가 다시 판정한다.

## 알려진 한계

- 정책 검사는 diff를 보는 정적 규칙이다. 주석·문자열·템플릿 보간은 `policy.mask_source` lexer로 구분하지만 완전한 파서는 아니다. 새 우회가 발견되면 `selftest/run.sh`에 시나리오를 추가하고 고친다.
- `"Bear" + "er "`처럼 문자열 상수를 쪼개면 토큰 단일 소스 위반을 diff로 잡지 못한다(컴파일러가 상수를 합쳐 ArchUnit도 못 본다). 자가 시험에 `GAP`으로 남겨 둔다.
- `diff_coverage.py`를 게이트 밖에서 단독으로 실행하면 지난 실행의 리포트를 그대로 쓴다. 판정은 `gate.sh`(통합 레인을 건너뛰면 지난 통합 리포트를 지운다)나 CI(매번 새로 만든 아티팩트)를 기준으로 한다.
- 동결된 기존 부채와 낮은 프론트 전체 커버리지는 남아 있다. 하한은 래칫으로만 올린다.
- `mask_source`는 JS/TS 정규식 리터럴(`/\/*/`)을 모른다. 정규식 안에 `/*`·`//`가 그대로 있으면 뒤 코드를 주석으로 볼 수 있다(H01.9 A7·A8·A11). 실제 저장소 테스트 94개 전수 비교에서는 차이가 0건이었다.
