# Frontend guidance

This directory contains the active Sprint 0 browser prototype.

- Use Vue 3 Composition API and strict TypeScript already present in the project.
- Keep operational state access behind `src/api.ts`; components should not duplicate transaction rules.
- Preserve integer VND calculations, FEFO behavior and the separation between training and operational data.
- Frontend roles are a demo boundary only; do not describe them as server-side security.
- Service Worker changes may cache public same-origin assets, but must not imply authenticated API caching.
- Preserve keyboard, pointer and touch flows for the training experience.
- Do not develop features in `archive/prototype-v1/`.

Run `npm run build` while iterating and `npm run verify` before handoff. Update Playwright tests when changing a user-observable workflow.
