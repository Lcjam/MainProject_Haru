#!/usr/bin/env python3
"""Newman JSON 리포트로 E2E 회귀의 '실제로 실행된 검사'를 확인한다.

Newman JUnit 리포터는 pm.test.skip 으로 건너뛴 단언을 skipped로 직렬화하지 않고 tests에 센다(H01.6 R4).
그래서 JUnit만 보면 '308개 통과'와 '308개 건너뜀'을 구분할 수 없다. JSON의 executions[].assertions[]를 직접 센다.

사용: newman_check.py <newman.json> [--min-requests N] [--min-assertions N]
      newman_check.py --self-test
"""
from __future__ import annotations

import argparse
import json
import sys

MIN_REQUESTS = 140    # 2026-10-16 실측 149요청
MIN_ASSERTIONS = 300  # 2026-10-16 실측 308검사


def evaluate(report: dict, min_requests: int, min_assertions: int) -> tuple[bool, str]:
    run = report.get("run", {})
    stats = run.get("stats", {})
    executions = run.get("executions", [])
    failures = run.get("failures", [])
    requests = sum(1 for e in executions if e.get("response") is not None or e.get("requestError"))
    executed = skipped = failed = 0
    for e in executions:
        for a in e.get("assertions", []) or []:
            if a.get("skipped"):
                skipped += 1
            else:
                executed += 1
                if a.get("error"):
                    failed += 1
    req_failed = int(stats.get("requests", {}).get("failed", 0))
    summary = (f"requests={requests} assertions_executed={executed} skipped={skipped} "
               f"assertion_failures={failed} request_failures={req_failed} run_failures={len(failures)}")
    problems = []
    if failures or failed or req_failed:
        problems.append("실패 있음")
    if skipped:
        problems.append(f"건너뛴 검사 {skipped}건(pm.test.skip 등) — 실행하지 않은 검사를 통과로 세지 않는다")
    if requests < min_requests:
        problems.append(f"요청 {requests} < 최소 {min_requests}")
    if executed < min_assertions:
        problems.append(f"실행된 검사 {executed} < 최소 {min_assertions}")
    return (not problems), summary + ("" if not problems else " ✗ " + "; ".join(problems))


def self_test() -> int:
    def mk(n_req: int, per: int, skip: bool = False, err: bool = False, response: bool = True) -> dict:
        ex = []
        for i in range(n_req):
            ex.append({"response": {"code": 200} if response else None,
                       "assertions": [{"assertion": f"a{i}-{j}", "skipped": skip,
                                       **({"error": {"message": "x"}} if err else {})} for j in range(per)]})
        return {"run": {"stats": {"requests": {"failed": 0}}, "executions": ex, "failures": []}}
    cases = [
        ("정상 149요청×3검사", mk(149, 3), True),
        ("R4: 요청은 성공, 단언 전부 skipped", mk(149, 3, skip=True), False),
        ("요청 자체를 skipRequest(응답 없음)", mk(149, 3, response=False), False),
        ("단언 실패", mk(149, 3, err=True), False),
        ("컬렉션 축소(요청 100)", mk(100, 4), False),
    ]
    ok = True
    for name, rep, expect in cases:
        got, summary = evaluate(rep, MIN_REQUESTS, MIN_ASSERTIONS)
        mark = "PASS" if got == expect else "FAIL"
        ok &= got == expect
        print(f"[newman-check self-test] {mark} {name}: {summary}")
    return 0 if ok else 1


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("report", nargs="?")
    ap.add_argument("--min-requests", type=int, default=MIN_REQUESTS)
    ap.add_argument("--min-assertions", type=int, default=MIN_ASSERTIONS)
    ap.add_argument("--self-test", action="store_true")
    a = ap.parse_args()
    if a.self_test:
        return self_test()
    if not a.report:
        ap.error("report 경로 필요")
    with open(a.report, encoding="utf-8") as fh:
        ok, summary = evaluate(json.load(fh), a.min_requests, a.min_assertions)
    print(f"[e2e] newman: {summary}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
