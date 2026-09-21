---
name: verify-sim-tim
description: Verify changes in the Bách Hóa Sim Tím repository before review or merge. Use after implementation or when reporting readiness; do not use for a read-only product question.
---

# Verify Sim Tím

Inspect the current diff and map changed files to the relevant module. Read the root and nearest module `AGENTS.md`, then run the narrowest checks that cover the change.

For a full Sprint 1 gate, run `pwsh -File scripts/verify.ps1` from the repository root. During iteration, use the nearest module checks documented in its `AGENTS.md`. If only documentation or repository metadata changed, still run `git diff --check` and `scripts/check-markdown-links.ps1`.

Do not fix unrelated failures or delete artifacts merely to obtain a green result. Stop and report when verification requires missing credentials, unavailable external services or a destructive reset.

Return:

- checks passed;
- checks failed with the useful error;
- checks not run and why;
- remaining risks and any owner decision required.
