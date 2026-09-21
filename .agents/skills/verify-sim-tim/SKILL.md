---
name: verify-sim-tim
description: Verify changes in the Bách Hóa Sim Tím repository before review or merge. Use after implementation or when reporting readiness; do not use for a read-only product question.
---

# Verify Sim Tím

Inspect the current diff and map changed files to the relevant module. Read the root and nearest module `AGENTS.md`, then run the narrowest checks that cover the change.

For the Sprint 0 PWA baseline, run `npm run verify` from `prototype-v2/`. If only documentation or repository metadata changed, still run `git diff --check` and validate links/paths affected by moves.

Do not fix unrelated failures or delete artifacts merely to obtain a green result. Stop and report when verification requires missing credentials, unavailable external services or a destructive reset.

Return:

- checks passed;
- checks failed with the useful error;
- checks not run and why;
- remaining risks and any owner decision required.
