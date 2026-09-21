# ADR 0001: Organize the repository by deployable unit

- Status: Accepted
- Date: 2026-09-21
- Owner: Nguyễn Đức Phát

## Context

The Sprint 1 work introduced a Vue client, Spring Boot API, Godot training client, infrastructure and extensive project records. Top-level technology names and mixed documents made ownership and automation ambiguous.

## Decision

Use `apps/` for user-facing clients, `services/` for server processes, `contracts/` for shared interfaces, `infra/` for local infrastructure, `docs/` by document purpose, `scripts/` for supported entry points, `tools/` for maintainer utilities and `archive/` for historical implementations.

Each active module has a nearest `AGENTS.md`; cross-module changes follow the root guide and CODEOWNERS.

## Consequences

Paths and CI must be updated together. Historical documents remain historical; links in active documents follow the new layout. New top-level product folders require an ADR.
