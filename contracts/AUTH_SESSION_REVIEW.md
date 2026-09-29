# BE-02 — Session/RBAC review

- Status: **Draft implementation proposal**, 28/09/2026. Owner TV1; reviewer API TV2, web TV3.
- Issue: [BE-02 #5](https://github.com/zomboXx/bach-hoa-sim-tim/issues/5).
- Implementation: [PR #14](https://github.com/zomboXx/bach-hoa-sim-tim/pull/14), pending contract/code review and merge.
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

## Decisions proposed for implementation

- Use a 32-byte random opaque Bearer token, store only SHA-256 hash, default 8-hour expiry (configurable up to one day). No JWT, refresh token, cookie session or Basic authentication.
- Login takes organizationCode/storeCode/username/password. MVP UI can configure SIMTIM/MAIN; server does not infer tenant from a client-supplied UUID alone.
- ADMIN is store-scoped through user_roles like the other roles. Organization-wide administration is outside this API.
- Login failure is generic for wrong password, missing user, disabled/locked account or missing store assignment. BCrypt cost 12 hashes passwords; passwords are limited to 72 UTF-8 bytes to avoid truncation.
- Login/session/logout use no-store responses. Transport must use HTTPS outside local development. Client keeps tokens in memory, never in logs or URLs; FE-01 must decide user experience after reload during its review.
- CSRF is disabled because no credentials are accepted from cookies or browser Basic authentication. Cross-origin access remains disabled; use a same-origin deployment or development proxy. CORS changes require a separate explicit origin policy.
- Login is limited per server instance to 20 requests/minute per socket peer IP, with bounded memory and Retry-After 60. Forwarded headers are not trusted. A shared limit for multiple API instances belongs to deployment work.

## BE-03 integration

Code reads a SessionPrincipal from Spring Security Authentication. Its organizationId/storeId are the source of scope. During BE-03's transition, X-Organization-Id/X-Store-Id are accepted only when they exactly match the authenticated session; a mismatching header returns 403. Controllers should replace those temporary headers with the authenticated principal before their final integration. The token is opaque, so clients and catalog controllers must not parse JWT claims.

## Acceptance and evidence

Provider tests exercise real PostgreSQL session/password/role data through Spring Security and HTTP: wrong login, four-role read/write policy, training flag, hash-only token storage, logout/expiry, locked account, removed role, scope spoofing, malformed credentials and login rate limit. A test-only catalog controller exercises the actual security chain before BE-03 is integrated; it does not claim the catalog CRUD implementation is present here. Test credentials are isolated fixtures, never demo defaults.

Reviewers still need to accept the request/response shape and current permission matrix. Consumer tests and web adapter changes belong to FE-01. Mark this contract Accepted only after that review is recorded.

Local evidence, 28/09/2026: `pwsh -File scripts/verify.ps1` passed repository/link checks, web lint/format/build, 4 Playwright tests and 10 API tests. Flyway ran from an empty PostgreSQL 18.6 database and upgraded the disposable BE-01 V3 database to V4; the upgrade test suite also passed. Local Java 25 compiled with release 21; the local Docker daemon is unavailable.

CI evidence, 28/09/2026: [run 36404651237](https://github.com/zomboXx/bach-hoa-sim-tim/actions/runs/36404651237) for implementation commit `4df9338` passed Repository policy, Web baseline and API bootstrap. The API job ran `./mvnw verify` on Java 21 with a fresh PostgreSQL 17 Testcontainer. Passing CI does not replace provider/consumer review or mark this Draft contract Accepted.

Implementation references: [Spring Security 6.5 password storage](https://docs.spring.io/spring-security/reference/6.5/features/authentication/password-storage.html) and [servlet architecture](https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html).
