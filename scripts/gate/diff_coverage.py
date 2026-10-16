#!/usr/bin/env python3
"""변경한 프로덕션 코드 줄이 테스트로 실행되는지 검사한다 (diff coverage).

전체 커버리지 하한(래칫)은 "나빠지지 않음"만 보장한다. 이 검사는 "이번에 바꾼 코드는 테스트가 있음"을 보장한다.
리팩토링에서 동작 보존을 사람이 대신 확인하지 않아도 되게 만드는 핵심 장치다.

입력 리포트(있는 것만 자동 수집, 여러 개면 줄 단위로 합친다):
  */build/reports/jacoco/**/*.xml            (Gradle JaCoCo: test, jacocoIntegrationReport, sql)
  vite-react-teamsketch/coverage/lcov.info   (Vitest v8)

사용:
  diff_coverage.py --base origin/main [--worktree] [--threshold 80]
변경된 프로덕션 파일이 속한 모듈의 리포트가 없으면 실패한다(테스트를 안 돌린 것을 통과로 세지 않는다).
"""
from __future__ import annotations

import argparse
import glob
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from policy import mask_source  # noqa: E402  주석·문자열을 구분하는 같은 lexer를 쓴다

ROOT = Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True,
                           check=True).stdout.strip())
JAVA_MAIN = re.compile(r"^(?P<module>[^/]+)/src/main/java/(?P<rel>.+\.java)$")
FRONT_MAIN = re.compile(r"^vite-react-teamsketch/(?P<rel>src/.+\.(ts|tsx))$")
FRONT_EXCLUDE = re.compile(r"(\.test\.|\.spec\.|/test/|/__tests__/|\.d\.ts$)")
# vitest coverage.exclude 와 같은 진입점. 리포트에 없으므로 바뀐 실행 줄은 '미커버'로 센다(업무 코드를 숨기지 못하게).
FRONT_ENTRY = re.compile(r"^vite-react-teamsketch/src/main\.tsx$")
# 주석을 가린 뒤 비어 있거나 닫는 괄호뿐인 줄만 면제한다. import 줄도, 주석 뒤 실행문도 센다(H01.7 N1, H01.8 F1)
FRONT_TRIVIAL = re.compile(r"^\s*[)}\];,]*\s*$")
# Core는 ../sql/src/main/java 를 소스셋에 포함한다. sql 모듈 파일은 Core 또는 sql 리포트 어디에 있어도 된다.
MODULE_REPORT_ALIASES = {"sql": ["sql", "CoreService"]}


def git(*args: str) -> str:
    return subprocess.run(["git", "-c", "core.quotePath=false", *args], cwd=ROOT, capture_output=True, text=True,
                          check=True).stdout


def changed_lines(base: str, worktree: bool, head: str) -> dict[str, set[int]]:
    mb = git("merge-base", base, head).strip()
    args = ["diff", mb] if worktree else ["diff", mb, head]
    raw = git(*args, "-M", "--no-color", "-U0", "--diff-filter=AMR")
    out: dict[str, set[int]] = {}
    cur = None
    for line in raw.splitlines():
        if line.startswith("+++ "):
            dst = line[4:]
            if dst.endswith("\t"):  # 공백 포함 경로의 헤더 끝 탭(H01.7 N2)
                dst = dst[:-1]
            cur = None if dst == "/dev/null" else dst[2:]
            if cur:
                out.setdefault(cur, set())
            continue
        m = re.match(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", line)
        if m and cur:
            start, count = int(m.group(1)), int(m.group(2) or 1)
            out[cur].update(range(start, start + count))
    if worktree:
        for p in git("ls-files", "--others", "--exclude-standard").splitlines():
            fp = ROOT / p
            if fp.is_file() and (JAVA_MAIN.match(p) or FRONT_MAIN.match(p)):
                n = len(fp.read_text(encoding="utf-8", errors="replace").splitlines())
                out[p] = set(range(1, n + 1))
    return out


FRESH_SINCE = 0.0  # 이 시각(epoch) 이전 리포트는 이번 실행이 만든 것이 아니므로 쓰지 않는다(H01.6 R7)


def fresh(path: str) -> bool:
    return os.path.getmtime(path) >= FRESH_SINCE


def load_jacoco() -> tuple[dict[str, dict[int, tuple[bool, int, int]]], set[str]]:
    """key: '<module>|<pkg/Name.java>' 와 '*|<pkg/Name.java>' → {line: (covered, missed_branches, covered_branches)}"""
    data: dict[str, dict[int, tuple[bool, int, int]]] = {}
    modules: set[str] = set()
    for xml_path in glob.glob(str(ROOT / "*/build/reports/jacoco/**/*.xml"), recursive=True):
        if not fresh(xml_path):
            continue
        module = Path(xml_path).relative_to(ROOT).parts[0]
        modules.add(module)
        try:
            tree = ET.parse(xml_path)
        except ET.ParseError:
            continue
        for pkg in tree.getroot().iter("package"):
            for sf in pkg.iter("sourcefile"):
                rel = f"{pkg.get('name')}/{sf.get('name')}"
                for key in (f"{module}|{rel}", f"*|{rel}"):
                    lines = data.setdefault(key, {})
                    for ln in sf.iter("line"):
                        nr, ci = int(ln.get("nr")), int(ln.get("ci"))
                        mb, cb = int(ln.get("mb")), int(ln.get("cb"))
                        prev = lines.get(nr)
                        if prev:
                            lines[nr] = (prev[0] or ci > 0, min(prev[1], mb), max(prev[2], cb))
                        else:
                            lines[nr] = (ci > 0, mb, cb)
    return data, modules


def load_lcov() -> dict[str, dict[int, bool]] | None:
    path = ROOT / "vite-react-teamsketch/coverage/lcov.info"
    if not path.is_file() or not fresh(str(path)):
        return None
    data: dict[str, dict[int, bool]] = {}
    cur = None
    for line in path.read_text().splitlines():
        if line.startswith("SF:"):
            sf = line[3:]
            p = Path(sf)
            if p.is_absolute():
                try:
                    sf = str(p.relative_to(ROOT / "vite-react-teamsketch"))
                except ValueError:
                    sf = str(p)
            cur = data.setdefault(sf, {})
        elif line.startswith("DA:") and cur is not None:
            nr, hits = line[3:].split(",")[:2]
            cur[int(nr)] = cur.get(int(nr), False) or int(hits) > 0
    return data


def ranges(nums: list[int]) -> str:
    out, start, prev = [], None, None
    for n in sorted(nums):
        if start is None:
            start = prev = n
        elif n == prev + 1:
            prev = n
        else:
            out.append(f"{start}-{prev}" if start != prev else str(start))
            start = prev = n
    if start is not None:
        out.append(f"{start}-{prev}" if start != prev else str(start))
    return ",".join(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--worktree", action="store_true")
    ap.add_argument("--threshold", type=float, default=float(os.environ.get("GATE_DIFF_COVERAGE", 80)))
    ap.add_argument("--only", choices=["java", "front"], help="한쪽만 검사(로컬 부분 실행용)")
    ap.add_argument("--fresh-since", type=float, default=float(os.environ.get("GATE_FRESH_SINCE", 0)),
                    help="이 epoch 이전 리포트 무시(기본 0=끔). Gradle UP-TO-DATE 리포트도 오래돼 보이므로 기본으로 켜지 않는다")
    a = ap.parse_args()
    global FRESH_SINCE
    FRESH_SINCE = a.fresh_since

    changes = changed_lines(a.base, a.worktree, a.head)
    jacoco, jmodules = load_jacoco()
    lcov = load_lcov()

    total = covered = 0
    missing_reports: set[str] = set()
    uncovered: list[str] = []
    partial: list[str] = []
    no_info: list[str] = []

    for path, lines in sorted(changes.items()):
        if not lines:
            continue
        jm, fm = JAVA_MAIN.match(path), FRONT_MAIN.match(path)
        if jm and a.only != "front":
            module, rel = jm.group("module"), jm.group("rel")
            candidates = MODULE_REPORT_ALIASES.get(module, [module])
            if not any(c in jmodules for c in candidates):
                missing_reports.add(module)
                continue
            info = None
            for c in candidates:
                info = jacoco.get(f"{c}|{rel}") or info
            if info is None:
                # 리포트는 있는데 파일이 없다 = 클래스가 한 번도 로드되지 않음(전부 미커버) 또는 인터페이스/상수뿐.
                no_info.append(path)
                continue
            exe = sorted(ln for ln in lines if ln in info)
            miss = [ln for ln in exe if not info[ln][0]]
            br = [ln for ln in exe if info[ln][0] and info[ln][1] > 0]
            total += len(exe)
            covered += len(exe) - len(miss)
            if miss:
                uncovered.append(f"{path}: {ranges(miss)}")
            if br:
                partial.append(f"{path}: {ranges(br)}")
        elif FRONT_ENTRY.match(path) and a.only != "java":
            raw_text = (ROOT / path).read_text(encoding="utf-8", errors="replace") if (ROOT / path).is_file() else ""
            text = mask_source(raw_text).splitlines()
            exe = sorted(ln for ln in lines if 0 < ln <= len(text) and not FRONT_TRIVIAL.match(text[ln - 1]))
            total += len(exe)
            if exe:
                uncovered.append(f"{path}: {ranges(exe)} (진입점은 테스트 리포트에 없다 → 로직은 테스트 가능한 모듈로 옮긴다)")
        elif fm and not FRONT_EXCLUDE.search(path) and a.only != "java":
            if lcov is None:
                missing_reports.add("vite-react-teamsketch")
                continue
            info = lcov.get(fm.group("rel"))
            if info is None:
                no_info.append(path)
                continue
            exe = sorted(ln for ln in lines if ln in info)
            miss = [ln for ln in exe if not info[ln]]
            total += len(exe)
            covered += len(exe) - len(miss)
            if miss:
                uncovered.append(f"{path}: {ranges(miss)}")

    failed = False
    if missing_reports:
        print(f"[diff-coverage] ✗ 커버리지 리포트 없음: {', '.join(sorted(missing_reports))} "
              f"— 해당 모듈 테스트(./gradlew check / npm run test:coverage)를 먼저 실행한다")
        failed = True
    # 리포트에 아예 없는 변경 파일: Java는 JaCoCo가 로드된 클래스만 기록하므로 '한 번도 실행 안 됨'으로 본다.
    real_no_info = []
    for p in no_info:
        text = (ROOT / p).read_text(encoding="utf-8", errors="replace") if (ROOT / p).is_file() else ""
        if p.endswith(".java") and re.search(r"\b(interface|@interface|record)\b", text) and not re.search(r"\bclass\b", text):
            continue  # 실행 줄 없는 인터페이스/애너테이션/레코드
        # TS: vitest는 coverage.include의 모든 파일을 리포트에 넣는다(타입 전용 파일도 SF로 존재).
        # 리포트에 없다 = 리포트가 이번 코드에서 만들어지지 않았다 → 예외 없이 실패(H01.6 R9)
        real_no_info.append(p)
    if real_no_info:
        print("[diff-coverage] ✗ 변경했지만 테스트가 한 번도 로드하지 않은 파일:")
        for p in real_no_info:
            print(f"    {p}")
        failed = True

    pct = 100.0 if total == 0 else covered * 100.0 / total
    print(f"[diff-coverage] 변경 실행 줄 {total}개 중 {covered}개 커버 = {pct:.1f}% (기준 {a.threshold:.0f}%)")
    if uncovered:
        print("  미커버 변경 줄:")
        for u in uncovered:
            print(f"    {u}")
    if partial:
        print("  분기 일부 미커버(경고):")
        for u in partial:
            print(f"    {u}")
    if total and pct < a.threshold:
        print(f"[diff-coverage] ✗ 변경 줄 커버리지 {pct:.1f}% < {a.threshold:.0f}%. 바꾼 동작을 고정하는 테스트를 추가한다")
        failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
