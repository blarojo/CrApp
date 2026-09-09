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

- Pick from a dropdown of known foods, **alphabetical** (matching the Food Catalog
  screen's own ordering, below), or add a new one inline without leaving the log
  flow. Selecting a food that has a **usual amount** set (see Food Catalog, below)
  pre-fills the amount fields below from it — still fully overridable, and a food
  with no usual amount set leaves the amount fields untouched rather than clearing
  them. ✅
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
- Food Catalog lists every known food **alphabetically**, and lets you edit an
  existing one's ingredients as free text (manual entry or pasted from a label) and
  its usual amount (see below). ✅
- **Add new food manually** ✅ — a `+` button on the Food Catalog screen opens an
  "Add new food" dialog: Name (required), Brand (optional), Ingredients (optional,
  typed/pasted or scanned — see below), Usual amount (optional, see below). Unlike
  the log screen's inline "Add new" (name only), this is the one place to fill in
  all four fields **without** logging a food entry first. Adding a name that already
  exists in the catalog updates that row (fills in whichever fields were left blank
  before) rather than creating a confusing duplicate. Round-tripped on-device: added
  "Peanut Butter" / "Homemade" / "Peanuts, salt" from an empty dialog, confirmed it
  appeared in the catalog with all three fields exactly right, in correct
  alphabetical position. Also covered by an instrumented BDD-style UI test
  (`FoodCatalogFlowTest`) for both the fresh-add and the
  update-existing-by-name-match cases.
- **Usual amount** 🧪 — a food's typical portion (value + unit, same
  cup/tbsp/g/tin (400g) vocabulary as the structured amount fields above), set from
  either the add-new or edit dialog. When a food has one, selecting it on the
  food-logging screen pre-fills the amount fields from it (see §1's dropdown
  bullet) — e.g. always "1 tin (400g)" for a wet food, "1 cup" for a dry food, "50 g"
  for treats, one setup per food instead of re-typing or re-tapping a quick-amount
  button every time, still fully overridable before saving. Shown on the food's
  catalog row too ("Usual amount: 1 tin (400g)"). Backed by a new schema column
  (`MIGRATION_4_5`) that was exercised for real — the actual `adb install -r` over
  the real on-device database, not just the synthetic `MigrationTestHelper` test —
  and confirmed clean (app launched, stayed running, no crash or SQL exception in
  logcat); the UI flow itself (pre-fill on selection, still-overridable) is written
  and covered by a new instrumented BDD-style test (`FoodUsualAmountFlowTest`) but
  wasn't click-tested live this pass — the phone locked (fingerprint) before that
  could happen — so treat the *behavior* as unverified live until that's done, even
  though the *data* is confirmed safe.
- **Scan label (OCR)** ✅ — a "📷 Scan label" button, in both the add-new and
  edit-ingredients dialogs, takes a photo of a food label and runs on-device text
  recognition (`com.google.mlkit:text-recognition`, the fully-bundled model — no
  network call, ever) to pre-fill the ingredients field for review/edit before
  saving; never auto-saved unreviewed. The photo itself is disposable (app cache,
  deleted right after each scan) — unlike a bowel-movement photo, it's not meant to
  be kept. Camera launch (a real intent to the device's camera app, same mechanism
  as the bowel-movement photo feature) and clean cancel-recovery are confirmed
  on-device; an actual label capture + recognized-text pass couldn't be automated
  the same way the bowel-movement photo capture couldn't (see Testing status) — a
  real scan still needs a manual on-device test.
  - **Ingredients-section extraction** ✅ — after real-world use showed the raw
    recognized text (nutritional info, weight, address, etc. all included) needed
    manual trimming, `IngredientsTextExtractor` now isolates just the
    ingredients/composition section before pre-filling the field: it looks for a
    line containing "Ingredients" or "Composition" (the UK/EU pet-food convention
    this app's own seed labels use) and takes everything up to the next recognized
    section heading (analytical constituents, nutritional info, additives, feeding
    guide, best-before, etc.). **No AI/cloud model involved** — plain rule-based
    text matching on the client, run on whatever ML Kit already recognized. It's an
    approximation, not a guarantee (an unusual label layout or wording won't match),
    so it falls back to the full recognized text (with a heads-up toast) rather than
    silently guessing wrong or losing text. Fully unit-tested (pure text logic, no
    Android/camera dependency — see `IngredientsTextExtractorTest`), unlike the
    camera-capture part above.
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
food/medication/energy/walk-report counts all in one line. The bowel-movement count
folds in dog-walker-reported [`walk_entry`](#5-walk-logging) counts too, not just
individually-logged movements (`countBowelMovementsToday`, shared with the widget
below) — logging a walk with a real count now moves this number the same day it's
logged, matching how the "Movements per day" chart further down already did. This
was a real bug (fixed): the hero number used to silently exclude walk-reported
movements even though the chart below it didn't.

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

## 14. Home screen widget

docs/backlog.md spec 15. A standard Android home screen widget (`CrAppWidget`,
built on Jetpack Glance — the Compose-based widget API, not classic `RemoteViews`,
to stay in the same UI toolkit as the rest of the app) — add it the normal Android
way: long-press an empty spot on the home screen → **Widgets** → **CrApp**.

**Layout: 2×2**, a rounded chip badge up top (the count, on a `primaryContainer`
background, echoing `HomeScreen`'s own Today card rather than plain text floating
on the widget background) and the two quick-add buttons side by side underneath
("+BM" / "+Food"), the whole stack centered in the widget's real allocated area
instead of pinned to the top. Went through two design passes on real-device
feedback: the original shipped layout (inline text summary, two half-width
buttons in a row) worked but looked unpolished; a first redesign fixed that with
a nicer chip and icon-labelled buttons but *stacked* them full-width, which on
the user's actual launcher only left room for the chip plus one button at true
2×2 size; side-by-side with short labels is what actually fits a real 2×2 slot
while still looking deliberate.

- **Summary chip** — a rounded badge with today's bowel-movement count as the
  bold headline number ("💩 6"), plus a smaller line underneath for food/energy
  counts when non-zero ("🍗 3   ⚡ 1") — the same numbers and the same "only show
  if non-zero" convention as `HomeScreen`'s own "Today" hero card, computed by the
  exact same shared logic (`computeWidgetTodayCounts`, built on
  `countBowelMovementsToday`/`Instant.isToday()` — see `com.crapp.util`) rather
  than a second implementation that could drift from the app's own numbers. There's
  no separate "walk" number shown (still deliberately excluded, to keep the widget
  compact) but the headline bowel-movement count itself *does* fold in
  dog-walker-reported counts — the same bug-fix as §7's hero card, see there for
  detail. ✅ layout/numbers-match-Home confirmed on-device (before the walk-fold
  fix existed); 🧪 the walk-fold behavior itself is unit-tested
  (`WidgetTodayCountsTest`) but not yet click-tested live — the test phone was
  locked when this fix was made.
  Tapping the chip (not either button) opens the app on its normal Home screen — a
  plain launch, no deep-link extra, so there's a way into the full dashboard
  straight from the widget. 🧪 Not yet click-tested live for the same reason.
- ✅ **"+BM" button** — opens the app directly on the Log Bowel Movement screen,
  reusing the exact same deep-link mechanism (`MainActivity.EXTRA_OPEN_LOG_BOWEL_MOVEMENT`)
  a reminder notification tap already uses, not a second path to the same
  destination. Confirmed on-device: tapping it from the home screen opens straight
  into that screen, never Home first.
- ✅ **"+Food" button** — opens the app directly on the Log Food screen, via a new
  equivalent extra (`MainActivity.EXTRA_OPEN_LOG_FOOD`) following the exact same
  pattern. Confirmed on-device the same way.
- **Refresh strategy**, two parts:
  - ✅ Near-instant: a reactive collector in `CrAppApplication.onCreate()` watches
    the same three repositories' data and calls `CrAppWidget().updateAll()` on any
    relevant change — the same established pattern this app already uses to keep a
    paired Wear OS watch's today-count display current, just pointed at the widget
    instead. Confirmed on-device: logged a real food entry from inside the app,
    returned to the home screen without removing/re-adding the widget, and its
    count updated automatically (🍗 3 → 4) with no manual refresh needed.
  - 🧪 Hourly fallback (`WidgetRefreshWorker`, a `WorkManager` periodic job, same
    idiom as the Reminders feature's own periodic check): catches the one case the
    reactive path can't — a local-midnight rollover with no new data logged at all,
    so "today's" count doesn't keep showing yesterday's numbers on a quiet morning.
    Not yet observed live (would need a real elapsed midnight, or a device clock
    change, to trigger).
  - `updatePeriodMillis` in the widget's own provider metadata is deliberately `0`
    (unused) — refresh is driven by the above instead, not the legacy
    once-every-30-minutes-minimum widget-metadata mechanism.
- No new data model/migration — reads the same `BowelMovementDao`/`FoodDao`/
  `EnergyEntryDao`/`WalkEntryDao` data `HomeViewModel` already reads for its own
  Today card.

**Status: implemented, unit-tested, and mostly click-tested live on a real
device** — widget placement, the summary numbers (as they stood before the
walk-fold fix), both quick-add buttons, and the reactive refresh-on-write path are
all confirmed working exactly as designed. Two things from the most recent pass
(the walk-fold fix and tap-the-chip-to-open-Home) are unit-tested but not yet
click-tested live — the test phone was locked at the time — plus the
still-standing hourly midnight-rollover fallback. See Testing status below for
the exact list.

## 15. Gallery (photo grid)

docs/backlog.md spec 14. A third top-bar link on Home, next to **History** and
**Insights** — a 2-column grid of every bowel-movement photo taken so far, newest
first, so browsing "what did this look like a few weeks ago" is a scroll instead of
a hunt through History for a 📷 marker. Backed by a dedicated query
(`BowelMovementDao.observeAllWithPhoto()` — only rows with a non-null `photoUri`,
ordered by timestamp descending), not a client-side filter over every movement. ✅
Confirmed on-device: the top bar comfortably fits all three text links plus the
Settings gear on the same row as the "💩 CrApp" title — no crowding, no overflow
menu needed.

- **Each card** shows the photo on top (cropped to a square, `ContentScale.Crop`)
  and a caption below it inside the same card (not text overlaid on the image):
  date **and time** (deliberately more than the ask's "the date" — Mango can have
  several movements a day, so date-only would make same-day cards indistinguishable),
  then "Consistency N" plus amount if set, then location + night tag if either is
  set (omitted entirely if neither is — same "don't show an empty line" rule used
  throughout this app's cards). Wording mirrors `HistoryScreen`'s own bowel-movement
  subtitle rather than inventing new copy; notes and blood/mucus flags are
  deliberately left off (a gallery card's job is "which day, roughly what
  happened" — History still covers full detail). The caption-building logic is
  pulled out as a pure function (`buildGalleryCaption`) specifically so its
  "what shows, what's omitted" rules are unit-tested (`GalleryCaptionTest`) rather
  than only checked by eye. ✅ Confirmed on-device with a real logged photo: date/
  time, consistency + amount, and location all rendered correctly and matched
  History's own wording for the same entry.
- **Empty state**: "No photos yet — attach one next time you log a bowel
  movement." instead of a blank grid. 🧪 Not click-tested live (the test device
  already has a photographed entry, so the empty state couldn't be triggered
  without deleting real data) — covered by an instrumented test instead
  (`GalleryFlowTest`).
- **Thumbnail decoding is properly downsampled**, not a reused full-resolution
  decode: `BowelMovementPhotoStore.loadThumbnail()` now does a real two-pass
  bounded decode (`BitmapFactory.Options.inSampleSize`, computed from the actual
  image bounds) capped at 480px on the longest side, generalized so both the
  Gallery grid and the pre-existing 80dp log-screen thumbnail share the same
  decode path — previously (single-photo era) it decoded every photo at full
  camera resolution regardless of how small it was drawn, a real memory/jank risk
  once a grid decodes several at once. `PhotoThumbnail` itself was generalized to
  take its size from the caller's `Modifier` instead of a hardcoded `.size(80.dp)`
  internally, so the grid can size it to a `fillMaxWidth().aspectRatio(1f)` cell.
  ✅ Confirmed on-device — and this fix was caught *because* of that live check:
  the first on-device pass showed the grid thumbnail as "photo not found" while
  the full-size viewer (a separate, undownsampled decode path) correctly showed
  the real photo for the exact same URI. Root cause: `BitmapFactory.decodeStream`
  always returns `null` in `inJustDecodeBounds` (bounds-only) mode — that's normal,
  not a failure — but the bounds-pass code treated that null as "the stream
  couldn't be opened" and bailed out every time. Fixed to read success from
  `bounds.outWidth`/`outHeight` instead of the decode call's return value;
  re-verified live after the fix, thumbnail rendering correctly.
- **A broken/inaccessible photo** (the file was removed outside the app, or a
  restored backup landed on a different device — same situation spec 8 already
  flagged) shows a "📷 Photo not found" placeholder instead of spinning forever,
  in both the grid (`PhotoThumbnail`, which now distinguishes loading/loaded/
  failed states) and the full-size viewer. This was actually observed live (as a
  side effect of the bug above, before it was fixed) and behaved exactly as
  designed — a real, if accidental, confirmation the fallback path works, not
  just a theoretical one.
- **Tapping a card** opens the full-size photo in a Compose `Dialog` (not a new
  screen/route) at its real aspect ratio, undownsampled
  (`BowelMovementPhotoStore.loadFullSize()`), on a scrim, with the same caption
  repeated below it. Two ways to close it: `Dialog`'s own default
  `dismissOnClickOutside = true` (tap outside, the literal ask) and a visible ✕
  button in the corner (a deliberate addition beyond the ask — tap-outside isn't
  self-evident to every user, and a modal with no visible way out is a common
  real complaint). ✅ Both confirmed on-device: tapping the card opens the viewer
  with the real photo; tapping outside it closes it back to the grid; reopening
  and tapping the ✕ button closes it the same way.
- **Not in scope for this version**: tap-to-edit from a gallery card (the existing
  edit path via History still covers that) and date-range/type filter chips
  matching History's own (the query already filters to "has a photo", which is
  all this screen needs).
- No new data model/migration — reads the same `bowel_movement` table every other
  bowel-movement feature already does, just with an added `WHERE photoUri IS NOT
  NULL` query.

**Status: implemented, unit-tested, and click-tested live on a real device** —
including a real bug (the thumbnail-downsampling logic) caught and fixed during
that live pass rather than shipped unnoticed. The one gap is the empty state,
which the test device's existing real data made impossible to trigger live;
that's covered by an instrumented test instead. See Testing status below.

## Data model

Tables: `bowel_movement`, `food_entry`, `medication_entry`, `energy_entry`,
`walk_entry`, `food` (catalog), `medication` (catalog), `ingredient`,
`food_ingredient`.

Notable columns beyond the obvious: `bowel_movement.{amount, location,
locationOther, isNightTime, photoUri}`, `food_entry.{amountValue, amountUnit}`,
`medication_entry.{doseValue, doseUnit}` (all added together in one migration,
`MIGRATION_2_3`, schema version 2 → 3); `medication_entry.medicationId`
(`MIGRATION_3_4`, version 3 → 4, added alongside the `medication` catalog table);
`food.{usualAmountValue, usualAmountUnit}` (`MIGRATION_4_5`, version 4 → 5) — see
`app/src/main/java/com/crapp/data/db/Migrations.kt`.

## Testing status

Everything above compiles, passes unit tests (`./gradlew testDebugUnitTest`), and
both APKs assemble (`./gradlew assembleDebug`). Most of it has also been
click-tested on a real phone/watch, per the ✅ markers throughout this document.
What's still genuinely unverified:

- **Medication structured dose** (§3) — fields render correctly, but no dedicated
  save round-trip has been run for medication dose specifically.
- **Gallery** (§15) — click-tested live: entry point, grid rendering, correct
  caption content, the downsampled thumbnail decode (after a real bug in it was
  caught and fixed during this same live pass), the full-size viewer, and both of
  its closing affordances (tap-outside and the ✕ button) are all confirmed
  working on-device with a real photographed entry. The one thing not click-tested
  live is the empty state (no way to trigger it without deleting the test device's
  real data) — covered instead by `GalleryFlowTest` (instrumented, not yet run --
  see the walk-count entry below for why) and, for the pure caption logic,
  `GalleryCaptionTest` (unit-tested, run and passing). Multi-photo grid layout
  (2+ cards, actual 2-column wrapping) also wasn't observed live, since the test
  device currently has only one photographed entry.
- **Home screen widget** (§14) — click-tested live for most of it, across two
  earlier passes: added via the real system widget picker (correct preview,
  correct description), a genuine 2×2 instance confirmed via `dumpsys appwidget`,
  summary numbers matched the app's Today card exactly, both "+BM"/"+Food" buttons
  opened directly on the right screen, and the reactive refresh-on-write path was
  confirmed (logged a real entry, the already-placed widget updated on its own, no
  remove/re-add needed). **Not yet click-tested live**, from the most recent
  change (unit-tested only — the test phone was locked): the walk-count fold-in
  fix (§7/§14 — a walk logged today should now move the headline "💩" number) and
  tapping the summary chip to open Home. Also still unobserved: leaving the widget
  untouched across a real local-midnight rollover, to confirm the hourly fallback
  job actually resets "today's" count — that needs either a real elapsed midnight
  or a device clock change to trigger, neither attempted yet.
- **"Today" bowel-movement count folding in walk reports** (§7) — the bug fix
  itself (`countBowelMovementsToday`, shared by `HomeViewModel` and the widget) is
  covered by direct unit tests (`BowelMovementCountsTest`,
  `WidgetTodayCountsTest`'s new walk case) and a new instrumented BDD test
  (`WalkBowelMovementCountFlowTest`) that logs a real walk and asserts the Today
  card's headline count reflects it — but that instrumented test hasn't actually
  been run (needs `connectedAndroidTest`, which per project memory must not run
  against a real device without a fresh backup first), and the manual click-test
  couldn't happen either since the test phone was locked. Trustworthy on the
  strength of the unit tests and a straightforward, well-isolated code change, but
  genuinely not yet observed live end to end.
- **Usual amount** (§2) — the schema migration (`MIGRATION_4_5`) was exercised for
  real (a genuine `adb install -r` over the real on-device database, confirmed
  clean via a stable process and no crash/SQL exception in logcat); the UI flow
  itself (setting one on a food, it pre-filling on selection, staying overridable)
  is written and unit/instrumented-test-covered but not yet click-tested live on a
  real device.
- **Photo capture** (§1) — the save/remove/thumbnail flow and camera launch/cancel
  are verified; an actual photo capture still needs a manual test (can't be
  automated via `adb` on the test phone's camera app).
- **Scan label (OCR)** (§2) — same limitation as photo capture above: camera launch
  and clean cancel-recovery are verified, but an actual label capture + on-device
  text recognition pass couldn't be automated and still needs a manual test.
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
