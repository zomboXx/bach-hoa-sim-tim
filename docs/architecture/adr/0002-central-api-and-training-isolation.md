# ADR 0002: Central API with isolated training state

- Status: Accepted
- Date: 2026-09-21
- Owner: Nguyễn Đức Phát

## Context

Operational sales and inventory need one transactional authority. Training must resemble the workflow without creating invoices or changing stock.

## Decision

Spring Boot/PostgreSQL is the authority for operational state. Vue consumes same-origin `/api` endpoints. Godot is exported into the web shell but writes only through `/api/training/**`. Training sessions are stored separately from operational tables.

Shared HTTP and authorization behavior is versioned in `contracts/`. Browser role checks never replace server authorization.

## Consequences

Training can be deployed with the PWA while remaining data-isolated. Cross-module contract changes require API and client review. Full browser acceptance requires PostgreSQL, the API and a Godot Web export.
