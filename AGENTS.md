# Agent operating guide

## Repository mission

Build a retail sales and inventory system with an isolated training experience. Preserve the distinction between an approved baseline, proposed designs and historical prototypes.

Before changing files, read `docs/README.md`, the relevant backlog item and the nearest applicable `AGENTS.md`.

## Sources of truth

Resolve repository-level conflicts in this order:

1. The Project Owner's current explicit decision.
2. The nearest applicable `AGENTS.md`.
3. The current sprint/backlog and accepted meeting decisions.
4. Approved requirements and design documents.
5. Integrated code and tests.
6. Drafts and historical documents.

Do not silently rewrite history. Mark old material as historical and record the replacement decision.

## Working agreement

The agent may inspect files, edit the requested scope and run local validation without asking. Ask before expanding product scope, adding a production dependency, making a breaking API/schema change, weakening security, deleting user data, deploying, pushing or merging unless the current task explicitly authorizes it.

- Preserve unrelated and uncommitted work.
- Do not claim a sprint, feature or test is complete without evidence.
- Keep deterministic formatting and build checks in CI rather than prose-only rules.
- Prefer small, reviewable changes and explain unavoidable coupling.

## Repository layout

- `prototype-v2/`: active Sprint 0 PWA baseline. Read `prototype-v2/AGENTS.md` before editing it.
- `docs/`: project records. Use `docs/README.md` to determine authority and status.
- `archive/`: read-only historical implementations; do not add features there.
- `.github/`: team workflow and CI.
- `.agents/skills/`: reusable repository workflows.

## Verification

For Sprint 0 frontend changes, run from `prototype-v2/`:

```text
npm run verify
```

Run narrower checks while iterating, but run the complete relevant gate before handoff. If a future backend or Godot module is present, follow its own checked-in instructions and CI rather than assuming Sprint 0 commands cover it.

## Documentation and handoff

- Update `CHANGELOG.md` under `Unreleased` for user-visible, architectural or workflow changes.
- Update `CONTRIBUTION_LOG.md` only after a human accepts responsibility, using its required separator, timestamp and diff-stat format.
- In the final handoff, report changed areas, commands run, failures or omissions, risks and decisions still needed.

## Code Review Rules

Prioritize correctness, data loss, authorization boundaries, inventory/price consistency, offline conflict behavior and breaking contracts. Training state must not mutate operational sales or inventory data. Do not raise style-only findings already enforced mechanically.
