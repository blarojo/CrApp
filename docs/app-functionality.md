# CrApp — Functionality Reference

A collated, always-current list of everything the app actually does, grouped by
area rather than by when it was built (that history lives in
[development-plan.md](development-plan.md)) or what's still an idea
([backlog.md](backlog.md)). Update this file whenever a feature is
added or changed — it's meant to be the one place that answers "does CrApp do X?"
without archaeology through commits or specs.

**Status key:** ✅ shipped and confirmed working on-device · 🧪 shipped but not yet
(or only partially) click-tested on a real device (see [Testing status](#testing-status)
at the bottom before relying on it) · — not implemented.

## 1. Bowel movement logging

Screen: **Log/Edit Bowel Movement** (`BowelMovementLogScreen`).

| Field | Status | Notes |
|---|---|---|
| Timestamp | ✅ | Defaults to now; editable date + time pickers. |
| Consistency (1–7) | ✅ | Purina Fecal Scoring Chart, tap-to-select with an icon + description per score. Chips are ordered **7 down to 1** (highest/loosest first) rather than 1 up to 7, since a dog whose scores cluster at the high end shouldn't have to scroll to reach them. A "What do these scores mean?" link opens [Purina's own reference chart](https://vetcentre.purina.co.uk/news-articles/faecal-score-chart) in the browser. |
| Amount | ✅ | Three-option tap selector: **Some drips** / **Medium amount** / **A lot of poo**. Independent of consistency — a small amount can still be liquid. Optional. Round-tripped on-device (save → dashboard/History reflect it → edit confirms it → delete reverts it). |
| When / Where | ✅ | Four tap chips: **Night** / **Walk** / **Inside home** / **Garden**. **Night** is an independent toggle (can be combined with any location) pre-filled from a configurable night window (default 10pm–6am) but always user-overridable; **Walk** / **Inside home** / **Garden** set the location and are mutually exclusive. (The `Location.OTHER` value and its free-text field still exist in the data model for backward compatibility but are no longer exposed in this UI.) Round-tripped on-device including the matching dashboard tiles/charts. |
| Blood / mucus present | ✅ | Two checkboxes. |
| Notes | ✅ | Free text. |
| Photo | 🧪 | "Take Photo" opens the device camera, saving into a shared, user-visible **`Pictures/CrApp`** album via `MediaStore` — deliberately *not* app-private storage, so photos survive an uninstall/reinstall. Thumbnail shown inline (decoded on-device, no image-loading library); tap "Remove" to delete. The save/remove/thumbnail flow, plus camera launch and cancel-cleanup, are verified; an actual successful photo capture couldn't be automated via `adb` (the test phone's camera app doesn't respond to synthetic shutter taps), so a real capture still needs a manual test. |
| Color | ✅ | Free text. |

Edit and delete both work from History; deleting an entry with a photo also deletes
the underlying `MediaStore` file.

## 2. Food logging + Food Catalog

Screens: **Log/Edit Food** (`FoodLogScreen`), **Food Catalog** (`FoodCatalogScreen`,
via Settings).

- Pick from a dropdown of previously-used foods (most-recently-used first) or add a
  new one inline without leaving the log flow. ✅
- Meal type: Meal / Treat chip select. ✅
- Amount: free text (e.g. "1/2 cup"). ✅
- **Structured amount** ✅ — additive numeric value + unit (cup / tbsp / g / tin (400g)) fields
  alongside the free-text amount, for analysis that needs real numbers instead of
  parsing prose. Round-tripped on-device (value 1, unit "tin (400g)", confirmed via
  the edit screen, then deleted).
- **Quick-amount buttons** ✅ — two one-tap shortcuts, **"1 cup"** and **"1 tin
  (400g)"**, above the amount fields for the most common portions. Tapping one fills
  the free-text amount *and* the structured value/unit fields in one go (e.g. "1 tin
  (400g)" sets amount = `"1 tin (400g)"`, value = `1`, unit = `"tin (400g)"`); tapping
  a different shortcut afterward overwrites rather than accumulates, and every field
  stays directly editable afterward — this is a starting point, not a locked value.
  Round-tripped on-device: tapped "1 tin (400g)" on a new "Scrambled egg" entry,
  confirmed both fields filled correctly, saved, and confirmed History shows "Meal ·
  1 tin (400g)" exactly. Also covered by an instrumented BDD-style UI test
  (`FoodLoggingFlowTest`) for both the fill and the overwrite-not-accumulate
  behavior.
- Food Catalog lets you add/edit a food's ingredients as free text (manual entry or
  pasted from a label). ✅
- **4 starter foods are pre-seeded** on a brand-new install (Hill's z/d Mini dry,
  Hill's z/d wet, Purina Pro Plan HA Mousse, Purina Pro Plan HA Dry) with their
  real ingredient labels already filled in. ✅
- **Structured ingredients** ✅ (backend only, no dedicated screen) — every food's
  ingredient text is parsed in the background into a normalized ingredient catalog +
  per-food join table (`Ingredient` / `FoodIngredient`), so the Insights feature (§13)
  can correlate individual ingredients against symptoms instead of string-matching
  label text. Runs automatically on every app start; a small known-synonym list
  (e.g. "corn starch" ↔ "maize starch") keeps the same real ingredient from
  fragmenting into duplicate rows across the 4 seeded labels. Verified correct
  against the real 4 seeded foods via direct on-device database inspection — there's
  no UI to click-test, by design.

## 3. Medication logging

Screen: **Log/Edit Medication** (`MedicationLogScreen`).

- Name, dose (free text), notes. ✅
- **Structured dose** 🧪 — additive numeric value + unit (mg / ml / mcg) fields
  alongside the free-text dose. The field and unit chips are confirmed present and
  rendering correctly; no dedicated save round-trip has been run for medication
  dose specifically (unlike the food side above, which has been).

## 4. Energy logging

Screen: **Log/Edit Energy** (`EnergyLogScreen`), reachable from the `+` menu.

- A 5-point named scale — **Slept all day / Low energy / Normal / A bit playful / A
  lot of energy** — tap to select (no raw numbers shown). ✅
- Optional notes. ✅
- Shows in History (its own filter chip) and is included in CSV export and
  backup/restore. ✅ Round-tripped on-device: saved a "Low energy" entry, confirmed
  it appeared on the Energy trend chart and in the Today card's count, deleted it via
  History, confirmed both reverted.

## 5. Walk logging

Screen: **Log/Edit Walk** (`WalkLogScreen`), reachable from the `+` menu.

- For the **dog walker's report only**: a time and a `+`/`−` stepper for how many
  bowel movements happened on the walk — no per-movement detail, since that's all
  the walker reports. ✅
- The screen shows an inline warning: only log this if you *didn't* already tag
  individual movements as "Walk" (via bowel-movement Location, above) for the same
  outing, or the dashboard double-counts it. This isn't enforced by the app —
  it's a reminder at the point of logging, not a hard rule.
- Shows in History (its own filter chip) and is included in CSV export and
  backup/restore. ✅ Round-tripped on-device: saved a 3-count walk entry, confirmed
  the dashboard total updated correctly, deleted it, confirmed the total reverted.

## 6. History

Screen: **History** (`HistoryScreen`).

- Reverse-chronological feed of every entry type: Bowel, Food, Medication, Energy,
  Walk. ✅
- Filter by type (chip toggles) and date range. ✅
- Tap to edit, long-press to delete (with confirmation). ✅
- A bowel-movement row's subtitle also shows amount, location (or the "Other"
  text), a "night" tag, a 📷 marker if it has a photo, and notes. ✅

## 7. Dashboard (Home screen)

Screen: **Home** (`HomeScreen`). Split into two scroll sections so the screen answers
"how's today going?" before "what's the recent pattern?" ✅

**Today** (top, always visible without scrolling): a filled hero card showing today's
bowel-movement count in large type, last-logged relative time, and today's
food/medication/energy/walk-report counts all in one line.

**History** (below a divider, scrolls): a **1d** / 7d / 14d / 30d / 90d window chip
row drives every chart and stat tile in this section together, filtering by real
calendar days (not by movement count).
- **Consistency trend chart** ✅ — line chart of recent consistency scores, a true
  time scale: each day gets an equal-width column and a point's position within its
  day reflects its actual time of day. Scrolls horizontally with one date label per
  day and opens scrolled to the latest day. Dog-walker-reported movements (no
  per-movement timestamp) are drawn as small unscored tick marks spread across an
  assumed one-hour window at their logged time, distinct from the scored line — see
  `ConsistencyTrendChart`'s KDoc. Tap any point to show its exact date/time + value
  in a caption below.
- **Movements-per-day bar chart** ✅ — every movement, including dog-walker-reported
  counts; zero-filled for days with no entries; date-labelled bars; scrolls
  horizontally and opens scrolled to the latest day; tap a bar or its label to show
  the full date + count.
- **Energy trend chart** ✅ — same time-scaled engine as the consistency chart
  (`ScoreTrendChart`, shared by both), plotting each `EnergyLevel`'s position (1-5,
  low to high) over real time with one date label per day. The y-axis shows plain
  numbers, but tapping a point shows its actual level name (e.g. "Normal") rather
  than the number.
- **Walk / Night / Inside home / Garden stat tiles** ✅ — window totals. Walk sums
  individually-tagged Location = Walk movements *plus* dog-walker-reported
  Walk-entry counts (the two are never double-counted at entry time, so summing them
  is safe).
- **Per-day breakdown charts** ✅ — one bar chart each for movements during walks,
  night movements, inside-home movements, and garden movements per day (same
  date-labelled, auto-scrolled-to-latest bar chart as above). Each shows a
  plain-language empty state ("No movements tagged … in this window") instead of an
  all-zero chart when a category has no data yet.

All of the above — the 1d window option, real calendar-day filtering, the
time-scaled x-axis, per-day date labels, dog-walker tick marks, and tap-to-inspect —
were round-tripped and verified on-device.

Cards throughout use a slightly larger corner radius and subtle elevation, and
"TODAY"/"HISTORY" section labels mark the two scroll regions.

`+` quick-add FAB: Bowel Movement, Food, Medication, Energy, Walk. ✅

## 8. CSV Export

Screen: **Export** (`ExportScreen`), reachable from Settings → **Export CSV** (see
§10).

- One CSV per entity type, optionally date-ranged, shared via Android's share
  sheet: `bowel_movements.csv`, `food_entries.csv`, `medication_entries.csv`,
  `energy_entries.csv`, `walk_entries.csv`. ✅
- Bowel movement CSV also includes amount, location, location_other,
  is_night_time, photo_uri columns. ✅
- Food entry CSV also includes amount_value, amount_unit. ✅
- Medication entry CSV also includes dose_value, dose_unit. ✅

All five CSVs and their extra columns have been exercised for real: an actual export
from the app was used as the input to a real Insights analysis run (§13), and every
column parsed correctly.

## 9. Backup & Restore / Clear All Data

Settings → **Backup & Restore**, **Danger Zone**.

- Full-database JSON backup/restore (all-or-nothing), byte-for-byte including ids
  and foreign keys — distinct from the (lossy, vet-facing) CSV export. ✅
- Also backs up/restores Energy and Walk entries, and every bowel movement / food
  entry / medication entry field above. 🧪 An *older* backup file (from before these
  fields existed) still restores cleanly — missing fields read as "not recorded,"
  not an error — but a full backup → clear → restore round trip with the newer
  fields populated hasn't been click-tested on-device yet.
- Structured ingredient data isn't part of the backup file itself — it's
  regenerated automatically from each restored food's ingredient text on next app
  start.
- "Clear All Data" wipes every table, including the newer ones. 🧪 Not yet
  click-tested on-device.

## 10. Settings

Screen: **Settings** (`SettingsScreen`).

- Theme: System / Light / Dark. ✅
- Food Catalog management shortcut. ✅
- Medication Catalog management shortcut. ✅
- **Export** — share Mango's logged data as CSV files, for a vet visit or as the
  input to the Insights analysis below. ✅ (This used to be a top-level link on Home;
  it now lives here instead, alongside the other data-management actions.)
- Insights shortcut (upload a report generated by the `crapp-insights` skill) — see
  [§13](#13-insights-ai-generated-report). Also reachable via a top-level link on
  Home, next to History. ✅
- **Reminders** 🧪 — toggle + threshold (12h / 24h / 48h) for a "no movement logged
  in over N hours" notification. Turning it on requests the Android 13+ notification
  permission if needed. Tapping the notification deep-links straight into the
  bowel-movement log screen. The toggle, the real permission prompt, the threshold
  chips, and the underlying WorkManager job registration are all confirmed on-device
  (`dumpsys jobscheduler` shows it scheduled and already ran once) — the actual "no
  movement in 24h+" notification firing hasn't been observed live yet, since that
  needs a real elapsed day with no logging.
- Backup/Restore, Clear All Data (see §9).

## 11. Reminders / notifications

- A `WorkManager` job checks periodically whether the most recent bowel movement is
  older than the configured threshold, and posts a local notification if so. 🧪 (see
  §10 for what's confirmed vs. still unobserved)
- Requires the reminder toggle to be on (see Settings above) and, on Android 13+,
  the notification permission granted.
- Notification tap opens the app directly on the bowel-movement log screen.

## 12. Wear OS companion app

A **second, separately-installed app** (`wear/`, applicationId `com.crapp.wear`,
Wear OS 3+ / API 30+) — not part of the phone APK. Sideload it the same way as the
phone app (see [install-on-phone.md](install-on-phone.md)), onto a paired watch or
Wear OS emulator.

- Single screen: 💩 icon, today's bowel-movement count, a `+` button. ✅ (built,
  installs and renders correctly)
- `+` sends a "log a movement now" message to the phone over the Wearable Data
  Layer API; the phone (not the watch) does the actual database insert, so there's
  only ever one source of truth.
- **What a watch-logged movement is meant to look like on the phone:** consistency
  defaults to 4 (the same neutral default the phone's own new-entry form starts on)
  with a note "Logged from Wear OS watch — edit to set a real consistency score."
  Per the original requirement, only the phone app can set a real consistency
  score, amount, location, etc. — the watch is deliberately poo-count-only.
- The phone is meant to push today's count to the watch on every change (not just
  ones the watch itself triggered), so the watch's number stays live even if you log
  from the phone instead.

**Known issue — message delivery blocked at the platform level, not a CrApp bug.**
🧪 Tested against a real Samsung Galaxy Watch5 already daily-paired to the test phone
(a OnePlus 12 running OxygenOS). The watch app installs, renders, and correctly
finds the phone as a connected node; `MessageClient.sendMessage` reports success
every time. But the phone's Google Play Services consistently logs
`WearableService: Failed to deliver message ... action=/crapp/log_movement` and
`PhoneWearableListenerService` never receives it. Ruled out, each independently
verified: app signing (identical debug cert on both APKs), manifest/protocol (an
exact match to Google's documented `WearableListenerService` pattern), and a
stale-cache/timing issue (retried after battery-optimization whitelisting,
Bluetooth toggle, a Play Services restart, and a full phone reboot — identical
failure every time). The Bluetooth link itself is fine throughout
(`dumpsys bluetooth_manager` shows an active `Connected` state, and the watch's own
first-party sync keeps working). This points at a Play Services incompatibility
specific to this non-Samsung/non-Pixel phone plus a Samsung "Wear OS powered by
Samsung" watch, for **generic third-party** `MessageClient` delivery — not something
fixable from the app side. Worth retrying on a Samsung or Pixel phone, or after a
Play Services update, before assuming the feature itself is broken.

## 13. Insights (AI-generated report)

Top-level **Insights** link on Home, next to History — also reachable via
Settings → Insights. See
[backlog.md's "Shipped features" note](backlog.md#shipped-features)
for background and `.claude/skills/crapp-insights/SKILL.md` for the authoritative
analysis workflow.

- Nothing computes on-device: the screen uploads a JSON report file produced
  *outside* the app by the `crapp-insights` Claude skill from a fresh CSV export
  (Settings → Export → share the CSVs → run the skill → upload the JSON it writes
  back here). No network calls, no on-device inference. ✅
- Findings are grouped under up to three headings: **🩺 Bowel movement
  progression**, **🍗 Mood, food & bowel correlations**, and "Other findings" (a
  heading is omitted entirely if the report has nothing in that section — never a
  forced empty group). ✅
- A ⚠️ data-completeness caveat line (e.g. noting dog-walker-reported walks have no
  consistency score) renders under the summary paragraph when the report includes
  one. ✅
- Below the grouped findings: the generic line/bar trend-series charts (consistency
  over time, movements per week, etc.). ✅
- The last-uploaded report persists locally (`InsightsPreferences`, plain
  SharedPreferences) so it's still there next time the app opens, without
  re-uploading. ✅
- **Backward compatible**: a `schemaVersion: 1` file from before sections/caveat
  existed still uploads and renders — everything just lands under "Other findings"
  with no caveat line, rather than being rejected. ✅

Fully click-tested end-to-end on-device with a real report generated by the skill
from a real CSV export: the top-level Home link, both section headings with their
insight cards (correct notable/info styling), the caveat line, and all series
charts all confirmed rendering correctly with values matching the source JSON
exactly.

## Data model

Tables: `bowel_movement`, `food_entry`, `medication_entry`, `energy_entry`,
`walk_entry`, `food` (catalog), `ingredient`, `food_ingredient`.

Notable columns beyond the obvious: `bowel_movement.{amount, location,
locationOther, isNightTime, photoUri}`, `food_entry.{amountValue, amountUnit}`,
`medication_entry.{doseValue, doseUnit}`. All added together in one migration,
`MIGRATION_2_3` (schema version 2 → 3) — see
`app/src/main/java/com/crapp/data/db/Migrations.kt`.

## Testing status

Everything above compiles, passes unit tests (`./gradlew testDebugUnitTest`), and
both APKs assemble (`./gradlew assembleDebug`). Most of it has also been
click-tested on a real phone/watch, per the ✅ markers throughout this document.
What's still genuinely unverified:

- **Medication structured dose** (§3) — fields render correctly, but no dedicated
  save round-trip has been run for medication dose specifically.
- **Photo capture** (§1) — the save/remove/thumbnail flow and camera launch/cancel
  are verified; an actual photo capture still needs a manual test (can't be
  automated via `adb` on the test phone's camera app).
- **Reminders' live notification firing** (§10, §11) — the toggle, permission
  prompt, and the underlying scheduled job are confirmed; the notification actually
  firing after 24h+ of no logging hasn't been observed live yet.
- **Backup → clear → restore round trip** with the newer fields populated (§9) —
  not yet click-tested.
- **Wear OS** (§12) — the watch app itself is confirmed working; message delivery to
  the phone is blocked by what looks like a platform-level Play Services
  incompatibility (see §12's "Known issue" for the full investigation), not
  something further click-testing on this hardware pair would resolve.
- A Room migration test for `MIGRATION_2_3`
  (`app/src/androidTest/java/com/crapp/data/db/MigrationTest.kt`) is written but,
  like all instrumented tests, needs `connectedAndroidTest` to run — **do not run
  that without a fresh backup first**; it has wiped a real device's data before
  (see project memory).

Nothing in this list blocks the app from running — it's the punch-list for the next
device pass, not known bugs.
