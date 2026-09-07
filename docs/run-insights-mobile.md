# Running CrApp Insights from the Claude phone app (no computer needed)

The `crapp-insights` skill (`.claude/skills/crapp-insights/SKILL.md`) is a Claude
*Code* construct — it only auto-loads inside a Claude Code session that has this
repo open, which the Claude phone app doesn't have. But nothing about the actual
analysis requires Claude Code: a plain chat with the CSVs attached and the right
instructions pasted in does the same job. This doc is that self-contained version —
copy/paste the prompt below into a new chat in the Claude app, and you never need to
touch a computer. See [run-insights.md](run-insights.md) for the computer-based
version this mirrors.

## Steps

1. **Export from CrApp**: Home → **Export** → share sheet → choose the Claude app as
   the share target (if it's not offered directly, save the 5 CSVs somewhere first —
   e.g. Files/Downloads — then attach them to a new Claude chat manually).
2. **Start a new chat** in the Claude app and pick the **strongest model available on
   your plan** from the model picker before sending anything — there's no way to
   delegate to a stronger model mid-conversation the way Claude Code can, so this
   step matters and has no automatic fallback here.
3. **Attach the 5 CSVs** (`bowel_movements.csv`, `food_entries.csv`,
   `medication_entries.csv`, `energy_entries.csv`, `walk_entries.csv` — whatever
   subset you have is fine) to your first message, and **paste the entire prompt
   below** as the message text.
4. Claude replies with a markdown report plus a JSON code block. Read the report
   right there in the chat.
5. **Save the JSON**: copy just the JSON code block's contents, then create a new
   file on your phone with a `.json` extension (a plain text/notes app that lets you
   name the file, or your phone's Files app "create file" option both work) and paste
   it in. Name doesn't matter — anything like `crapp_insights.json` is fine.
6. **Upload it**: CrApp → **Insights** (top of Home, next to History and Export) →
   **Upload Report** → pick the file you just saved.

## The prompt to paste

Copy everything between the lines below into your message, along with the CSV
attachments:

---

I'm attaching CSV exports from CrApp, a personal app tracking my dog's bowel
movements, food, medications, energy levels, and walks. Analyze them and give me
two things: a markdown report I can read directly in this chat, and a JSON code
block matching the exact schema below (I'll upload the JSON into the app).

**CSV columns** (header row, RFC 4180 quoting, `timestamp` is `yyyy-MM-dd HH:mm:ss`
local time):
- `bowel_movements.csv`: id, timestamp, consistency, color, has_blood, has_mucus,
  notes, amount, location, location_other, is_night_time, photo_uri — consistency is
  the Purina Fecal Scoring Chart, 1 (very hard/dry) to 7 (liquid); amount is "Some
  drips" / "Medium amount" / "A lot of poo"; location is WALK / HOME / GARDEN / blank.
- `food_entries.csv`: id, timestamp, food, brand, amount, meal_type, ingredients,
  amount_value, amount_unit.
- `medication_entries.csv`: id, timestamp, name, dose, notes, dose_value, dose_unit.
- `energy_entries.csv`: id, timestamp, level, notes — level is a named energy level
  (read whatever values are actually present).
- `walk_entries.csv`: id, timestamp, bowel_movement_count, notes — a dog-walker's
  aggregate report: N movements happened sometime during the walk, no individual
  timestamps or consistency scores per movement. This is the one structurally
  incomplete data source (not everyone who walks the dog uses the app) — treat that
  as a caveat to state plainly, not a reason to go quiet.

**Analysis**: compute what the data actually supports — say plainly when it's too
sparse for a given kind of analysis (few entries, short date range, one food, etc.)
rather than fabricating a trend. Organize every finding into exactly one of two
sections:
- `bowel_progression` — frequency trend (fold in walk_entries counts) and
  consistency trend (walk_entries have no score, so they count toward frequency but
  not consistency), plus whether a shift lines up with a food/ingredient change,
  feed timing relative to movement time, a medication change, or a
  location/night-time pattern change.
- `mood_food_correlation` — energy level (the "mood" signal) against food/ingredient
  changes, and against bowel consistency/frequency.

Only claim a link when it recurs across multiple independent instances, not a single
coincidence — mark those `"severity": "notable"`; everything else (including single
coincidences worth naming as a caveat) is `"info"`. Say explicitly that this is a
hand-inspected pattern in a small personal dataset, not a statistically validated
finding.

**JSON schema** (write this as a code block after the markdown report — extra fields
are ignored, but these names/types must be exactly right):

```json
{
  "schemaVersion": 2,
  "generatedAt": "<ISO 8601 timestamp>",
  "summary": "One short paragraph: overall picture, date range, what the analysis couldn't cover.",
  "dataCompleteness": "One sentence on what's missing or uncertain in this specific export.",
  "insights": [
    {
      "title": "Short headline",
      "detail": "1-3 sentences: the specific evidence (dates/counts) and how confident this is.",
      "severity": "info",
      "section": "bowel_progression"
    }
  ],
  "series": [
    {
      "label": "Consistency over time",
      "kind": "line",
      "unit": "Purina score (1-7)",
      "points": [ { "date": "2026-08-01", "value": 6 }, { "date": "2026-08-03", "value": 5 } ]
    },
    {
      "label": "Movements per week",
      "kind": "bar",
      "unit": "count",
      "points": [ { "date": "2026-W31", "value": 9 } ]
    }
  ]
}
```

`severity` is `"info"` or `"notable"`. `section` is `"bowel_progression"` or
`"mood_food_correlation"` (anything else groups under an "Other" heading in the
app, so get this right). `kind` is `"line"` or `"bar"`; `points[].date` is a short
display label, not parsed as a real date, so use whatever's most readable
consistently within one series. Keep `insights` to genuinely notable findings —
a handful of good ones beats a long list restating the raw data.

---

## Doing this again later

Same steps, fresh export. There's no state carried between runs — each pass is a
full re-analysis of whatever CSVs you attach that time. Uploading a new JSON in
the app replaces the previously-uploaded one.
