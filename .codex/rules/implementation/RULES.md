# Implementation Rules

## When To Open
Use when editing code, configs, scripts, or docs.

## Rules
1. Follow existing project patterns unless there is a clear reason not to.
2. Keep diffs focused; avoid opportunistic refactors.
3. Add or update comments only where logic is non-obvious.
4. Prefer pure, deterministic functions where practical.
5. Handle errors explicitly with actionable messages.
6. Keep interfaces stable; if changed, update all callers in the same change.
7. Update related docs/config when behavior changes.

## Safety Checks
- No debug code or temporary flags left behind.
- No hardcoded secrets or environment-specific paths.
- No dead code added without a ticketed follow-up.
