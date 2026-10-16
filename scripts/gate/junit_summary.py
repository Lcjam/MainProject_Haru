#!/usr/bin/env python3
"""JUnit XML 합계를 출력하고, 실행 0건·실패가 있으면 실패한다.

사용: junit_summary.py <dir> [--min N]
'테스트가 돌았다'를 숫자로 확인한다. 태그 필터·조건부 스킵 때문에 아무것도 안 돌고 초록불이 되는 것을 막는다.
"""
import argparse
import glob
import sys
import xml.etree.ElementTree as ET

ap = argparse.ArgumentParser()
ap.add_argument("dir")
ap.add_argument("--min", type=int, default=0, help="최소 실행(=tests-skipped) 건수")
ap.add_argument("--max-skipped", type=int, default=-1, help="허용 skipped 상한(-1: 제한 없음)")
a = ap.parse_args()

t = f = e = s = 0
files = glob.glob(f"{a.dir}/**/TEST-*.xml", recursive=True)
for p in files:
    r = ET.parse(p).getroot()
    for suite in ([r] if r.tag == "testsuite" else r.iter("testsuite")):
        t += int(suite.get("tests", 0)); f += int(suite.get("failures", 0))
        e += int(suite.get("errors", 0)); s += int(suite.get("skipped", 0))
ran = t - s
print(f"[junit] {a.dir}: files={len(files)} tests={t} ran={ran} failures={f} errors={e} skipped={s}")
problems = []
if f or e:
    problems.append("실패/오류 있음")
if ran < a.min:
    problems.append(f"실행 {ran}건 < 최소 {a.min}건")
if a.max_skipped >= 0 and s > a.max_skipped:
    problems.append(f"skipped {s} > 허용 {a.max_skipped}")
if problems:
    sys.exit("[junit] ✗ " + ", ".join(problems))
