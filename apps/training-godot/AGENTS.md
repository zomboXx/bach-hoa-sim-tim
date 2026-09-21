# Godot training guidance

- Training may write only training-session data through `/api/training/**`; it must not mutate sales, inventory or catalog data.
- Keep source assets and their provenance in `ASSETS.md`; generated Web export stays ignored under `apps/web/public/training/`.
- Preserve keyboard and touch controls and keep the headless tour test deterministic.
- Use Godot 4.6.3 with matching Web export templates unless an ADR deliberately upgrades it.

Run the import, `tests/tour_test.gd`, and Web export before handoff.
