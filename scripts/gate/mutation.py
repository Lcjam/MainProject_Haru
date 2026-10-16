#!/usr/bin/env python3
"""Run PIT on changed service classes and score only mutants on changed lines."""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from diff_coverage import JAVA_MAIN, ROOT, changed_lines

SERVICES = {"CoreService", "AssistService", "GateWay"}
# Preserve newlines while removing comments/literals before inspecting Java declarations.
JAVA_NON_CODE = re.compile(r'//[^\n]*|/\*.*?\*/|""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', re.S)


def target_class(path: str, source: str) -> tuple[str | None, str]:
    match = JAVA_MAIN.fullmatch(path)
    if not match:
        return None, "Java main 소스 아님"
    if match["module"] not in SERVICES:
        return None, "세 서비스 밖의 모듈"
    rel = Path(match["rel"])
    name = rel.stem
    code = JAVA_NON_CODE.sub(lambda m: "\n" * m[0].count("\n") + " ", source)
    # shortcut: one primary top-level type per file; extend discovery if auxiliary types gain logic.
    declaration = re.search(r"\b(class|interface|record|enum)\s+" + re.escape(name) + r"\b", code)
    if not declaration:
        if name in {"package-info", "module-info"}:
            return None, "패키지/모듈 메타데이터"
        raise ValueError(f"Java 타입 선언을 찾을 수 없음: {path}")
    if declaration[1] in {"interface", "record"}:
        return None, declaration[1]
    if "dto" in (part.lower() for part in rel.parts[:-1]) or name.lower().endswith("dto"):
        return None, "DTO"
    if name.endswith("Application"):
        return None, "Application 진입점"
    if ("config" in (part.lower() for part in rel.parts[:-1])
            or re.search(r"@(?:[\w.]+\.)?(?:Configuration|SpringBootConfiguration|TestConfiguration)\b", code)):
        return None, "설정 클래스"
    package = re.search(r"\bpackage\s+([\w.]+)\s*;", code)
    return (package[1] + "." if package else "") + name, ""


def tally(report: ET.Element, targets: dict[str, tuple[str, set[int]]]) -> tuple[int, int, list[str]]:
    if report.tag != "mutations":
        raise ValueError(f"PIT XML 루트가 mutations가 아님: {report.tag}")
    killed = total = 0
    survivors = []
    for mutation in report.findall("mutation"):
        outer = mutation.findtext("mutatedClass", "").split("$", 1)[0]
        if outer not in targets:
            continue
        path, lines = targets[outer]
        line = int(mutation.findtext("lineNumber", "0"))
        if line not in lines:
            continue
        status = mutation.get("status")
        if status == "NON_VIABLE":
            print(f"[mutation] 집계 제외 NON_VIABLE: {path}:{line}")
            continue
        if status not in {"KILLED", "TIMED_OUT", "SURVIVED", "NO_COVERAGE"}:
            raise ValueError(f"PIT 실행 오류 상태 {status}: {path}:{line}")
        total += 1
        killed += status in {"KILLED", "TIMED_OUT"}
        if status in {"SURVIVED", "NO_COVERAGE"}:
            survivors.append(f"{path}:{line} {mutation.findtext('mutator')} "
                             f"{mutation.findtext('description')} [{status}]")
    return killed, total, survivors


def self_test() -> None:
    prefix = "CoreService/src/main/java/com/example/"
    assert target_class(prefix + "A.java", 'package com.example; /* record A */ class A { record R() {} }')[0] == "com.example.A"
    for rel, source in [("A.java", "interface A {}"), ("A.java", "record A() {}"),
                        ("dto/A.java", "class A {}"), ("ADTO.java", "class ADTO {}"),
                        ("MyApplication.java", "class MyApplication {}"),
                        ("config/A.java", "class A {}"), ("A.java", "@Configuration class A {}")]:
        assert target_class(prefix + rel, source)[0] is None, rel
    assert target_class(prefix + "A.java", 'class A { String s = "@Configuration"; }')[0] == "A"
    report = ET.fromstring("<mutations>" + "".join(
        f'<mutation status="{status}"><mutatedClass>A$Inner</mutatedClass><lineNumber>{line}</lineNumber>'
        '<mutator>M</mutator><description>removed call</description></mutation>'
        for status, line in [("KILLED", 2), ("TIMED_OUT", 2), ("SURVIVED", 2),
                             ("NO_COVERAGE", 2), ("SURVIVED", 99)]) + "</mutations>")
    killed, total, survivors = tally(report, {"A": ("A.java", {2})})
    assert (killed, total, len(survivors)) == (2, 4, 2)
    assert survivors[0] == "A.java:2 M removed call [SURVIVED]"
    report[0].set("status", "RUN_ERROR")
    for invalid in (report, ET.fromstring("<invalid/>")):
        try:
            tally(invalid, {"A": ("A.java", {2})})
        except ValueError:
            pass
        else:
            raise AssertionError("PIT errors must fail closed")
    print("[mutation] self-test PASS")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base")
    parser.add_argument("--worktree", action="store_true")
    parser.add_argument("--threshold", type=float, default=60)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    if not args.base:
        parser.error("--base is required")
    if not 0 <= args.threshold <= 100:
        parser.error("--threshold must be between 0 and 100")

    services: dict[str, dict[str, tuple[str, set[int]]]] = {}
    for path, lines in sorted(changed_lines(args.base, args.worktree, "HEAD").items()):
        if not lines or not JAVA_MAIN.fullmatch(path):
            continue
        source = (ROOT / path).read_text(encoding="utf-8")
        if not args.worktree:
            committed = subprocess.run(["git", "show", f"HEAD:{path}"], cwd=ROOT,
                                       capture_output=True, text=True, check=True).stdout
            if source != committed:
                raise ValueError(f"커밋과 작업 트리가 다름: {path}; --worktree 사용 필요")
        name, reason = target_class(path, source)
        if name is None:
            print(f"[mutation] 제외 {path}: {reason}", flush=True)
            continue
        services.setdefault(path.split("/", 1)[0], {})[name] = (path, lines)
    if not services:
        print("[mutation] 대상 없음 — PASS")
        return 0

    started = time.monotonic()
    killed = total = 0
    for service, targets in sorted(services.items()):
        report = ROOT / service / "build/reports/pitest/mutations.xml"
        # Never accept a previous invocation's report after a failed/skipped PIT run.
        report.unlink(missing_ok=True)
        command = ["./gradlew", "pitest", "-PpitTargetClasses=" + ",".join(sorted(targets)), "--console=plain"]
        print(f"[mutation] {service}: {' '.join(command)}", flush=True)
        service_started = time.monotonic()
        result = subprocess.run(command, cwd=ROOT / service)
        elapsed = time.monotonic() - service_started
        print(f"[mutation] {service}: {elapsed:.1f}s", flush=True)
        if result.returncode:
            print(f"[mutation] FAIL: {service} Gradle exit {result.returncode}")
            return 1
        if not report.is_file():
            raise ValueError(f"PIT 리포트 없음: {report}")
        service_killed, service_total, survivors = tally(ET.parse(report).getroot(), targets)
        killed += service_killed
        total += service_total
        print(f"[mutation] {service}: 변경 줄 {service_killed}/{service_total} killed")
        for survivor in survivors:
            print(f"    {survivor}")

    score = 100.0 if total == 0 else killed * 100.0 / total
    if total == 0:
        print("[mutation] 변경 줄에 집계 가능한 뮤턴트 없음")
    passed = score >= args.threshold
    print(f"[mutation] {'PASS' if passed else 'FAIL'}: 변경 줄 {killed}/{total} = {score:.1f}% "
          f"(기준 {args.threshold:g}%), 총 {time.monotonic() - started:.1f}s")
    return 0 if passed else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, ET.ParseError, subprocess.CalledProcessError) as error:
        print(f"[mutation] FAIL: {error}", file=sys.stderr)
        sys.exit(1)
