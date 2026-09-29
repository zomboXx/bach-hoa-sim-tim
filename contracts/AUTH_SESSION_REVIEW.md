# BE-02 — Session/RBAC review

- Status: **Accepted**, 29/09/2026. Owner API TV1; reviewer API TV2, consumer owner TV3, consumer reviewer TV1.
- Issue: [BE-02 #5](https://github.com/zomboXx/bach-hoa-sim-tim/issues/5).
- Provider: [PR #14](https://github.com/zomboXx/bach-hoa-sim-tim/pull/14), approved by TV2 and merged into main at `9cd293d`.
- Consumer: [TV1 approval of TV3's FE-01 implementation](https://github.com/zomboXx/bach-hoa-sim-tim/pull/12#pullrequestreview-5347924990). PR #12 still awaits integration and independent approval after the latest push.
- Wire contract: [OpenAPI 3.1](auth-session.openapi.yaml).
- The Project Owner's four-role decision replaces the old three-role wording in Issue #5. Team approval of DB-01 does not imply approval of this API contract.

## Current permission matrix

| Server role | Catalog GET | Catalog POST/PUT/DELETE | Login/session/logout |
|---|---|---|---|
| SALES | catalog.read | Denied | Own session |
| STOCK | catalog.read | catalog.write | Own session |
| MANAGER | catalog.read | catalog.write | Own session |
| ADMIN | catalog.read | catalog.write | Own session |

This matrix covers the existing Sprint 1 catalog paths: categories, units, products, suppliers. There is no catch-all ADMIN bypass and no training role. Training access is a boolean employee state, separate from operational permissions. Sales, inventory, account administration and training APIs receive their policies with their owning backlog items; unmatched routes are denied.

Roles and permission grants live in IAM tables; every request reloads them for the session's organization/store. V4 installs the permission codes and grants for existing roles. The repeatable demo seed applies the same grants to demo roles on a fresh DB. New organization provisioning must create its roles and grants when that feature is implemented.

## Accepted decisions

- Use a 32-byte random opaque Bearer token, store only SHA-256 hash, default 8-hour expiry (configurable up to one day). No JWT, refresh token, cookie session or Basic authentication.
- Login takes organizationCode/storeCode/username/password. MVP UI can configure SIMTIM/MAIN; server does not infer tenant from a client-supplied UUID alone.
- ADMIN is store-scoped through user_roles like the other roles. Organization-wide administration is outside this API.
- Login failure is generic for wrong password, missing user, disabled/locked account or missing store assignment. BCrypt cost 12 hashes passwords; passwords are limited to 72 UTF-8 bytes to avoid truncation.
- Login/session/logout use no-store responses. Transport must use HTTPS outside local development. Client keeps tokens in memory, never in logs or URLs; FE-01 requires login again after reload, as accepted in the consumer review.
- CSRF is disabled because no credentials are accepted from cookies or browser Basic authentication. Cross-origin access remains disabled; use a same-origin deployment or development proxy. CORS changes require a separate explicit origin policy.
- Login is limited per server instance to 20 requests/minute per socket peer IP, with bounded memory and Retry-After 60. Forwarded headers are not trusted. A shared limit for multiple API instances belongs to deployment work.

## BE-03 integration

Code reads a SessionPrincipal from Spring Security Authentication. Its organizationId/storeId are the source of scope. During BE-03's transition, X-Organization-Id/X-Store-Id are accepted only when they exactly match the authenticated session; a mismatching header returns 403. Controllers should replace those temporary headers with the authenticated principal before their final integration. The token is opaque, so clients and catalog controllers must not parse JWT claims.

## Acceptance and evidence

Provider tests exercise real PostgreSQL session/password/role data through Spring Security and HTTP: wrong login, four-role read/write policy, training flag, hash-only token storage, logout/expiry, locked account, removed role, scope spoofing, malformed credentials and login rate limit. The test-only catalog controller uses `/api/v1/products/__be02_security_fixture` under the existing catalog policy so it can run alongside BE-03's real controllers. Test credentials are isolated fixtures, never demo defaults.

The provider and consumer reviews above accept the request/response shape and permission matrix. The 28/09/2026 Draft is superseded by this acceptance record; paths, payloads and security requirements are unchanged. Acceptance of the contract does not claim PR #12 is merged or the sprint is complete.

BE-03 integration tests now create their own STOCK/SALES credentials and log in through the real HTTP endpoint. CRUD requests carry the returned Bearer token. Additional cases verify unauthenticated catalog requests, SALES write denial and organization/store scope spoofing without bypassing Spring Security. Fixture accounts and sessions are removed at suite teardown.

## Integration verification — 29/09/2026

On PR #12 based on main `6ebf601` plus the test fixes: root `pwsh -File scripts/verify.ps1` passed repository policy, Markdown links, lint, formatting, typecheck/build, 6 demo tests, 23 auth/consumer tests and 30 API tests (10 auth/bootstrap, 20 catalog). All API tests used real PostgreSQL; catalog requests used sessions obtained over HTTP. Six live PWA/backend cases also passed: four roles, logout/revocation, reload with no persistent token and invalid credentials. Local Java 25 release 21/PostgreSQL 18.6 used fresh disposable databases; CI Java 21/PostgreSQL 17 verification is pending publication. The initial local live-test harness lacked its ESM setting; after correcting the ignored harness configuration all six cases passed without product changes.

## Historical implementation evidence — 28/09/2026

Local evidence, 28/09/2026: `pwsh -File scripts/verify.ps1` passed repository/link checks, web lint/format/build, 4 Playwright tests and 10 API tests. Flyway ran from an empty PostgreSQL 18.6 database and upgraded the disposable BE-01 V3 database to V4; the upgrade test suite also passed. Local Java 25 compiled with release 21; the local Docker daemon is unavailable.

CI evidence, 28/09/2026: [run 36404651237](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/36404651237) for implementation commit `4df9338` passed Repository policy, Web baseline and API bootstrap. The API job ran `./mvnw verify` on Java 21 with a fresh PostgreSQL 17 Testcontainer. Passing CI does not replace provider/consumer review or mark this Draft contract Accepted.

Implementation references: [Spring Security 6.5 password storage](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html) and [servlet architecture](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html).
