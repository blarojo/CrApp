---
name: crapp-insights
description: Analyze CrApp's exported CSVs (bowel movements, food + ingredients, medications) for trends and possible correlations, and write an insights report JSON file to upload back into CrApp's Insights screen (Settings -> Insights -> Upload Report).
---

# CrApp Insights

CrApp (see `docs/development-plan.md`) is a personal Android app tracking a dog's
bowel movements, food, and medications. Its Export feature (§8) produces three CSVs
(`bowel_movements.csv`, `food_entries.csv`, `medication_entries.csv`). This skill
turns those into an insights report the app can render as an in-app dashboard --
promoting the "CSV export -> Claude analysis skill -> in-app dashboard upload" idea
from `docs/future-features.md` to a working feature (Phase 8).

This is a one-shot, offline analysis: run it after the user hands you (or points you
at) a fresh export. It does not run inside the app or read the device directly.

## 1. Locate the input

Ask the user for the three CSV files (or the folder they're in) if not already
given -- they're produced by CrApp's Export screen and typically saved to Downloads
or shared via email/Drive. All three files are expected; if one is missing, proceed
with what's available and note the gap in the summary rather than failing.

## 2. Parse

Each CSV has a header row; RFC 4180 quoting applies (quoted fields may contain
commas/newlines -- a real CSV parser handles this, don't split naively on `,`).

- `bowel_movements.csv`: `id, timestamp, consistency, color, has_blood, has_mucus, notes`
  -- `consistency` is the Purina Fecal Scoring Chart, 1 (very hard/dry) to 7 (liquid).
  `timestamp` is `yyyy-MM-dd HH:mm:ss` in the device's local time zone.
- `food_entries.csv`: `id, timestamp, food, brand, amount, meal_type, ingredients`
  -- `ingredients` is a free-text ingredient list (may be blank if the food's
  ingredients were never recorded in the app's Food Catalog).
- `medication_entries.csv`: `id, timestamp, name, dose, notes`

## 3. Analyze

Compute what the data actually supports -- don't force a finding that isn't there,
and say so plainly when the data is too sparse for a given kind of analysis (few
entries, short date range, all one food, etc.) rather than fabricating a trend.

- **Frequency trend**: bowel movements per day (or per week if the range is long),
  across the full available date range.
- **Consistency trend**: consistency score over time, in chronological order.
- **Correlations worth flagging** (each becomes an `insights` entry, `severity:
  "notable"` if the pattern recurs across multiple independent instances, otherwise
  `"info"`):
  - A specific food, ingredient, or medication that reliably precedes a
    consistency-score shift (worse *or* better) within roughly the next 24-48h.
  - A shift in typical consistency or frequency around a switch between two foods
    (e.g., a formulation or brand change visible in `food_entries.csv`).
  - Repeated co-occurrence of `has_blood`/`has_mucus` with a particular recent food
    or medication.
  - Only claim a correlation when it recurs across multiple independent instances in
    the data, not a single coincidence -- and note that this is a hand-inspected
    pattern in a small personal dataset, not a statistically validated finding, so
    it's a lead worth watching, not a diagnosis.

## 4. Write the report

Write a JSON file (`crapp_insights_<date>.json`) matching this exact schema (matches
`com.crapp.data.insights.InsightsParser` -- extra fields are ignored, but these
names/types must be right or the app will reject the file):

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-09-02T21:00:00Z",
  "summary": "One short paragraph: overall picture, date range covered, anything the analysis couldn't cover.",
  "insights": [
    {
      "title": "Short headline, e.g. \"Looser stools ~1 day after chicken treats\"",
      "detail": "1-3 sentences: the specific evidence (dates/counts), and how confident this is given the data.",
      "severity": "info"
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
- `schemaVersion` must be present and `1` -- the app uses it as the file-format marker.
- `insights[].severity` is `"info"` or `"notable"` (anything else falls back to `"info"`).
- `series[].kind` is `"line"` (a trend over time) or `"bar"` (magnitude per bucket,
  e.g. per day/week). `points[].date` is a short label string -- it's displayed as
  the x-axis tick, not parsed as a real date by the app, so use whatever's
  most readable (`2026-08-01`, `2026-W31`, `Mon`, etc.) consistently within one series.
- `points[].value` is numeric (int or float both fine).
- Keep `insights` to the genuinely notable findings -- a handful of good ones beats
  a long list restating the raw data. `series` similarly: 2-4 series is typical
  (e.g. consistency trend + frequency trend, plus one more if a real correlation
  merits its own chart).

## 5. Hand off

Tell the user where the file was written, and that to view it: open CrApp -> the
gear icon (Settings) -> **Insights** -> **Upload Report** -> pick this file.
