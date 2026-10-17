#!/usr/bin/env bash
# 저장소 훅(.githooks)을 활성화한다. worktree마다 공유된다(core.hooksPath는 저장소 설정).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
chmod +x .githooks/* scripts/gate/*.sh scripts/gate/*.py
git config core.hooksPath .githooks
echo "core.hooksPath=.githooks 설정 완료 (pre-commit: 정책, pre-push: 게이트)"
