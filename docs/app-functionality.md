# CrApp — Functionality Reference

A collated, always-current list of everything the app actually does, grouped by
area rather than by when it was built (that history lives in
[development-plan.md](development-plan.md)) or what's still an idea
([future-features.md](future-features.md)). Update this file whenever a feature is
added or changed — it's meant to be the one place that answers "does CrApp do X?"
without archaeology through commits or specs.

**Status key:** ✅ shipped and expected to work · 🧪 shipped but not yet click-tested
on a real device (see [Testing status](#testing-status) at the bottom before relying
on it) · — not implemented.

## 1. Bowel movement logging

Screen: **Log/Edit Bowel Movement** (`BowelMovementLogScreen`).

| Field | Status | Notes |
|---|---|---|
| Timestamp | ✅ | Defaults to now; editable date + time pickers. |
| Consistency (1–7) | ✅ | Purina Fecal Scoring Chart, tap-to-select with an icon + description per score. Chips are ordered **7 down to 1** (highest/loosest first) rather than 1 up to 7, since a dog whose scores cluster at the high end shouldn't have to scroll to reach them. A "What do these scores mean?" link opens [Purina's own reference chart](https://vetcentre.purina.co.uk/news-articles/faecal-score-chart) in the browser. |
| Amount | 🧪 | Three-option tap selector: **Some drips** / **Medium amount** / **A lot of poo**. Independent of consistency — a small amount can still be liquid. Optional. |
| When / Where | 🧪 | Four tap chips: **Night** / **Walk** / **Inside home** / **Garden**. **Night** is an independent toggle (can be combined with any location) pre-filled from a configurable night window (default 10pm–6am) but always user-overridable; **Walk** / **Inside home** / **Garden** set the location and are mutually exclusive. (The `Location.OTHER` value and its free-text field still exist in the data model for backward compatibility but are no longer exposed in this UI.) |
| Blood / mucus present | ✅ | Two checkboxes. |
| Notes | ✅ | Free text. |
| Photo | 🧪 | "Take Photo" opens the device camera, saving into a shared, user-visible **`Pictures/CrApp`** album via `MediaStore` — deliberately *not* app-private storage, so photos survive an uninstall/reinstall. Thumbnail shown inline (decoded on-device, no image-loading library); tap "Remove" to delete. |
| Color | ✅ | Free text (pre-existing). |

Edit and delete both work from History; deleting an entry with a photo also deletes
the underlying `MediaStore` file.

## 2. Food logging + Food Catalog

Screens: **Log/Edit Food** (`FoodLogScreen`), **Food Catalog** (`FoodCatalogScreen`,
via Settings).

- Pick from a dropdown of previously-used foods (most-recently-used first) or add a
  new one inline without leaving the log flow. ✅
- Meal type: Meal / Treat chip select. ✅
- Amount: free text (e.g. "1/2 cup"). ✅
- **Structured amount** 🧪 — additive numeric value + unit (cup / tbsp / g / tin (400g)) fields
  alongside the free-text amount, for future analysis that needs real numbers
  instead of parsing prose.
- Food Catalog lets you add/edit a food's ingredients as free text (manual entry or
  pasted from a label). ✅
- **4 starter foods are pre-seeded** on a brand-new install (Hill's z/d Mini dry,
  Hill's z/d wet, Purina Pro Plan HA Mousse, Purina Pro Plan HA Dry) with their
  real ingredient labels already filled in. ✅
- **Structured ingredients** 🧪 (backend only, no dedicated screen yet) — every
  food's ingredient text is parsed in the background into a normalized
  ingredient catalog + per-food join table (`Ingredient` / `FoodIngredient`), so a
  future insights feature can correlate individual ingredients against symptoms
  instead of string-matching label text. Runs automatically on every app start;
  a small known-synonym list (e.g. "corn starch" ↔ "maize starch") keeps the same
  real ingredient from fragmenting into duplicate rows across the 4 seeded labels.
  No UI surfaces this data yet — it's plumbing for later.

## 3. Medication logging

Screen: **Log/Edit Medication** (`MedicationLogScreen`).

- Name, dose (free text), notes. ✅
- **Structured dose** 🧪 — additive numeric value + unit (mg / ml / mcg) fields
  alongside the free-text dose.

## 4. Energy logging *(new)*

Screen: **Log/Edit Energy** (`EnergyLogScreen`), reachable from the `+` menu.

- A 5-point named scale — **Slept all day / Low energy / Normal / A bit playful / A
  lot of energy** — tap to select (no raw numbers shown). 🧪
- Optional notes. 🧪
- Shows in History (its own filter chip) and is included in CSV export and
  backup/restore. 🧪

## 5. Walk logging *(new)*

Screen: **Log/Edit Walk** (`WalkLogScreen`), reachable from the `+` menu.

- For the **dog walker's report only**: a time and a `+`/`−` stepper for how many
  bowel movements happened on the walk — no per-movement detail, since that's all
  the walker reports. 🧪
- The screen shows an inline warning: only log this if you *didn't* already tag
  individual movements as "Walk" (via bowel-movement Location, above) for the same
  outing, or the dashboard double-counts it. This isn't enforced by the app —
  it's a reminder at the point of logging, not a hard rule.
- Shows in History (its own filter chip) and is included in CSV export and
  backup/restore. 🧪

## 6. History

Screen: **History** (`HistoryScreen`).

- Reverse-chronological feed of every entry type: Bowel, Food, Medication, Energy,
  Walk. ✅ (Energy/Walk rows are 🧪 — new.)
- Filter by type (chip toggles) and date range. ✅
- Tap to edit, long-press to delete (with confirmation). ✅
- A bowel-movement row's subtitle now also shows amount, location (or the "Other"
  text), a "night" tag, a 📷 marker if it has a photo, and notes. 🧪

## 7. Dashboard (Home screen)

Screen: **Home** (`HomeScreen`). Split into two scroll sections so the screen answers
"how's today going?" before "what's the recent pattern?" 🧪

**Today** (top, always visible without scrolling): a filled hero card showing today's
bowel-movement count in large type, last-logged relative time, and — new — today's
food/medication/**energy**/**walk-report** counts all in one line (previously only
food and medication were surfaced here).

**History** (below a divider, scrolls): a 7d / 14d / 30d / 90d window chip row drives
every chart in this section together.
- **Consistency trend chart** — line chart of recent consistency scores; tap any
  point to show its exact date/time + value in a caption below the chart.
- **Movements-per-day bar chart** — zero-filled for days with no entries; scrolls
  horizontally rather than squeezing bars for a longer window; tap a bar or its day
  label to show the full date + count.
- **Walk / Night / Inside home / Garden stat tiles** — window totals. Walk sums
  individually-tagged Location = Walk movements *plus* dog-walker-reported
  Walk-entry counts (the two are never double-counted at entry time, so summing them
  is safe).
- **Per-day breakdown charts** — one bar chart each for Walks, Night movements,
  Inside-home movements, and Garden movements per day, so a pattern in *where* or
  *when* movements happen is visible at a glance rather than only the aggregate
  count. Each shows a plain-language empty state ("No movements tagged … in this
  window") instead of an all-zero chart when a category has no data yet.

Cards throughout use a slightly larger corner radius and subtle elevation, and
"TODAY"/"HISTORY" section labels mark the two scroll regions — a visual refresh on
top of the same underlying charts.

`+` quick-add FAB: Bowel Movement, Food, Medication, **Energy** 🧪, **Walk** 🧪.

## 8. CSV Export

Screen: **Export** (`ExportScreen`).

- One CSV per entity type, optionally date-ranged, shared via Android's share
  sheet: `bowel_movements.csv`, `food_entries.csv`, `medication_entries.csv`. ✅
  Plus **`energy_entries.csv`** and **`walk_entries.csv`** 🧪 (new).
- Bowel movement CSV now also includes amount, location, location_other,
  is_night_time, photo_uri columns. 🧪
- Food entry CSV now also includes amount_value, amount_unit. 🧪
- Medication entry CSV now also includes dose_value, dose_unit. 🧪

## 9. Backup & Restore / Clear All Data

Settings → **Backup & Restore**, **Danger Zone**.

- Full-database JSON backup/restore (all-or-nothing), byte-for-byte including ids
  and foreign keys — distinct from the (lossy, vet-facing) CSV export. ✅
- Now also backs up/restores **Energy** and **Walk** entries, and every new bowel
  movement / food entry / medication entry field above. 🧪 An *older* backup file
  (from before these fields existed) still restores cleanly — missing fields read
  as "not recorded," not an error.
- Structured ingredient data isn't part of the backup file itself — it's
  regenerated automatically from each restored food's ingredient text on next app
  start.
- "Clear All Data" wipes every table, including the new ones. 🧪

## 10. Settings

Screen: **Settings** (`SettingsScreen`).

- Theme: System / Light / Dark. ✅
- Food Catalog management shortcut. ✅
- Insights shortcut (upload a report generated by the `crapp-insights` skill). ✅
- **Reminders** 🧪 *(new)* — toggle + threshold (12h / 24h / 48h) for a "no movement
  logged in over N hours" notification. Turning it on requests the Android 13+
  notification permission if needed. Tapping the notification deep-links straight
  into the bowel-movement log screen.
- Backup/Restore, Clear All Data (see above).

## 11. Reminders / notifications *(new)*

- A `WorkManager` job checks every 6 hours whether the most recent bowel movement
  is older than the configured threshold, and posts a local notification if so. 🧪
- Requires the reminder toggle to be on (see Settings above) and, on Android 13+,
  the notification permission granted.
- Notification tap opens the app directly on the bowel-movement log screen.

## 12. Wear OS companion app *(new, separate `:wear` module/APK)*

A **second, separately-installed app** (`wear/`, applicationId `com.crapp.wear`,
Wear OS 3+ / API 30+) — not part of the phone APK. Sideload it the same way as the
phone app (see [install-on-phone.md](install-on-phone.md)), onto a paired watch or
Wear OS emulator.

- Single screen: 💩 icon, today's bowel-movement count, a `+` button. 🧪 — **not yet
  tested on any Wear OS device or emulator; this is the least proven part of this
  session's work.** See [Testing status](#testing-status).
- `+` sends a "log a movement now" message to the phone over the Wearable Data
  Layer API; the phone (not the watch) does the actual database insert, so
  there's only ever one source of truth.
- **What a watch-logged movement looks like on the phone:** consistency defaults
  to 4 (the same neutral default the phone's own new-entry form starts on) with a
  note "Logged from Wear OS watch — edit to set a real consistency score." Per the
  original requirement, only the phone app can set a real consistency score,
  amount, location, etc. — the watch is deliberately poo-count-only.
- The phone pushes today's count to the watch on every change (not just ones the
  watch itself triggered), so the watch's number stays live even if you log from
  the phone instead.
- Requires the `android.permission.POST_NOTIFICATIONS`-style pairing dance to
  happen once between the two apps via the Play Services Wearable Data Layer —
  no manual setup beyond having both apps installed and the watch actually paired
  to the phone.

## Data model additions this session

New tables: `energy_entry`, `walk_entry`, `ingredient`, `food_ingredient`.
New columns: `bowel_movement.{amount, location, locationOther, isNightTime,
photoUri}`, `food_entry.{amountValue, amountUnit}`, `medication_entry.{doseValue,
doseUnit}`. All shipped together in one migration, `MIGRATION_2_3` (schema version
2 → 3) — see `app/src/main/java/com/crapp/data/db/Migrations.kt`.

## Testing status

Everything marked 🧪 above compiles and passes unit tests (`./gradlew
testDebugUnitTest`) and both APKs assemble (`./gradlew assembleDebug`), but none of
it has been click-tested on a real phone/watch yet. In particular, before trusting
it day-to-day:

- **Phone app:** amount/location chips + "Other" text field, photo capture (camera
  permission prompt, `Pictures/CrApp` album actually appearing, thumbnail
  decoding), Energy/Walk logging screens end-to-end, History's new filter chips
  and bowel-movement subtitle, Home's window selector + tap-to-inspect on both
  charts, night/walk stat tiles, structured dose/amount fields on Food/Medication
  screens, Reminders toggle + permission prompt + an actual notification firing
  and deep-linking correctly, Settings round-tripping all of the above, and a full
  backup → clear → restore round trip with the new fields populated.
- **Wear OS:** the entire `:wear` module — needs a paired watch or a Wear OS
  emulator, neither of which this session had access to. Build/install it via
  Android Studio (Run on the `wear` module with a Wear device/emulator target)
  alongside the phone app, then verify the `+` button actually reaches the phone
  and the count pushes back.
- A Room migration test for `MIGRATION_2_3`
  (`app/src/androidTest/java/com/crapp/data/db/MigrationTest.kt`) is written but,
  like all instrumented tests, needs `connectedAndroidTest` to run — **do not run
  that without a fresh backup first**; it has wiped a real device's data before
  (see project memory).
- Structured ingredients: the backfill runs silently at startup; there's no UI to
  confirm the 4 seeded foods actually got parsed correctly beyond re-reading the
  code, short of a debug DB inspection.

Nothing in this list blocks the app from running — it's the punch-list for
tomorrow's device pass, not known bugs.
