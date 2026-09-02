# CrApp — Future Features Backlog

Ideas beyond the MVP described in [development-plan.md](development-plan.md). Nothing
here is scheduled — add to this list freely as things come up; prune or promote items
into active development as needed.

## Backlog

- [ ] **Good DevEx and snappy UI design** — keep the app fast and pleasant to build on
  and use: quick Gradle build/iteration times, minimal boilerplate, responsive Compose
  UI with no jank on the logging screens (these get used multiple times a day, so any
  friction compounds).

## Other ideas worth considering later

- Android watch feature
- Reminders/notifications (e.g. "no movement logged in over 24h").
- Photo attachment on bowel movement entries.
- Photo-based ingredient capture (OCR a label photo) for the Food Catalog, on top of
  the manual/pasted-text entry added in Phase 8 — needs a camera + text-recognition
  capability (e.g. ML Kit), a real dependency addition, so deferred until the
  text-entry version shows it's worth the jump.
- Multi-dog support (would require introducing a `Dog` entity and scoping all queries).
- Structured dose/amount fields (numeric + unit) once real usage data shows the free
  text fields are limiting analysis.
- Tap-to-inspect on the Phase 7 dashboard charts (exact date/value on tap, since a
  touch device has no hover) and a longer/adjustable time window than the current
  fixed last-14-points / last-7-days.
