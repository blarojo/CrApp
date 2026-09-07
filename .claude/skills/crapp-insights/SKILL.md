---
name: crapp-insights
description: Analyze CrApp's exported CSVs (bowel movements, food + ingredients, medications, energy, walks) for progression and correlations, and write both a markdown report and a JSON file to upload into CrApp's Insights screen (the "Insights" link on Home, or Settings -> Insights -> Upload Report).
---

# CrApp Insights

CrApp (see `docs/development-plan.md`) is a personal Android app tracking a dog's
bowel movements, food, medications, energy levels, and walks. Its Export feature
(§8) produces five CSVs. This skill turns those into an insights report the app can
render in-app — see `docs/app-functionality.md` §13 for the full design, and
`docs/development-plan.md` Phase 8 for how the original (bowel/food/medication-only)
version of this was promoted from an idea to a working feature.

This is a one-shot, offline analysis: run it after the user hands you (or points you
at) a fresh export. It does not run inside the app or read the device directly, and
it produces **two files** every run — a markdown report for the user to read
directly, and a JSON file for CrApp's Insights screen to render.

## 1. Locate the input

Ask the user for the CSV files (or the folder they're in) if not already given —
they're produced by CrApp's Export screen and typically saved to Downloads or
shared via email/Drive. Proceed with whatever subset is available and note the gap
in both outputs' `dataCompleteness`/caveat text rather than failing outright.

## 2. Parse

Each CSV has a header row; RFC 4180 quoting applies (quoted fields may contain
commas/newlines — a real CSV parser handles this, don't split naively on `,`).
`timestamp` in every file is `yyyy-MM-dd HH:mm:ss` in the device's local time zone.

- `bowel_movements.csv`: `id, timestamp, consistency, color, has_blood, has_mucus,
  notes, amount, location, location_other, is_night_time, photo_uri` — `consistency`
  is the Purina Fecal Scoring Chart, 1 (very hard/dry) to 7 (liquid). `amount` is one
  of "Some drips" / "Medium amount" / "A lot of poo" (may be blank on older entries).
  `location` is `WALK` / `HOME` (displayed "Inside home") / `GARDEN`, or blank.
- `food_entries.csv`: `id, timestamp, food, brand, amount, meal_type, ingredients,
  amount_value, amount_unit` — `ingredients` is the catalog food's free-text
  ingredient list (may be blank if never recorded).
- `medication_entries.csv`: `id, timestamp, name, dose, notes, dose_value, dose_unit`
- `energy_entries.csv`: `id, timestamp, level, notes` — `level` is a named energy
  level (e.g. "Low energy" / "Normal" / "High energy" — read whatever values are
  actually present rather than assuming a fixed list).
- `walk_entries.csv`: `id, timestamp, bowel_movement_count, notes` — a **dog walker's**
  aggregate report: N movements happened sometime during that walk, with no
  individual timestamps, consistency scores, or other detail per movement. This is
  the app's one structurally incomplete data source (see the caveat below) — not
  every person who walks the dog uses the app, so some walk-time movements may be
  missing from `bowel_movements.csv` entirely and only show up here as a count.

## 3. Delegate the analysis to your strongest available model

The correlation-finding in step 4 is exactly the kind of reasoning task worth
spending the best model on, not whatever the current session happens to be running.
**Before analyzing, spawn a subagent via the `Agent` tool with `model: "opus"`**
(Anthropic's strongest available Claude model at time of writing — check what's
current if this skill is stale) and hand it the parsed CSV contents plus steps 3-4
of this file verbatim as its task. Do the CSV parsing and file-writing yourself
(no need to spend the best model's budget on I/O); let the subagent do the actual
progression/correlation reasoning and hand back its findings for you to write out.
If no model override is available in the current environment, do the analysis
directly instead of failing — note in the output that no model override was used.

## 4. Analyze

Compute what the data actually supports — don't force a finding that isn't there,
and say so plainly when the data is too sparse for a given kind of analysis (few
entries, short date range, all one food, etc.) rather than fabricating a trend. The
data is **never complete** by nature of how it's collected (see §2's walk-entries
note) — work with that rather than around it: a gap is a caveat to state, not a
reason to stay silent.

Organize every finding into exactly one of these two fixed sections (matches
`InsightSection` in the JSON schema below) — this is what the Insights screen groups
cards under:

### `bowel_progression` — Bowel movement progression

- **Frequency trend**: movements per day (or per week if the range is long), across
  the full available date range, folding in `walk_entries.csv` counts (movements
  happened, just without a consistency score) so the trend isn't undercounted.
- **Consistency trend**: consistency score over time, in chronological order —
  `walk_entries.csv` rows have no consistency score, so they contribute to frequency
  but not to this trend; say so if it materially changes what the trend can show.
- **What might explain a shift**: does a consistency or frequency change line up
  with anything in the other data — a specific food or ingredient introduced/removed
  around the same time (`food_entries.csv`'s `food`/`ingredients` columns), a feed
  *time* relative to movement time (e.g. movements reliably firmer/looser a
  consistent number of hours after a particular meal time), a medication started or
  stopped (`medication_entries.csv`), or a change in `location`/`is_night_time`
  patterns. Only claim a link when it recurs across multiple independent instances,
  not a single coincidence.

### `mood_food_correlation` — Mood, food & bowel correlations

- Read "mood" as `energy_entries.csv`'s `level`. Look for: energy level shifting
  around a food/ingredient/medication change; energy level correlating with bowel
  consistency or frequency (e.g. low-energy stretches coinciding with looser stools);
  a particular food consistently preceding an energy dip or lift within roughly the
  next 24h.
- Same evidentiary bar as above: recurring pattern across multiple independent
  instances, not a single coincidence, and say plainly when energy data is too sparse
  (few entries, short overlap with the food/bowel data) to support a claim.

**Every finding you do write** — in either section — gets `severity: "notable"` if
the pattern recurs across multiple independent instances, otherwise `"info"`. Note
in prose (in `detail`, or the report's `dataCompleteness`) that this is a
hand-inspected pattern in a small personal dataset, not a statistically validated
finding — a lead worth watching, not a diagnosis.

## 5. Write the reports

Write **two files**, both named from the analysis date, e.g. for 2026-09-07:

- `crapp_insights_2026-09-07.md` — a plain markdown report for the user to read
  directly: the summary paragraph, a "Bowel movement progression" heading with its
  findings, a "Mood, food & bowel correlations" heading with its findings, and the
  data-completeness caveat. This is the readable artifact; the JSON below is a
  structured mirror of the same content for the app.
- `crapp_insights_2026-09-07.json` — matching this exact schema (matches
  `com.crapp.data.insights.InsightsParser` — extra fields are ignored, but these
  names/types must be right or the app will reject the file):

```json
{
  "schemaVersion": 2,
  "generatedAt": "2026-09-07T21:00:00Z",
  "summary": "One short paragraph: overall picture, date range covered, anything the analysis couldn't cover.",
  "dataCompleteness": "One sentence on what's missing or uncertain, e.g. \"3 of 12 walk-time movements have no consistency score (dog-walker reports).\"",
  "insights": [
    {
      "title": "Short headline, e.g. \"Looser stools ~1 day after chicken treats\"",
      "detail": "1-3 sentences: the specific evidence (dates/counts), and how confident this is given the data.",
      "severity": "info",
      "section": "bowel_progression"
    }
  ],
  "series": [
    {
      "label": "Consistency over time",
      "kind": "line",
      "unit": "Purina score (1-7)",
      "points": [
        { "date": "2026-08-01", "value": 6 },
        { "date": "2026-08-03", "value": 5 }
      ]
    },
    {
      "label": "Movements per week",
      "kind": "bar",
      "unit": "count",
      "points": [
        { "date": "2026-W31", "value": 9 },
        { "date": "2026-W32", "value": 7 }
      ]
    }
  ]
}
```

Notes on the schema:
- `schemaVersion` must be present — use `2`. (`1` still parses for backward
  compatibility with reports from before `section`/`dataCompleteness` existed, but
  always write `2` going forward.)
- `insights[].section` is `"bowel_progression"` or `"mood_food_correlation"` — this
  is new in schema 2 and is how the app groups cards under the two headings above;
  anything else (including omitted) falls back to an "Other findings" group, so get
  this right rather than relying on the fallback.
- `insights[].severity` is `"info"` or `"notable"` (anything else falls back to `"info"`).
- `dataCompleteness` is optional but should be present whenever the data has a real
  gap worth naming (it almost always will — see §2's walk-entries note).
- `series[].kind` is `"line"` (a trend over time) or `"bar"` (magnitude per bucket,
  e.g. per day/week). `points[].date` is a short label string — it's displayed as
  the x-axis tick, not parsed as a real date by the app, so use whatever's
  most readable (`2026-08-01`, `2026-W31`, `Mon`, etc.) consistently within one series.
- `points[].value` is numeric (int or float both fine).
- Keep `insights` to the genuinely notable findings — a handful of good ones beats
  a long list restating the raw data. `series` similarly: 2-4 series is typical
  (e.g. consistency trend + frequency trend + an energy series, plus one more if a
  real correlation merits its own chart).

## 6. Hand off

Tell the user where both files were written. To view the JSON one: open CrApp ->
**Insights** (top of the Home screen, next to History, or via Settings)
-> **Upload Report** -> pick the `.json` file. The `.md` file is for reading directly
— mention it's there too, since not every finding worth writing makes it into a
short in-app card.

## Reusing this skill for an update

To refresh the in-app insights later (new export, more data since last time), just
re-run this skill against a fresh export — it always does a full pass over whatever
CSVs it's given, not an incremental diff, so there's no state to carry over between
runs. Re-uploading the new JSON in the app replaces the previously-uploaded one
(see `InsightsPreferences` — it keeps only the most recent report).
