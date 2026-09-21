# API guidance

- Keep controllers thin; transaction, inventory and pricing rules belong in `StoreService`.
- Flyway migrations are append-only after merge. Never edit an applied migration to change production data.
- Preserve integer VND, FEFO, idempotency keys and optimistic-version checks.
- Authorization must be enforced server-side and reflected in `contracts/role-permissions.md`.
- A breaking endpoint or schema change requires an updated OpenAPI contract and web consumer review.
- Tests use a separate PostgreSQL database through `TEST_DB_URL`; never point tests at operational data.

Run `mvn -B -ntp -f services/api/pom.xml verify` from the repository root after starting PostgreSQL and creating `simtim_test`.
