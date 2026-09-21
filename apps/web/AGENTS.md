# Frontend guidance

This directory contains the active Vue PWA.

- Use Vue 3 Composition API and strict TypeScript already present in the project.
- Keep transport in `src/shared/server.ts` and shared domain types in `src/shared/api.ts`; components should not duplicate transaction rules.
- Put product workflows under `src/features/<feature>/`; keep `App.vue` as composition/navigation glue.
- Preserve integer VND calculations, FEFO behavior and the separation between training and operational data.
- Treat frontend role checks as presentation only; authorization remains in `services/api/`.
- Service Worker changes may cache public same-origin assets, but must not imply authenticated API caching.
- Preserve keyboard, pointer and touch flows for the training experience.
- Do not develop features in `archive/`.

Run `npm run build` while iterating and `npm run verify` before handoff. Update Playwright tests when changing a user-observable workflow.
