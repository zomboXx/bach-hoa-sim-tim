# API module guidance

Follow the root `AGENTS.md` and accepted ADR 0002/0003. Keep packages by business feature. Controllers are HTTP adapters; application services own transactions; domain rules do not depend on HTTP or JPA.

Run `./mvnw verify` (or `mvnw.cmd verify` on Windows) before handoff. Integration tests require a working Docker daemon and start PostgreSQL 17 through Testcontainers. New migrations must be forward-only and tested from an empty database. Never commit local credentials or sample production data.
