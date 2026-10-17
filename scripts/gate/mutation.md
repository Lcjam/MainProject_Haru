# 변경 줄 뮤테이션 게이트

저장소 루트에서 실행한다. Python 표준 라이브러리와 기존 `diff_coverage.py`의 변경 줄 수집기를 사용한다.

```bash
python3 scripts/gate/mutation.py --self-test
python3 scripts/gate/mutation.py --base origin/main --worktree --threshold 60
# CI의 깨끗한 checkout: 커밋된 HEAD만 비교
python3 scripts/gate/mutation.py --base origin/main --threshold 60
```

`base`와 HEAD의 merge-base를 기준으로 비교한다. `--worktree`는 staged·unstaged·새 Java 파일을 포함한다. 기본 모드는 커밋된 변경을 검사하며 대상 파일이 HEAD와 다르면 실패한다. CI에서는 테스트·설정도 HEAD와 일치하는 깨끗한 checkout을 쓴다.

세 서비스만 실행 대상으로 삼는다. Java 파일의 package 선언과 파일명으로 FQCN을 만들고, 내부 클래스도 PIT 대상에 포함한다. `targetTests=['*']`로 다른 패키지의 HTTP/계약 테스트까지 허용하며 JUnit 태그 `mysql`, `redis`만 제외한다. `check`에는 PIT를 연결하지 않는다.

## 대상에서 제외하는 파일

- 파일의 대표 타입이 interface 또는 record: 계약/운반 타입 중심이며 생성 코드와 기본 메서드의 업무 로직은 이 게이트가 평가하지 않는다.
- `dto` 패키지(대소문자 무관) 또는 `*DTO`: 데이터 운반/직렬화 코드의 생성 뮤턴트가 업무 로직 점수를 압도하지 않게 한다.
- `*Application`: 부트스트랩 진입점.
- `config` 패키지 또는 `@Configuration`, `@SpringBootConfiguration`, `@TestConfiguration`이 있는 클래스: 환경/빈 조립은 별도 기동·계약 검사 대상이다.
- `package-info`, `module-info`, 세 서비스 밖의 모듈: 실행 가능한 서비스 클래스가 아니다. 특히 `sql`은 독립 마이그레이션 도구라 이번 범위 밖이다.

주석과 문자열은 제거한 뒤 타입/애너테이션을 찾는다. 서비스 안에 중첩 record가 있다는 이유로 서비스 전체를 제외하지 않는다. 제외 사유는 콘솔에 출력한다. 제외 타입에 업무 로직을 넣으면 이 검사로 보호되지 않으므로 일반 테스트와 diff coverage를 함께 유지한다.

## 설정과 점수

세 서비스 공통: Gradle PIT 플러그인 `1.19.0`, PIT `1.22.1`, `pitest-junit5-plugin` `1.2.3`. Spring Boot BOM이 관리하는 `junit-platform-launcher`를 명시하고 플러그인의 launcher 자동 감지를 끈다. 자동 감지의 detached configuration은 Boot의 버전 관리를 반영하지 않아 launcher/engine 불일치를 만들었다.

`threads=2`, `mutators=['DEFAULTS']`, `timeoutFactor=2.0`, `timeoutConstInMillis=5000`, JUnit 기본 타임아웃 `60s`. `DEFAULTS`는 분기·반환·호출 변이를 검사하면서 CI의 5분 예산을 지키기 위한 선택이다. `STRONGER`는 더 많은 변이와 동등 뮤턴트 검토 비용을 만들며 이번에는 실행하지 않았다. 전체 테스트를 허용하고 PIT가 런타임 커버리지로 관련 테스트를 선택하게 한다. 테스트 이름 추정이나 incremental history는 쓰지 않는다.

직접 실행할 때도 대상 속성이 필수다. 빈 값·패키지 와일드카드는 실패하고 명시적인 클래스명만 받는다.

```bash
cd CoreService
./gradlew pitest -PpitTargetClasses=com.example.demo.service.Market.ProductService --console=plain
```

리포트는 각 서비스의 `build/reports/pitest/mutations.xml`, `index.html`에 덮어쓴다. 타임스탬프 디렉터리는 만들지 않는다. PIT는 매번 실행하며 스크립트는 실행 전 이전 XML을 제거한다. 실패·보고서 누락·잘못된 XML은 통과로 취급하지 않는다.

집계는 변경 파일의 변경된 새 줄 번호와 일치하는 뮤턴트에 한정한다. 내부 클래스는 바깥 소스 파일의 줄에 합산한다.

```text
killed = KILLED + TIMED_OUT
score = killed / (killed + SURVIVED + NO_COVERAGE) * 100
```

서비스별 수를 출력하고 전체 합계 점수에 임계값을 적용한다. `NON_VIABLE`은 실행 불가능한 변이로 표시 후 제외한다. 변경 줄의 나머지 오류 상태는 실패한다. 생존/미커버 뮤턴트는 `파일:줄 mutator 설명 [상태]`로 출력한다. 대상이 없거나 성공한 PIT 실행에서 변경 줄에 집계할 변이가 없으면 그 사실을 표시하고 통과한다. 주석·선언만 변경해도 대상 클래스에는 PIT가 실행될 수 있지만 변이가 없는 줄을 인위적으로 실패시키지 않는다.

삭제된 줄에는 새 바이트코드 변이가 없으므로 이 점수는 삭제 전 동작 보존을 보장하지 않는다. 동일한 줄에 여러 표현식이 있으면 줄 전체가 집계된다. 동등 뮤턴트와 분기 입력 부족은 실제 단언 누락과 함께 생존으로 나타날 수 있다.

## CI 연결

팀장이 `gate.sh`/`ci.yml`에 위 Python 명령을 별도 단계로 연결한다. `check` 이후 실행하고 XML·HTML을 아티팩트로 보존한다. shallow checkout이면 먼저 base ref와 merge-base가 존재하도록 fetch한다. 동일 작업 트리의 빌드 산출물을 동시에 생성하지 않도록 기존 gate 잠금 안에서 호출한다. 게이트 파일 변경 승인 절차는 기존 정책을 따른다.

PIT는 Java 바이트코드에서 성숙한 Gradle/JUnit 통합과 실제 테스트 재실행을 제공한다. JaCoCo/diff coverage는 빠르게 실행 여부를 검사하지만 단언 유효성은 확인하지 못한다. 수동 소스 변이는 새 운영 코드가 필요하고 변경마다 설계·복원 부담이 있어 자동 게이트의 대안으로 채택하지 않았다. 변경 줄 점수는 기존 부채로 새 PR을 막지 않는 대신 미변경 줄의 약한 단언을 강제하지 않는다. 클래스 전체 결과는 HTML/XML에 그대로 남긴다.
