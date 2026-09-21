# Frontend guidance

This directory contains the approved Sprint 0 browser baseline and the starting web client for Sprint 1.

- Use Vue 3 Composition API and strict TypeScript already present in the project.
- Keep operational state access behind `src/api.ts`; components should not duplicate transaction rules.
- Preserve integer VND calculations, FEFO behavior and the separation between training and operational data.
- Frontend roles are a demo boundary only; do not describe them as server-side security.
- During Sprint 1, introduce a server adapter behind the existing boundary; do not remove the demo adapter until API E2E tests replace the baseline workflows.
- Service Worker changes may cache public same-origin assets, but must not imply authenticated API caching.
- Preserve keyboard, pointer and touch flows for the training experience.
- Do not develop features in `archive/prototype-v1/`.

Run `npm run build` while iterating and `npm run verify` before handoff. Update Playwright tests when changing a user-observable workflow. ESLint and Prettier are mechanical gates; do not bypass them with broad disable comments.
