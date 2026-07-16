# Project Rules Hub

This directory stores working rules for this project.
Open only the files you need for the current task.

## Quick Map
- `global/RULES.md`: Always-on rules that apply to every task.
- `planning/RULES.md`: Rules for discovery, scoping, and implementation plans.
- `implementation/RULES.md`: Rules for coding and file edits.
- `testing/RULES.md`: Rules for validating changes.
- `review/RULES.md`: Rules for self-review and code review.
- `release/RULES.md`: Rules for merge, release, and rollback readiness.
- `tasks/TEMPLATE.md`: Template for task-specific rule files.

## How To Use
1. Start with `global/RULES.md`.
2. Add one task file based on current work (planning, implementation, testing, review, release).
3. If the work has unique constraints, create a task rule file from `tasks/TEMPLATE.md`.
4. Keep rules concrete, testable, and short.

## Rule Quality Checklist
- Clear owner: who follows this rule.
- Trigger: when this rule applies.
- Action: exact behavior required.
- Verification: how we confirm it was followed.
