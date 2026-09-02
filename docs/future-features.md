# CrApp — Future Features Backlog

Ideas beyond the MVP described in [development-plan.md](development-plan.md). Nothing
here is scheduled — add to this list freely as things come up; prune or promote items
into active development as needed.

## Backlog

- [ ] Ensure we have a way to backup an wipe the data from the app screen, maybe add an admin screen
- [ ] **Good DevEx and snappy UI design** — keep the app fast and pleasant to build on
  and use: quick Gradle build/iteration times, minimal boilerplate, responsive Compose
  UI with no jank on the logging screens (these get used multiple times a day, so any
  friction compounds).

- [ ] **CSV export → Claude analysis skill → in-app dashboard upload** — extend the
  CSV export (development-plan.md §8) with a Claude skill that ingests the exported
  CSV(s) and surfaces findings (frequency trends, consistency trends over time,
  correlations with logged events). Output of that analysis gets loaded back into the
  app as a file upload and rendered as a dashboard — first real analytics/insights
  surface for the app.

- [ ] **Food ingredient upload → correlate with bowel movements** — allow uploading the
  ingredient list for a given food (photo/text of the label, or manual entry), so
  ingredients can be linked to food entries. Combined with logged bowel movements, this
  opens the door to surfacing correlations between specific ingredients/medications and
  changes in stool consistency or frequency — the most clinically useful long-term
  feature for identifying triggers.

## Other ideas worth considering later

- Reminders/notifications (e.g. "no movement logged in over 24h").
- Photo attachment on bowel movement entries.
- Multi-dog support (would require introducing a `Dog` entity and scoping all queries).
- Structured dose/amount fields (numeric + unit) once real usage data shows the free
  text fields are limiting analysis.
- Backup/restore of the local database (e.g. export/import the Room DB file directly)
  in case of phone loss or reset.
- Tap-to-inspect on the Phase 7 dashboard charts (exact date/value on tap, since a
  touch device has no hover) and a longer/adjustable time window than the current
  fixed last-14-points / last-7-days.
