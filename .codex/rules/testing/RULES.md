# Testing Rules

## When To Open
Use when validating implementation changes.

## Rules
1. Test changed behavior at the nearest useful level first (unit/integration/e2e).
2. Cover at least one success path and one failure/edge path.
3. Prefer deterministic tests over timing-dependent tests.
4. Keep tests isolated; avoid hidden dependencies on local state.
5. If tests are skipped, record why and remaining risk.

## Minimum Validation
1. Run targeted tests for edited areas.
2. Run broader smoke checks when cross-cutting code changes.
3. Verify logs/errors are understandable for failures.
