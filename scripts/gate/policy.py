#!/usr/bin/env python3
"""Haru diff policy gate.

변경분(diff)만 보고 "들어오면 안 되는 것"을 막는다. 빌드·테스트가 못 잡는 규약 위반,
테스트 약화, 마이그레이션 변조, 시크릿, 게이트 자체의 약화를 대상으로 한다.

사용:
  policy.py --staged                 # pre-commit: 스테이징된 변경
  policy.py --base origin/main       # pre-push / CI: merge-base(base, HEAD)..HEAD
  policy.py --base origin/main --worktree   # 커밋 전 작업 트리(추적 + 미추적 파일)

게이트 약화(보호 파일 수정, 동결 저장소 증가, 커버리지 하한 하락 등)는 기본 실패다.
GATE_APPROVED=1 (CI에서는 PR 라벨 gate-change-approved)일 때만 경고로 낮춘다.
표준 라이브러리만 사용한다.
"""
from __future__ import annotations

import argparse
import fnmatch
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True,
                           check=True).stdout.strip())

MAX_FILE_BYTES = 1024 * 1024
LARGE_FILE_ALLOW = {"vite-react-teamsketch/package-lock.json", "api-tests/haru.postman_collection.json"}

# 게이트·하네스 자체. 바뀌면 승인 필요.
PROTECTED_GLOBS = [
    ".github/workflows/*",
    "scripts/gate/*",
    "scripts/gate/**",
    ".githooks/*",
    ".claude/settings.json",
    "*/src/test/resources/archunit.properties",
    "*/src/test/java/*/architecture/*",
    "*/build.gradle",
    "*/settings.gradle",
    "*/gradle.properties",
    "*/gradle/wrapper/*",
    "*/gradle/*.gradle",
    "vite-react-teamsketch/eslint.config.js",
    "vite-react-teamsketch/vitest.config.ts",
]
# 동결 저장소: 위반 목록이 늘면 승인 필요.
ARCHUNIT_STORE_GLOB = "*/src/test/resources/archunit_store/*"
ESLINT_SUPPRESSIONS = "vite-react-teamsketch/eslint-suppressions.json"
WEAKENING_LINES = re.compile(r"ignoreFailures\s*=\s*true|continue-on-error\s*:\s*true|-x\s+(test|check)\b|"
                             r"--no-verify|\|\|\s*true\b")

JAVA_MAIN = re.compile(r"^[^/]+/src/main/java/.+\.java$")
JAVA_TEST = re.compile(r"^[^/]+/src/test/java/.+\.java$")
FRONT_SRC = re.compile(r"^vite-react-teamsketch/src/.+\.(ts|tsx|js|jsx)$")
FRONT_TEST = re.compile(r"(\.test\.|\.spec\.|/test/|/__tests__/)")

STDOUT_JAVA = re.compile(r"\bSystem\s*\.\s*(out|err)\b|\.\s*printStackTrace\s*\(")
CONSOLE_TS = re.compile(r"\bconsole\s*\.\s*(?!warn\b|error\b)\w+\s*\(|\bconsole\s*\[")
TEST_DISABLE = re.compile(r"@(?:[\w.]+\.)?(?:Disabled|Ignore)\b|\b(it|describe|test)\.(only|skip|todo)\s*\(|\bx(it|describe|test)\s*\(|"
                          r"\bf(it|describe)\s*\(")
TEST_ASSUME = re.compile(r"\bassume(True|False|That)?\s*\(|@(?:[\w.]+\.)?(Enabled|Disabled)(If|On|For)\w*")
JAVA_TEST_DECL = re.compile(r"@(?:[\w.]+\.)?(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b")
TS_TEST_DECL = re.compile(r"^\s*(it|test)(\.each\([^)]*\))?\s*\(", re.M)
CONFLICT = re.compile(r"^\s*(<<<<<<<( |$)|>>>>>>>( |$)|=======$)")
BEARER = re.compile(r"[\"']Bearer ")
LOCALHOST_LITERAL = re.compile(r"[\"'][^\"']*(localhost|127\.0\.0\.1)[^\"']*[\"']")

SECRET_PATTERNS = [
    ("private-key", re.compile(r"-----BEGIN ([A-Z]+ )?PRIVATE KEY-----")),
    ("aws-key", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("github-token", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,}\b")),
    ("openai-like-key", re.compile(r"\bsk-[A-Za-z0-9_-]{24,}\b")),
    ("slack-token", re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}\b")),
    ("jwt", re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
]
CONFIG_SECRET = re.compile(
    r"(?i)^\s*[\w.\-]*(password|passwd|secret|api[_-]?key|private[_-]?key|access[_-]?key|token)[\w.\-]*\s*[:=]\s*"
    r"[\"']?(?P<val>[^\s\"'#]+)")
CONFIG_FILE = re.compile(r"\.(properties|ya?ml|env|conf|ini|toml)$|(^|/)\.env[^/]*$")
SECRET_PLACEHOLDER = re.compile(r"^(\$\{.*|replace-with.*|changeme|example.*|<.*>|\*+|xxx+|dummy.*|test.*|null|none|"
                                r"true|false|\d{1,7})$", re.I)
SECRET_FILE_NAMES = re.compile(r"(^|/)(\.env(\.[\w.-]+)?|[^/]+\.env|.+\.(pem|p12|pfx|jks|keystore|key))$")
SECRET_FILE_ALLOW = re.compile(r"\.example$|\.sample$")

# 실행 파일. core.fileMode=false 저장소에서는 chmod가 커밋에 반영되지 않아 CI·새 클론에서 Permission denied,
# git은 실행 권한 없는 훅을 조용히 건너뛴다(H01 PR #10 CI에서 실제 발생)
EXECUTABLE_GLOBS = [".githooks/*", "scripts/gate/*.sh", "scripts/gate/*.py", "scripts/gate/selftest/*.sh",
                    "*/gradlew", "*/docker-entrypoint.sh"]
MIGRATION = re.compile(r"^sql/migrations/(?P<name>[^/]+)$")
MIGRATION_NESTED = re.compile(r"^sql/migrations/.+/")
# 단언 계열 호출. 테스트 선언 수는 그대로인데 단언만 지워 껍데기 테스트를 만드는 것을 잡는다.
ASSERTION = re.compile(r"\b(assert\w*|verify\w*|then|expect|andExpect|fail|assume\w*)\s*\(|\.should\w*\(|\.to(Be|Equal|Have|Match|Throw|Contain)\w*\(")
MIGRATION_NAME = re.compile(r"^V(?P<v>\d+)__[a-z0-9_]+\.sql$")


@dataclass
class FileChange:
    path: str
    status: str  # A M D R
    old_path: str | None
    added: list[tuple[int, str]] = field(default_factory=list)
    removed: list[str] = field(default_factory=list)


@dataclass
class Finding:
    rule: str
    path: str
    line: int | None
    msg: str
    approvable: bool = False


def git(*args: str, check: bool = True) -> str:
    r = subprocess.run(["git", "-c", "core.quotePath=false", *args], cwd=ROOT, capture_output=True, text=True)
    if check and r.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} failed: {r.stderr.strip()}")
    return r.stdout


class ChangeSet:
    def __init__(self, mode: str, base: str | None, head: str = "HEAD"):
        self.mode = mode
        self.head = head
        if mode == "staged":
            self.base_ref = "HEAD"
            diff_args = ["diff", "--cached"]
        else:
            mb = git("merge-base", base, head).strip()
            self.base_ref = mb
            diff_args = ["diff", mb] if mode == "worktree" else ["diff", mb, head]
        raw = git(*diff_args, "-M", "--no-color", "--no-ext-diff", "-U0", "--diff-filter=ACDMRT")
        self.files = self._parse(raw)
        # 패치 본문이 없는 변경(순수 이름변경, 바이너리, 모드 변경)은 위 파서에 안 잡힌다 → name-status로 보충
        ns = git(*diff_args, "-M", "--name-status", "-z", "--diff-filter=ACDMRT").split("\0")
        i = 0
        while i < len(ns) and ns[i]:
            st = ns[i][0]
            if st in "RC":
                old, new = ns[i + 1], ns[i + 2]
                i += 3
            else:
                old, new = ns[i + 1], ns[i + 1]
                i += 2
            key = old if st == "D" else new
            if key not in self.files:
                status = {"T": "M", "C": "A"}.get(st, st)
                self.files[key] = FileChange(key, status, old if st in "RDMT" else None)
        if mode == "worktree":
            for p in git("ls-files", "--others", "--exclude-standard").splitlines():
                fp = ROOT / p
                if not fp.is_file():
                    continue
                fc = FileChange(p, "A", None)
                try:
                    fc.added = list(enumerate(fp.read_text(encoding="utf-8").splitlines(), 1))
                except UnicodeDecodeError:
                    pass
                self.files[p] = fc

    @staticmethod
    def _parse(raw: str) -> dict[str, FileChange]:
        files: dict[str, FileChange] = {}
        cur: FileChange | None = None
        new_line = 0
        old_path = None
        for line in raw.splitlines():
            if line.startswith("diff --git "):
                cur = None
                old_path = None
                continue
            if line.startswith("rename from "):
                old_path = line[len("rename from "):]
                continue
            if line.startswith("--- "):
                src = line[4:]
                if src.endswith("\t"):  # 공백 포함 경로는 Git이 헤더 끝에 탭 1개를 붙인다(H01.7 N2)
                    src = src[:-1]
                if src != "/dev/null":
                    old_path = old_path or src[2:]
                continue
            if line.startswith("+++ "):
                dst = line[4:]
                if dst.endswith("\t"):
                    dst = dst[:-1]
                if dst == "/dev/null":
                    cur = FileChange(old_path, "D", old_path)
                    files[old_path] = cur
                else:
                    p = dst[2:]
                    status = "A" if old_path is None else ("R" if old_path != p else "M")
                    cur = FileChange(p, status, old_path)
                    files[p] = cur
                continue
            if line.startswith("@@"):
                m = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@", line)
                new_line = int(m.group(1)) if m else 0
                continue
            if cur is None:
                continue
            if line.startswith("+"):
                cur.added.append((new_line, line[1:]))
                new_line += 1
            elif line.startswith("-"):
                cur.removed.append(line[1:])
        return files

    def old_text(self, path: str | None) -> str | None:
        if not path:
            return None
        r = subprocess.run(["git", "show", f"{self.base_ref}:{path}"], cwd=ROOT, capture_output=True)
        return r.stdout.decode("utf-8", "replace") if r.returncode == 0 else None

    def new_text(self, path: str) -> str | None:
        if self.mode == "staged":
            r = subprocess.run(["git", "show", f":{path}"], cwd=ROOT, capture_output=True)
            return r.stdout.decode("utf-8", "replace") if r.returncode == 0 else None
        if self.mode == "commits":
            r = subprocess.run(["git", "show", f"{self.head}:{path}"], cwd=ROOT, capture_output=True)
            return r.stdout.decode("utf-8", "replace") if r.returncode == 0 else None
        fp = ROOT / path
        return fp.read_text(encoding="utf-8", errors="replace") if fp.is_file() else None

    def new_mode(self, path: str) -> str | None:
        """커밋될 파일 모드(인덱스 또는 head 기준). 작업 트리 모드는 core.fileMode=false면 의미가 없다."""
        if self.mode in ("staged", "worktree"):
            out = git("ls-files", "-s", "--", path, check=False).split()
        else:
            out = git("ls-tree", self.head, "--", path, check=False).split()
        return out[0] if out else None

    def new_size(self, path: str) -> int:
        if self.mode == "worktree":
            fp = ROOT / path
            return fp.stat().st_size if fp.is_file() else 0
        ref = ":" if self.mode == "staged" else f"{self.head}:"
        r = subprocess.run(["git", "cat-file", "-s", f"{ref}{path}"], cwd=ROOT, capture_output=True, text=True)
        return int(r.stdout.strip() or 0) if r.returncode == 0 else 0


def match_any(path: str, globs: list[str]) -> bool:
    return any(fnmatch.fnmatch(path, g) for g in globs)


def count_tests(text: str | None, path: str) -> int:
    if not text:
        return 0
    text = strip_comments(text)
    if path.endswith(".java"):
        return len(JAVA_TEST_DECL.findall(text))
    return len(TS_TEST_DECL.findall(text))


def is_test_file(path: str) -> bool:
    return bool(JAVA_TEST.match(path) or (FRONT_SRC.match(path) and FRONT_TEST.search(path)))


def store_entries(text: str | None) -> set[str]:
    """ArchUnit 동결 위반 목록. 코드가 움직여 줄 번호만 바뀐 항목은 같은 위반으로 본다."""
    if not text:
        return set()
    return {re.sub(r"\.java:\d+\)", ".java)", ln.strip()) for ln in text.splitlines() if ln.strip()}


def suppression_map(text: str | None) -> dict[tuple[str, str], int]:
    if not text:
        return {}
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return {("<invalid json>", "*"): 10**9}
    out: dict[tuple[str, str], int] = {}
    for f, rules in data.items():
        for r, v in rules.items():
            out[(f, r)] = int(v.get("count", 0)) if isinstance(v, dict) else int(v)
    return out


def mask_source(text: str) -> str:
    """Java/TS 최소 lexer. 주석과 문자열 리터럴 '내용'을 공백으로 가리고 길이·줄 경계를 그대로 둔다.

    - 문자열 따옴표는 남긴다(it('이름', …) 형태 유지, import 경로 위치 추적용).
    - 템플릿 문자열의 ${ … } 보간은 실행 코드이므로 가리지 않는다(H01.8 F3). 보간 안의 문자열·템플릿도 재귀 처리.
    정규식만으로는 문자열 안의 "//", "/*", import 예문, 보간 안의 단언을 구분하지 못했다(H01.7 N3, H01.8 F1~F3).
    """
    out = list(text)
    n = len(text)
    i = 0
    stack: list[int] = []  # 템플릿 보간마다 열린 중괄호 깊이

    def blank(a: int, b: int) -> None:
        for k in range(a, b):
            if out[k] != "\n":
                out[k] = " "

    def template(i: int) -> int:
        """백틱 다음 위치에서 시작. 닫는 백틱 다음 위치를 돌려준다. 보간은 code()로 넘긴다."""
        while i < n:
            c = text[i]
            if c == "\\":
                blank(i, min(i + 2, n))
                i += 2
            elif c == "`":
                return i + 1
            elif c == "$" and i + 1 < n and text[i + 1] == "{":
                i = code(i + 2, interp=True)
            else:
                blank(i, i + 1)
                i += 1
        return n

    def code(i: int, interp: bool = False) -> int:
        depth = 0
        while i < n:
            c = text[i]
            nx = text[i + 1] if i + 1 < n else ""
            if c == "/" and nx == "/":
                j = text.find("\n", i)
                j = n if j < 0 else j
                blank(i, j)
                i = j
            elif c == "/" and nx == "*":
                j = text.find("*/", i + 2)
                j = n if j < 0 else j + 2
                blank(i, j)
                i = j
            elif text.startswith('"""', i):  # Java 텍스트 블록(H01.9 A1/A2)
                j = text.find('"""', i + 3)
                j = n if j < 0 else j
                blank(i + 3, j)
                i = min(j + 3, n)
            elif c in "\"'":
                j = i + 1
                while j < n and text[j] != c and text[j] != "\n":
                    j += 2 if text[j] == "\\" else 1
                blank(i + 1, min(j, n))
                i = j + 1 if j < n and text[j] == c else j
            elif c == "`":
                i = template(i + 1)
            elif interp and c == "{":
                depth += 1
                i += 1
            elif interp and c == "}":
                if depth == 0:
                    return i + 1
                depth -= 1
                i += 1
            else:
                i += 1
        return n

    code(0)
    return "".join(out)


strip_comments = mask_source  # 하위 호환 이름

IMPORT_AT = re.compile(r"""(?:\bfrom|\bimport|\brequire)\s*\(?\s*(['"`])""")
TEST_SPEC = re.compile(r"(^|/)(test|tests|__tests__|fixtures?)(/|$)|\.(test|spec)(\.\w+)?$")


def imported_test_specs(text: str | None) -> set[str]:
    """실제 import/require 구문의 모듈 경로 중 테스트·fixture 경로.

    마스킹한 코드에서 구문 위치를 찾고, 같은 위치의 원문에서 경로 문자열을 읽는다.
    주석·문자열 안의 import 예문은 코드가 아니므로 무시된다(H01.8 F2). 여러 줄·주석 낀 동적 import도 잡는다(H01.7 N1).
    """
    if not text:
        return set()
    masked = mask_source(text)
    specs = set()
    for m in IMPORT_AT.finditer(masked):
        q = m.start(1)
        close = masked.find(masked[q], q + 1)
        if close < 0:
            continue
        spec = text[q + 1:close]
        if TEST_SPEC.search(spec):
            specs.add(spec)
    return specs


def minimums(text: str | None) -> list[float]:
    if not text:
        return []
    return [float(x) for x in re.findall(r"minimum\s*=\s*(?:new BigDecimal\(\s*['\"])?([0-9]*\.?[0-9]+)", text)]


def vitest_thresholds(text: str | None) -> dict[str, float]:
    if not text:
        return {}
    m = re.search(r"thresholds\s*:\s*\{([^}]*)\}", text, re.S)
    if not m:
        return {}
    return {k: float(v) for k, v in re.findall(r"(\w+)\s*:\s*([0-9]+(?:\.[0-9]+)?)", m.group(1))}


def check(cs: ChangeSet) -> list[Finding]:
    out: list[Finding] = []
    test_delta: dict[str, int] = {}
    assert_delta: dict[str, int] = {}
    migrations_added: list[str] = []

    for path, fc in sorted(cs.files.items()):
        added = fc.added

        # ---- 파일 단위 ----
        if fc.status != "D" and path not in LARGE_FILE_ALLOW and cs.new_size(path) > MAX_FILE_BYTES:
            out.append(Finding("large-file", path, None, f"{cs.new_size(path)} bytes > 1MiB"))
        if fc.status in ("A", "R") and SECRET_FILE_NAMES.search(path) and not SECRET_FILE_ALLOW.search(path):
            out.append(Finding("secret-file", path, None, "비밀 파일(.env/키스토어/키)을 추가하지 않는다"))

        mm = MIGRATION.match(path)
        mold = MIGRATION.match(fc.old_path or "")
        if path == "sql/schema.sql" and fc.status != "A":
            out.append(Finding("migration-baseline", path, None, "기준선 schema.sql은 수정하지 않는다. 새 V<n> 마이그레이션을 추가한다"))
        if MIGRATION_NESTED.match(path) and fc.status != "D":
            out.append(Finding("migration-name", path, None, "마이그레이션은 sql/migrations/ 바로 아래 V<n>__name.sql 로만 둔다"))
        if (mm or mold) and fc.status in ("M", "D", "R"):
            out.append(Finding("migration-immutable", fc.old_path or path, None,
                               "이미 존재하는 마이그레이션은 수정·삭제·이름변경하지 않는다(새 V<n> 추가)"))
        if mm and fc.status == "A" and path.endswith(".sql"):
            migrations_added.append(mm.group("name"))

        if fc.status != "D" and match_any(path, EXECUTABLE_GLOBS):
            mode = cs.new_mode(path)
            if mode is not None and mode != "100755":
                out.append(Finding("exec-bit", path, None,
                                   f"실행 파일인데 모드 {mode}. `git update-index --chmod=+x {path}` (core.fileMode=false라 chmod만으로는 안 된다)"))
        if match_any(path, PROTECTED_GLOBS) or (fc.old_path and match_any(fc.old_path, PROTECTED_GLOBS)):
            out.append(Finding("gate-protected", path, None, "게이트/하네스 설정 변경", approvable=True))

        if fnmatch.fnmatch(path, ARCHUNIT_STORE_GLOB) and not path.endswith("stored.rules"):
            new_entries = store_entries(cs.new_text(path) if fc.status != "D" else None) - \
                store_entries(cs.old_text(fc.old_path or path) if fc.status != "A" else None)
            if new_entries:
                sample = next(iter(new_entries))[:100]
                out.append(Finding("archunit-freeze-grew", path, None,
                                   f"동결에 새 위반 {len(new_entries)}건(예: {sample}). 줄 번호만 바뀐 것은 제외하고 비교했다",
                                   approvable=True))
        if path == ESLINT_SUPPRESSIONS:
            old_s, new_s = suppression_map(cs.old_text(path)), suppression_map(cs.new_text(path))
            grown = sorted(k for k, v in new_s.items() if v > old_s.get(k, 0))
            if grown:
                out.append(Finding("eslint-suppression-grew", path, None,
                                   f"lint 동결 증가(파일·규칙별, 총량이 같아도 교체는 증가로 본다): "
                                   f"{', '.join(f'{f}:{r}' for f, r in grown[:5])}", approvable=True))
        if path.endswith("build.gradle") and fc.status != "A":
            old, new = cs.old_text(path), cs.new_text(path)
            if sorted(minimums(new)) != sorted(minimums(old)) and (
                    len(minimums(new)) < len(minimums(old)) or min(minimums(new) or [1]) < min(minimums(old) or [0])
                    or sum(minimums(new)) < sum(minimums(old))):
                out.append(Finding("coverage-threshold-lowered", path, None,
                                   f"커버리지 하한 {minimums(old)} → {minimums(new)}", approvable=True))
        if path == "vite-react-teamsketch/vitest.config.ts":
            old, new = vitest_thresholds(cs.old_text(path)), vitest_thresholds(cs.new_text(path))
            lowered = [k for k, v in old.items() if new.get(k, -1) < v]
            if lowered:
                out.append(Finding("coverage-threshold-lowered", path, None,
                                   f"vitest 하한 하락: {', '.join(lowered)}", approvable=True))
        if path == "vite-react-teamsketch/package.json":
            old, new = cs.old_text(path), cs.new_text(path)
            try:
                os_, ns_ = json.loads(old or "{}").get("scripts", {}), json.loads(new or "{}").get("scripts", {})
            except json.JSONDecodeError:
                os_, ns_ = {}, {}
            changed = [k for k in ("lint", "typecheck", "test", "test:coverage", "verify", "build")
                       if os_.get(k) != ns_.get(k) and k in os_]
            if changed:
                out.append(Finding("frontend-gate-script", path, None,
                                   f"게이트 npm 스크립트 변경: {', '.join(changed)}", approvable=True))

        if FRONT_SRC.match(path) and not FRONT_TEST.search(path) and fc.status != "D":
            new_specs = imported_test_specs(cs.new_text(path)) - \
                imported_test_specs(cs.old_text(fc.old_path or path) if fc.status != "A" else None)
            if new_specs:
                out.append(Finding("prod-imports-test", path, None,
                                   f"프로덕션 코드가 테스트·fixture 경로를 가져온다: {', '.join(sorted(new_specs))} "
                                   "(커버리지·테스트 제외 경로로 업무 코드가 숨는다)"))
        if is_test_file(path) or (fc.old_path and is_test_file(fc.old_path)):
            old_t = cs.old_text(fc.old_path or path) if fc.status != "A" else None
            new_t = cs.new_text(path) if fc.status != "D" else None
            assert_delta[path] = len(ASSERTION.findall(strip_comments(new_t or ""))) - \
                len(ASSERTION.findall(strip_comments(old_t or "")))
            test_delta[path] = count_tests(cs.new_text(path) if fc.status != "D" else None, path) - \
                count_tests(cs.old_text(fc.old_path or path) if fc.status != "A" else None, fc.old_path or path)

        # ---- 줄 단위 ----
        is_md = path.endswith(".md")
        for ln, text in added:
            if CONFLICT.match(text):
                out.append(Finding("conflict-marker", path, ln, "머지 충돌 표식"))
            if not is_md and text != text.rstrip() and not path.endswith((".diff", ".patch")):
                out.append(Finding("trailing-whitespace", path, ln, "줄 끝 공백"))
            for name, rx in SECRET_PATTERNS:
                if rx.search(text):
                    out.append(Finding("secret", path, ln, f"시크릿 의심({name})"))
            if CONFIG_FILE.search(path) and not SECRET_FILE_ALLOW.search(path) and "/src/test/" not in path:
                m = CONFIG_SECRET.match(text)
                val = m.group("val") if m else ""
                if m and "${" not in val and not SECRET_PLACEHOLDER.match(val) and len(val) >= 6:
                    out.append(Finding("secret", path, ln, "설정 파일 시크릿 하드코딩 의심(${ENV} 참조로)"))
            if WEAKENING_LINES.search(text) and (path.endswith((".gradle", ".yml", ".yaml", ".sh", ".json"))
                                                 or path.startswith(".githooks/")):
                # 보호 파일은 gate-protected 로 이미 승인 대상이다. 중복 보고하지 않는다.
                if not match_any(path, PROTECTED_GLOBS):
                    out.append(Finding("gate-bypass", path, ln, f"검증 우회 의심: {text.strip()[:80]}", approvable=True))

            if JAVA_MAIN.match(path):
                if STDOUT_JAVA.search(text):
                    out.append(Finding("java-stdout", path, ln, "System.out/err·printStackTrace 금지 → SLF4J"))
                if path.startswith("CoreService/") and BEARER.search(text) and not path.endswith("/TokenUtils.java"):
                    out.append(Finding("token-single-source", path, ln,
                                       "토큰 추출은 TokenUtils.extractTokenWithoutBearer() 단일 소스"))
                if LOCALHOST_LITERAL.search(text) and "${" not in text and not text.lstrip().startswith(("//", "*")):
                    out.append(Finding("hardcoded-host", path, ln, "호스트 하드코딩 금지 → 프로퍼티/env"))
            if FRONT_SRC.match(path) and not FRONT_TEST.search(path) and CONSOLE_TS.search(text):
                out.append(Finding("ts-console", path, ln, "console.log 계열 금지(console.error/warn만 허용)"))
            if is_test_file(path) and TEST_DISABLE.search(text):
                out.append(Finding("test-disabled", path, ln, f"테스트 비활성·포커스 금지: {text.strip()[:80]}"))
            elif is_test_file(path) and TEST_ASSUME.search(text):
                out.append(Finding("test-conditional-skip", path, ln,
                                   f"조건부 스킵은 CI에서 조용히 빠질 수 있다: {text.strip()[:80]}", approvable=True))

    # ---- 집계 규칙 ----
    net = sum(test_delta.values())
    if net < 0:
        detail = ", ".join(f"{p}:{d:+d}" for p, d in test_delta.items() if d)
        out.append(Finding("test-count-decreased", "(changeset)", None,
                           f"테스트 선언 순감 {net} ({detail}). 테스트를 지우지 말고 옮기거나 대체한다"))

    anet = sum(assert_delta.values())
    if anet < 0:
        detail = ", ".join(f"{p}:{d:+d}" for p, d in assert_delta.items() if d)
        out.append(Finding("assertion-count-decreased", "(changeset)", None,
                           f"테스트 단언 순감 {anet} ({detail}). 단언을 지우면 테스트가 껍데기가 된다. "
                           "정말 대체했다면 같은 변경에서 더 강한 단언을 추가한다", approvable=True))

    if migrations_added:
        existing = [MIGRATION_NAME.match(n) for n in _base_migrations(cs)]
        base_versions = sorted(int(m.group("v")) for m in existing if m)
        nxt = (base_versions[-1] if base_versions else 0) + 1
        for name in sorted(migrations_added, key=lambda n: int(MIGRATION_NAME.match(n).group("v")) if MIGRATION_NAME.match(n) else 0):
            m = MIGRATION_NAME.match(name)
            if not m:
                out.append(Finding("migration-name", f"sql/migrations/{name}", None, "이름 규칙 V<n>__snake_case.sql"))
                continue
            if int(m.group("v")) != nxt:
                out.append(Finding("migration-version", f"sql/migrations/{name}", None,
                                   f"다음 버전은 V{nxt} (기존 최대 V{nxt - 1}, 중복·건너뛰기 금지)"))
            nxt = int(m.group("v")) + 1
    return out


def _base_migrations(cs: ChangeSet) -> list[str]:
    r = subprocess.run(["git", "ls-tree", "--name-only", f"{cs.base_ref}:sql/migrations"], cwd=ROOT,
                       capture_output=True, text=True)
    return r.stdout.split() if r.returncode == 0 else []


def main() -> int:
    ap = argparse.ArgumentParser()
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--staged", action="store_true")
    g.add_argument("--base")
    ap.add_argument("--worktree", action="store_true", help="--base와 함께: 커밋되지 않은 작업 트리까지 검사")
    ap.add_argument("--head", default="HEAD", help="--base와 함께: 검사할 커밋(기본 HEAD)")
    ap.add_argument("--json", help="결과를 JSON으로 저장")
    a = ap.parse_args()

    mode = "staged" if a.staged else ("worktree" if a.worktree else "commits")
    cs = ChangeSet(mode, a.base, a.head)
    findings = check(cs)
    approved = os.environ.get("GATE_APPROVED") == "1"

    errors = [f for f in findings if not (f.approvable and approved)]
    warns = [f for f in findings if f.approvable and approved]
    print(f"[policy] mode={mode} base={cs.base_ref[:12]} files={len(cs.files)} "
          f"errors={len(errors)} approved-warnings={len(warns)}")
    for f in errors:
        loc = f"{f.path}:{f.line}" if f.line else f.path
        tag = " (게이트 변경: 승인 필요 → GATE_APPROVED=1 / PR 라벨 gate-change-approved)" if f.approvable else ""
        print(f"  ✗ [{f.rule}] {loc} — {f.msg}{tag}")
    for f in warns:
        loc = f"{f.path}:{f.line}" if f.line else f.path
        print(f"  ! [{f.rule}] {loc} — {f.msg} (승인됨)")
    if a.json:
        Path(a.json).write_text(json.dumps([f.__dict__ for f in findings], ensure_ascii=False, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
