# API error contract

The Sprint 1 API currently returns a stable HTTP status and a Vietnamese `message`. A machine-readable `code` is planned but not yet implemented; clients must not parse message text.

| Status | Meaning | Client behavior |
|---:|---|---|
| 400 | Invalid body, field, format or quantity | Keep user input and show validation guidance |
| 401 | Missing/expired session or invalid login | Clear local session state and request login |
| 403 | Authenticated but forbidden, or invalid CSRF token | Do not retry automatically; refresh authorization/CSRF state |
| 404 | Requested entity does not exist | Refresh the affected view |
| 409 | Duplicate, stale version, reused operation with different data, or resource in use | Preserve the operation and ask the user to reconcile |
| 500 | Unexpected server failure | Treat the operation as unconfirmed and avoid blind duplication |

Mutating requests use CSRF protection. Transaction endpoints that accept `Idempotency-Key` may safely return the prior response for an identical retry.
