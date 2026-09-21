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

- `apps/web/`: Vue PWA. Read `apps/web/AGENTS.md` before editing it.
- `apps/training-godot/`: isolated Godot training client. Read its local `AGENTS.md`.
- `services/api/`: Spring Boot API and Flyway migrations. Read its local `AGENTS.md`.
- `contracts/`: reviewed HTTP, error and authorization contracts shared by clients and API.
- `docs/`: project records. Use `docs/README.md` to determine authority and status.
- `infra/`: local infrastructure definitions; never commit real credentials or data.
- `scripts/`: supported repository entry points for setup, development and verification.
- `tools/`: maintainers' reproducible document/asset utilities, not runtime code.
- `archive/`: read-only historical implementations; do not add features there.
- `.github/`: team workflow and CI.
- `.agents/skills/`: reusable repository workflows.

## Verification

Run the repository gate from the root:

```text
pwsh -File scripts/verify.ps1
```

Run narrower module checks while iterating, but run the complete relevant gate before handoff. A contract change requires both provider and consumer checks.

## Documentation and handoff

- Update `CHANGELOG.md` under `Unreleased` for user-visible, architectural or workflow changes.
- Update `CONTRIBUTION_LOG.md` only after a human accepts responsibility, using its required separator, timestamp and diff-stat format.
- In the final handoff, report changed areas, commands run, failures or omissions, risks and decisions still needed.

## Code Review Rules

Prioritize correctness, data loss, authorization boundaries, inventory/price consistency, offline conflict behavior and breaking contracts. Training state must not mutate operational sales or inventory data. Do not raise style-only findings already enforced mechanically.
