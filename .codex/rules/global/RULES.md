# Global Rules

## Scope
Applies to every change in this repository.

## Core Rules
1. Make the smallest safe change that solves the problem.
2. Do not silently change behavior outside the requested scope.
3. Keep backward compatibility unless a breaking change is explicitly approved.
4. Never commit secrets, tokens, or private keys.
5. Prefer readability over cleverness.
6. Document non-obvious decisions in PR notes or task logs.

## Operational Rules
1. Check current git status before edits and before final handoff.
2. Avoid destructive commands (`reset --hard`, force delete) unless explicitly approved.
3. Preserve unrelated existing changes.
4. Use consistent naming and existing project conventions.

## Execution Governance
1. Every task must be led by a single lead coordinator acting as principal owner.
2. Before execution, the lead coordinator must create sub-agents and assign roles from `.codex/agents` TOML definitions based on task fit.
3. The lead coordinator must collect each sub-agent's output, review quality, provide feedback, and request revisions when needed.
4. Only finalized, reviewed deliverables are presented to the user by the lead coordinator.

## Single-Task Policy
1. Execute only one task type at a time (for example: implementation, refactoring, or testing).
2. Do not run multiple task types in parallel within the same work cycle.
3. Sub-agents must not be used to bypass this policy by performing multiple different task types at once.

## Rule-Reading Order (Mandatory)
1. Read `global/RULES.md` before starting any task.
2. Then read the task-specific rule file required for the current work (planning, implementation, testing, review, or release).
3. Start work only after both rule layers are reviewed.

## Done Criteria
- Requested outcome is implemented.
- Relevant checks/tests were run or explicitly marked as not run.
- Risks and assumptions are stated.
