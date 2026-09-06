# CrApp — Future Features Backlog

Ideas beyond the MVP described in [development-plan.md](development-plan.md). Nothing
here is scheduled — add to this list freely as things come up; prune or promote items
into active development as needed.

## Implementation & testing status

All 12 specs below were reviewed and 10 of them (excluding OCR and multi-dog) were
built in one batch, then click-tested on-device over several follow-up sessions. This
table is that status as of the most recent session — see each spec's own section
below for the full design, and `docs/app-functionality.md` for the always-current
feature reference.

| # | Feature | Implemented | Tested | Notes |
|---|---|---|---|---|
| 1 | Bowel movement amount (1–3 scale) | ✅ Yes | ✅ Yes | Amount chips (Some drips / Medium amount / A lot of poo); saved, edited, and displayed in History and the dashboard during device testing. |
| 2 | Tap-to-inspect + adjustable dashboard window | ✅ Yes | 🧪 Partial | Tap-to-inspect and the 7/14/30/90d window chips were verified on-device in an earlier session. This session added a 1d option and fixed real bugs (the window was filtering by movement *count* not calendar days; chart points were evenly spaced by index instead of by actual time; dog-walker-reported movements were invisible on the chart) — those fixes build clean and pass unit tests but haven't been click-tested yet (phone was disconnected this round). |
| 3 | Bowel movement location + night-time (+ Garden) | ✅ Yes | ✅ Yes | Night / Walk / Inside home / Garden chips, `isNightTime`, and the matching dashboard tiles/charts were all round-tripped on-device (save → dashboard updates → shows correctly in History → edit screen confirms it → delete → dashboard reverts). |
| 4 | Energy level logging | ✅ Yes | 🧪 Partial | The 5 named energy levels render correctly on the log screen and real logged entries display correctly in History/dashboard; no dedicated save-then-delete round-trip test was run for this one specifically. |
| 5 | Walker-logged walk summary | ✅ Yes | ✅ Yes | Full round-trip tested: saved a 3-count walk entry, confirmed the dashboard total updated correctly (this was also the exact bug fixed earlier in this session — the dashboard was silently ignoring these), deleted it, confirmed the total reverted. |
| 6 | Wear OS companion app | ✅ Yes | ❌ No | The `:wear` module and the phone-side sync service both build and install correctly, but there's no physical or emulated Wear OS watch available to actually test the watch UI or the phone↔watch sync. |
| 7 | Reminders / notifications | ✅ Yes | 🧪 Partial | The enable toggle, the real Android `POST_NOTIFICATIONS` permission prompt, the threshold chips, and the underlying WorkManager periodic job registration were all verified on-device (`dumpsys jobscheduler` shows it scheduled and already ran once). The real "no movement in 24h+" notification firing wasn't observed live, since that needs a real elapsed day with no logging. |
| 8 | Photo attachment | ✅ Yes | 🧪 Partial | The MediaStore save/remove/thumbnail flow is implemented; camera launch and cancel-cleanup were verified, but an actual successful photo capture couldn't be automated via `adb` (this phone's camera app doesn't respond to synthetic shutter taps) — a real photo capture still needs your manual test. |
| 9 | Structured ingredient data | ✅ Yes | ✅ Yes (backend only) | The `Ingredient`/`FoodIngredient` tables, the parser, and the synonym-based canonicalization were verified correct against the real 4 seeded foods via direct on-device database inspection. There's no UI to click-test, by design — it's backend plumbing feeding a future insights feature, not a user-facing screen yet. |
| 10 | Structured dose/amount fields | ✅ Yes | ✅ Yes | Food side fully round-tripped this session (value 1, unit "tin (400g)", confirmed via the edit screen, then deleted). Medication side: the dose value field and mg/ml/mcg unit chips were confirmed present and rendering correctly, but no dedicated save round-trip was run for medication dose specifically. |
| 11 | Photo-based ingredient capture (OCR) | ❌ No | ❌ No | Explicitly deferred/excluded from the implementation batch (needs ML Kit, a new dependency). |
| 12 | Multi-dog support | ❌ No | ❌ No | Explicitly deferred/excluded — the highest-risk, largest-scope item in this list; not started. |

The top-of-backlog "Good DevEx and snappy UI design" item isn't a discrete feature to
implement/test — it's an ongoing engineering principle this project has generally
followed (fast Gradle iteration, tap-first logging screens with no dead time), not
tracked in the table above.

## Backlog

- [ ] **Good DevEx and snappy UI design** — keep the app fast and pleasant to build on
  and use: quick Gradle build/iteration times, minimal boilerplate, responsive Compose
  UI with no jank on the logging screens (these get used multiple times a day, so any
  friction compounds).

## Other ideas worth considering later

- Add an amount-of-poo scale to bowel movement entries: 1 = some drips, 2 = medium
  amount, 3 = a lot of poo.
- Store whether poos were in a walk or at home or at night to compare data
- Display how many times she got up at night and how many poos she did on a walk - may be overlap with 1st
- Store and show her energy levels
- When the dog walker walks her we only get information of how many poos. Add an option on the + menu to add a walk info (just time of walk and number of bowel movements)
- Android watch feature - Add functionality for me to be able to enter data on an app on my watch. When opened, it should just display the poo icon, number of bowel movements on a given day and a + sign to add bowel movements. Food and the rest can be added only from the phone app.
- Reminders/notifications (e.g. "no movement logged in over 24h").
- Photo attachment on bowel movement entries.
- Structure the ingredient lists for the 4 currently-defined foods (looked up online
  and already stored as free text in Phase 8's `SeedFoods.kt`) into discrete,
  queryable ingredients per food, so a future feature can infer allergy/trigger
  correlations from individual ingredients rather than free-text blobs.
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

## Specs for the above (draft — for review)

Each spec below expands one bullet from the previous section into something buildable.
Nothing here is scheduled or approved; review and cut/edit before any of it gets
promoted into `development-plan.md`. Specs are grouped by the schema/entity they'd
touch, since several turn out to be facets of the same change.

### 1. Bowel movement amount

Covers *"Add amount of poo to bowel movement. It should have a scale of 1 to 3, being:
some drips, medium amount, a lot of poo."*

- **Data model:** add `amount: Amount?` to `bowel_movement` — a fixed 3-value enum,
  following the same named-level convention established for spec 4's `EnergyLevel`
  (and spec 3's `Location`) rather than a bare `Int`, since each level already has a
  specific label given up front:

  ```kotlin
  enum class Amount(val displayName: String) {
      SOME_DRIPS("Some drips"),
      MEDIUM_AMOUNT("Medium amount"),
      A_LOT("A lot of poo")
  }
  ```

  Ordered low → high so it sorts/plots by `ordinal`, same as `EnergyLevel`. Needs a
  `Converters.kt` type converter, same pattern as the other two enums.
  - **Nullable, not required:** unlike `consistency` (non-nullable today), `amount`
    should be optional (`Amount?`) so the migration doesn't have to invent a fabricated
    value for every already-logged historical row — matches how `color`/`notes` are
    already optional on the same table. The logging screen can still default-select
    "Medium amount" on the picker to make logging it feel like the norm, without the
    column itself being `NOT NULL`.
  - Requires a Room migration (`MIGRATION_2_3`) — natural to combine with spec 3's
    location/night-time fields below, since both add columns to `bowel_movement` in
    the same version bump rather than shipping back-to-back migrations for the same
    table.
- **Logging UI:** add a 3-option chip/tap selector to `BowelMovementLogScreen`,
  alongside the existing consistency 1–7 selector — same tap-first interaction the
  rest of the logging screen already uses (this app's "snappy UI design" backlog item
  explicitly calls out avoiding friction on screens used multiple times a day).
- **History/dashboard:** show the amount label on each History row next to
  consistency; a dashboard trend line is optional/lower priority than for consistency,
  since amount is a coarser 3-point read.
- **Export:** add `amount` (the `displayName`, not the raw enum constant) to CSV
  export and to `BackupSerializer`.
- **Open question:** does amount ever move independently of consistency (e.g. a small
  amount that's still liquid/high on the 1–7 scale)? Likely yes — recommend keeping
  them as two independent fields rather than deriving one from the other, which is
  already how this spec is written; flagging it here so that's read as intentional,
  not an oversight.

### 2. Tap-to-inspect + adjustable dashboard window

- **Tap-to-inspect:** the Phase 7 dashboard trend charts need a tap handler per data
  point (Compose Canvas hit-testing against the plotted points, or swap to a charting
  approach that supports it natively) that shows exact date + value, e.g. in a small
  tooltip/bottom sheet — replacing hover, which touch devices don't have.
- **Adjustable window:** replace the current fixed "last 14 points" / "last 7 days"
  windows with a selector (e.g. 7d / 14d / 30d / 90d chip row above each chart,
  matching the History screen's existing filter-chip UX). Dashboard `ViewModel`s
  already query by date range for their trend data, so this is mostly a UI + query
  parameter change, not a schema change.
- **Data model:** none.
- **Scope note:** smallest, lowest-risk item in this whole list — pure UI/query work
  on an existing screen, no migration, no new dependency. Good first pick if picking
  just one of these twelve to promote into `development-plan.md`.

### 3. Bowel movement context: location + time-of-day

Covers *"Store whether poos were in a walk or at home or at night"* and *"Display how
many times she got up at night and how many poos she did on a walk"* — the second is
just a rollup of the first, so one schema change covers both.

- **Data model:** add two fields to `bowel_movement`:
  - `location: Location?` — a fixed enum rather than the free-text convention used
    elsewhere (`color`, dose/amount fields), since "home/walk/other" is a small, known,
    stable set up front — no need to wait for usage data to justify it here. New
    `Location` enum (`ui/model` or alongside `MealType`, following that existing
    enum's placement convention):
    ```kotlin
    enum class Location { HOME, WALK, OTHER }
    ```
    When `OTHER` is selected, a `locationOther: String?` free-text field on
    `bowel_movement` becomes enabled/required in the UI (mirrors the "Add new" inline
    free-text pattern already used for foods in the Food dropdown) so an
    unanticipated location isn't lost or forced into `HOME`/`WALK`. `locationOther` is
    only meaningful when `location == OTHER`; leave it `null` otherwise. `Converters.kt`
    needs a `Location` <-> `String` type converter, matching how existing enums
    (`MealType`) are persisted.
  - `isNightTime: Boolean` — derived automatically from `timestamp` at save/query time
    against a configurable night-window setting (default e.g. 10pm–6am), not
    hand-entered, so it's consistent day to day. Could equally be computed on read
    instead of stored — stored is simpler for the dashboard rollups below but must be
    recomputed if the night-window setting ever changes.
  - Requires a Room migration (`MIGRATION_2_3`) — see `AppDatabase.kt` header comment on
    why a version bump always needs a real `Migration`, never
    `fallbackToDestructiveMigration()`.
- **Logging UI:** add a location chip-select (Home / Walk / Other, matching the
  existing quick-tap style of the consistency 1–7 selector) to
  `BowelMovementLogScreen`. Selecting "Other" reveals an inline text field for
  `locationOther` (hidden otherwise); leaving it blank while "Other" is selected should
  be treated the same as not filling in an optional field elsewhere in the app (allowed,
  just less useful data) rather than blocked with a validation error. Night-time is
  inferred, not a UI control.
- **History/dashboard:** two new stat tiles — "night movements" and "walk movements" —
  count per day/week, joinable with the existing dashboard trend charts (Phase 7).
- **Open questions:** should "walk" location also require/allow a walk-time entry (see
  spec 5 below), or stay independent? Recommend independent — not every walk poo comes
  from a walker-logged walk. **Resolved (see spec 5):** `location == WALK` on an
  individually-logged movement and a spec 5 `walk_entry` summary are both kept, used
  for two different situations (the user personally walking and logging each
  movement, vs. the dog walker reporting only a count) — not double-entered for the
  same walk. The app doesn't enforce this; see spec 5's UI warning.
- **Implemented update:** shipped as `enum class Location(val displayName: String) {
  HOME("Inside home"), GARDEN("Garden"), WALK("Walk"), OTHER("Other") }` — a `GARDEN`
  value was added (outdoor-but-not-a-walk is common enough to want its own bucket
  rather than falling into `OTHER`'s free text), and `HOME`'s UI label changed to
  "Inside home" so it reads unambiguously next to "Garden". No migration needed
  (`Location` is still stored as its constant name via `Converters.kt`, and `GARDEN`
  is just a new valid string). The dashboard also gained a per-day breakdown chart
  and window stat tile for each of Walk/Night/Inside home/Garden.

### 4. Energy level logging

Covers *"Store and show her energy levels."*

**Build together with spec 5 (walker-logged walk summary):** both are the same shape
of work — a small new entity, a one-field-plus-notes log screen, a `+` menu entry,
CSV/backup wiring — so implement the "simple log type" scaffolding once and reuse it
for both rather than deriving the pattern twice.

- **Data model:** new entity `energy_entry`, same shape as `medication_entry`:

  | Field | Type | Notes |
  |---|---|---|
  | id | Long (PK, autogenerate) | |
  | timestamp | Instant | |
  | level | EnergyLevel | named 5-point scale, not a bare 1–5 int — see below |
  | notes | String? | free text |

  Named rather than numeric, since "energy" is a subjective daily read (unlike the
  clinical Purina consistency scale) and a name is faster to recognize at a glance
  than remembering what a number meant last time. New `EnergyLevel` enum (same
  placement convention as `MealType`), persisted via a `Converters.kt` type converter
  the same way as spec 3's `Location`:

  ```kotlin
  enum class EnergyLevel(val displayName: String) {
      SLEPT_ALL_DAY("Slept all day"),
      LOW_ENERGY("Low energy"),
      NORMAL("Normal"),
      A_BIT_PLAYFUL("A bit playful"),
      A_LOT_OF_ENERGY("A lot of energy")
  }
  ```

  Ordered low → high so dashboard trend charts (which need a numeric y-axis) can still
  sort/plot by `ordinal`. Names are a first pass — worth confirming wording against
  what actually gets typed/said day to day before building.

- **Logging UI:** new `ui/energy/EnergyLogScreen.kt` + ViewModel, same shape as the
  existing bowel/food/medication log screens; a 5-option tap selector showing the
  `displayName`s above (not raw numbers) plus notes. Add an entry point on the `+`
  quick-add menu (`HomeScreen`) alongside the existing three.
- **History/dashboard:** add to the History filter type list; add an energy trend line
  to the Phase 7 dashboard charts alongside consistency.
- **Export:** add `energy_entry` to CSV export and to `BackupSerializer` (new entities
  must be included in backup/restore, per the existing pattern for the other three
  entities).
- **Open questions:** is energy logged once/day, or multiple times? If once/day, a
  simpler `UNIQUE(date)` constraint might be more correct than a plain log table —
  needs a decision before implementing.

### 5. Walker-logged walk summary

Covers *"When the dog walker walks her we only get information of how many poos. Add
an option on the + menu to add a walk info (just time of walk and number of bowel
movements)."*

**Build together with spec 4 (energy level logging):** both are the same shape of
work — a small new entity, a one-field-plus-notes log screen, a `+` menu entry,
CSV/backup wiring — so implement the "simple log type" scaffolding once and reuse it
for both rather than deriving the pattern twice.

- **Data model:** new entity `walk_entry`:

  | Field | Type | Notes |
  |---|---|---|
  | id | Long (PK, autogenerate) | |
  | timestamp | Instant | walk start time (or end time — pick one and document it) |
  | bowelMovementCount | Int | count reported by the walker, not individually detailed |
  | notes | String? | free text |

  This is deliberately a lightweight summary row, separate from individual
  `bowel_movement` rows — the walker only reports a count, not per-movement
  consistency/color/etc., so it can't be modeled as N real `BowelMovement` inserts
  without inventing data that wasn't observed.
- **Logging UI:** new quick-add option "Walk" on the `+` FAB menu (`HomeScreen`) next
  to Bowel/Food/Medication; a minimal screen with just a time picker (defaults to now)
  and a number stepper for movement count.
- **History/dashboard:** walk entries show in History as their own row type; dashboard
  rolls up "walk movements" from here, kept **separate from and additive to** spec 3's
  `location == WALK` flag on individually-logged movements rather than merged into it.
- **Resolved — coexistence, not merge:** this entry is specifically for the dog
  walker's report (a count only, no per-movement detail); when the user herself walks
  the dog and logs each movement individually, she tags those with `location == WALK`
  (spec 3) instead of also adding a `walk_entry` for the same walk. The two paths cover
  two different real situations (who did the walking / how much detail is available),
  so both stay in the data model — the risk is the user accidentally logging the same
  walk both ways, not the schema needing to pick one. The app doesn't enforce this
  programmatically (no way to know "this walk was already logged the other way"); add
  a small inline warning/reminder on this screen instead, e.g. "Only log this if you
  didn't already tag individual movements as 'Walk' for this outing," so the choice is
  visible at the moment of logging rather than relying on memory alone.
- **Export:** add to CSV export and `BackupSerializer`, same as spec 4.

### 6. Wear OS companion app

Covers *"Android watch feature — enter data on an app on my watch: poo icon, number of
bowel movements today, and a + to add one. Food and the rest stay phone-only."*

- **Scope:** a separate Wear OS module (`wear/`), not just a phone-UI tweak — this is
  the largest item in this list by far.
- **Data model:** no new entities; the watch app reads/writes the existing
  `bowel_movement` table. Needs a sync path between phone and watch storage since
  they're separate processes/devices:
  - Simplest viable approach: Wearable Data Layer API (`DataClient`/`MessageClient`)
    to relay "log a bowel movement now" from watch → phone, where the phone's existing
    `BowelMmovementDao` does the actual insert. Avoids a second source of truth or a
    second Room database to keep consistent.
  - Watch UI needs *today's count* to display — either the phone pushes a small
    `DataClient` payload (date + count) on every change, or the watch requests it live;
    push is more resilient to the watch being briefly unreachable (BLE out of range).
- **Watch UI:** single screen — poo icon, today's count (large text), a `+` button.
  Tapping `+` sends the log-a-movement message immediately with `timestamp = now` and
  no consistency/notes (those stay phone-only per the requirement) — so this also
  needs a decision on what `consistency` value a watch-originated row gets, since the
  column is currently non-nullable `Int`. Options: make `consistency` nullable
  (schema change affecting all existing rows/screens), or default to a sentinel like
  `0`/"unset" that the phone UI treats as "needs follow-up." Needs a decision before
  implementation — this is the trickiest modeling question in this spec.
- **Requires:** a Wear OS-capable physical device or emulator for testing (separate
  from the existing phone-only test setup), and the `connectedAndroidTest` caveat in
  memory (backs up real data first) would extend to whatever device pairing is used
  for watch testing too.

### 7. Reminders / notifications

Covers *"Reminders/notifications (e.g. 'no movement logged in over 24h')."*

- **Mechanism:** `WorkManager` periodic work (already idiomatic for "check a condition
  on a schedule, survives process death/reboot" on Android) — a daily (or every few
  hours) worker queries `bowelMovementDao` for the most recent `timestamp`, and posts a
  local notification if `now - lastTimestamp > 24h` (threshold configurable in
  Settings).
- **Data model:** no new entities. New `NOTIFICATIONS_ENABLED` / threshold-hours
  preference in the existing `data/prefs` DataStore.
- **Permissions:** Android 13+ (API 33+) requires runtime `POST_NOTIFICATIONS`
  permission — needs an in-app request flow (Settings screen toggle triggers the
  permission prompt), plus a notification channel setup in `CrAppApplication`.
- **UI:** new toggle + threshold picker in `SettingsScreen`. Tapping the notification
  deep-links into the bowel-movement log screen.
- **Open questions:** should the reminder also consider "night" hours (spec 3) so it
  doesn't fire a false alarm overnight before a sleeping human could act on it anyway?

### 8. Photo attachment on bowel movement entries

- **Data model:** add `photoUri: String?` to `bowel_movement` (stores a reference to
  the image, not the bytes themselves) — another `MIGRATION_2_3`-style schema bump,
  could potentially combine with spec 3's migration if both are built together.
- **Storage — must NOT be app-private storage.** `context.filesDir` /
  `getExternalFilesDir()` are both deleted the moment the app is uninstalled, which
  defeats the point of a photo log (uninstall/reinstall — e.g. switching phones, or the
  `connectedAndroidTest` mishap noted in memory — would silently wipe every photo).
  Instead, save into a **shared, user-visible folder that survives uninstall**, using
  the `MediaStore` API (required on Android 10+/scoped storage; no broad storage
  permission needed for images the app itself creates):
  - Insert into `MediaStore.Images` with
    `RELATIVE_PATH = "Pictures/CrApp"` (or `DCIM/CrApp` — `Pictures/CrApp` reads more
    like an album, not a camera roll — matching the `data/prefs`-style naming already
    used elsewhere). This creates a real, sensible `Pictures/CrApp/` folder the user
    can browse in any file manager or gallery app, exactly like a normal camera photo
    album — not something buried in app-internal storage.
  - Store the resulting `content://` `MediaStore` URI string in `photoUri`. Because the
    app owns the row it inserted (same package name), it keeps read/write access to
    that URI across app updates *and* across an uninstall+reinstall — MediaStore
    ownership is keyed by package name, not by a live install — so the photo and the
    ability to open it both survive exactly the same events that used to wipe
    app-private storage. This directly satisfies the original ask: images aren't
    removed on uninstall and reinstall.
  - No cleanup-of-app-private-files routine is needed as a result (the earlier draft's
    concern about orphaned app-private copies goes away); deleting an entry's photo
    should still delete the underlying `MediaStore` row via `ContentResolver.delete()`
    so the shared `Pictures/CrApp` folder doesn't accumulate orphans the app itself
    created.
- **UI:** camera-or-gallery picker button on `BowelMovementLogScreen` (Android's
  built-in `ActivityResultContracts.TakePicture` targeting a `MediaStore`-issued URI, or
  `PickVisualMedia` for an existing photo — no new library dependency needed, unlike
  spec 11's OCR ask). Thumbnail shown in the History list row and in an entry's edit
  view; tap to view full-size.
- **Backup/restore:** since `photoUri` is now a stable reference into shared storage
  rather than an app-private path, `BackupRepository`/`BackupSerializer` only need to
  back up that URI string alongside the rest of the entry's fields — **no change to the
  existing JSON-only backup format**, and no need to bundle image bytes into the backup
  file. This resolves the open question the original draft flagged. Caveat worth
  documenting: a backup restored onto a *different* phone (not the original device)
  won't have that device's `Pictures/CrApp` folder, so `photoUri` would resolve to
  nothing — the UI should show a graceful "photo not found" placeholder rather than
  crash in that case, same-device uninstall/reinstall being the primary case this
  storage choice actually guarantees.
- **Privacy note:** `Pictures/CrApp` photos are visible to any app with photo-library
  access (e.g. show up in the phone's general Gallery app, not just inside CrApp) —
  worth a one-line mention in Settings/docs so that's an expected tradeoff of "easy to
  find in a file manager," not a surprise.

### 9. Structured ingredient data for the current food catalog

Covers *"For the foods we have defined currently (the 4 options I defined) look up
the ingredients online and store them somewhere so that we have the structure of
ingredients for a specific food — this will help us infer data for allergies etc."*

- **Context:** the lookup itself already happened — `SeedFoods.kt` (Phase 8) already
  stores each of the 4 seeded foods' online-sourced ingredient list in
  `Food.ingredients`, as a single free-text comma-separated blob per food (z/d Mini
  Food Sensitivities dry, z/d Food Sensitivities wet, HA Hypoallergenic Mousse, HA
  Hypoallergenic Dry — see that file's header comment for the "as of Sept 2026,
  verify against packaging if it matters" caveat, which still applies here). What's
  missing for the allergy-inference goal is *structure*: right now correlating "does
  chicken liver show up in every food she reacted badly to" means string-matching
  over four free-text blobs, which is fragile and doesn't scale to more foods.
- **Data model:** two new normalized tables, additive to (not replacing) the existing
  free-text `Food.ingredients` field — that field stays as the source-of-truth label
  text / fallback for anything that doesn't cleanly decompose, same "structured is
  additive" principle as spec 10's dose/amount fields below.

  | Table | Fields | Notes |
  |---|---|---|
  | `ingredient` | id (PK, autogenerate), name (unique) | canonical catalog, e.g. "coconut oil", "hydrolyzed chicken liver" — deduped across foods |
  | `food_ingredient` | foodId (FK → food.id), ingredientId (FK → ingredient.id), position (Int) | join row per food+ingredient; `position` preserves the label's original order, since pet-food labels list ingredients by descending concentration — that ordering carries real meaning for an allergy read, not just display |

  Another `MIGRATION_2_3`-style schema bump (candidate to combine with specs 3 and 8
  if several of these ship together).
- **Populating the 4 current foods:** a one-time backfill, not a migration script —
  parse each `SeedFood.ingredients` string (split on commas, trim), canonicalize each
  token, dedupe against existing `ingredient` rows, insert `food_ingredient` rows
  preserving order. Write this as a small reusable parser (not a one-off script) since
  any food added later (manual entry, or the OCR spec below) needs the same
  text-to-structure step applied to its `ingredients` field.
- **Open question — canonicalization is the hard part:** the 4 existing labels
  already disagree on naming for the same thing (e.g. Hill's z/d dry lists "Maize
  starch" while Hill's z/d wet — same manufacturer — lists "Corn starch" for what's
  functionally the same ingredient). Splitting on commas alone would create duplicate
  `ingredient` rows for the same real ingredient and quietly break the correlation
  this feature exists for. Needs a small synonym/alias map (maize=corn, at minimum)
  reviewed before trusting any allergy inference built on top of it, and a decision on
  whether generic catch-all phrases like "vitamins and trace elements" become one
  `ingredient` row (not useful for correlation) or get dropped entirely during parsing.
- **What this unlocks:** this is what the "infer data for allergies" goal actually
  needs underneath it — a future insights feature (or an extension of the
  `crapp-insights` skill) can then query "for every `bowel_movement` with
  `hasBlood`/`hasMucus`/low consistency, which `ingredient` rows are common across the
  `food_entry` rows logged in the preceding N hours" instead of pattern-matching free
  text. Flag this as the concrete consumer that justifies doing the parsing work now
  rather than speculatively.

### 10. Structured dose/amount fields

Already scoped as deferred-until-justified in the original bullet.

- **Data model:** would replace/augment the free-text `dose` on `medication_entry` and
  `amount` on `food_entry` with `amountValue: Double?` + `amountUnit: String?` pairs
  (nullable, so existing free-text-only rows remain valid — or run a best-effort
  migration parser over existing free text, which is risky given how inconsistent
  real-world dosing text tends to be; recommend nullable-additive fields over a lossy
  parse-and-replace).
  - Keep the original free-text field too, unstructured, as a fallback for anything
    that doesn't fit `value + unit` (e.g. "half a pill", "as needed"). Structured
    fields are additive, not a replacement, per the original note's own reasoning for
    why they're deferred.
- **UI:** log screens gain a numeric field + unit dropdown (mg/ml/mcg for meds;
  cup/tbsp/g for food) alongside the existing free-text field.
- **Why deferred:** per the original bullet, needs real usage data to show the current
  free-text fields are actually limiting the insights feature before paying this
  complexity cost — this spec is ready to go whenever that evidence shows up, likely
  surfaced by `crapp-insights` runs that can't cleanly bucket dose amounts.

### 11. Photo-based ingredient capture (OCR)

Already scoped as deferred-until-justified in the original bullet; spec captured here
so it's ready when that justification shows up.

- **Dependency:** ML Kit Text Recognition (on-device, no network call — keeps the
  app's "no backend, no third-party analytics" constraint intact). Real Gradle
  dependency addition, consistent with why this was deferred.
- **UI:** on the Food Catalog "Add new" / edit food flow (`ui/foodcatalog`), add a
  "scan label" button next to the existing manual/pasted-text `ingredients` field.
  Opens camera, runs ML Kit text recognition on the captured frame, and pre-fills the
  `ingredients` text field with the recognized text for the user to review/edit before
  saving — never auto-saves OCR output unreviewed, since label OCR is commonly noisy.
- **Data model:** none — writes into the existing `Food.ingredients` free-text field.
- **Prereq:** per the original note, build and ship the manual-entry `ingredients`
  field's usage first; this spec only becomes worth implementing once that shows
  photo capture would actually save meaningful time over typing/pasting.

### 12. Multi-dog support

- **Data model:** the big one — introduces a `Dog` entity and a `dogId` FK on
  `bowel_movement`, `food_entry`, `medication_entry`, and any of the new entities
  above (`energy_entry`, `walk_entry`) that ship before this does. `Food` (the catalog)
  and its `ingredients` likely stay dog-independent (a food is a food regardless of
  which dog ate it), but `FoodEntry` needs `dogId`.
- **Migration:** substantial — every existing row needs to be backfilled with a
  single "default" `Dog` row (representing the current dog) so existing data doesn't
  become orphaned. This is the highest-risk migration of anything in this list; needs
  its own dedicated migration test (following the existing `MigrationTest.kt` pattern)
  before ever shipping.
- **UI:** every log/history/dashboard/export screen needs a dog selector or filter.
  Given this app started single-dog-single-user by design (see
  `development-plan.md` §1), this is effectively a second app shape and should be
  scoped as its own mini development-plan phase rather than a single PR, if it's ever
  picked up.
- **Recommendation:** lowest priority in this list unless a second dog is actually
  imminent — cost is high and every other feature above becomes slightly more complex
  to build on top of once `dogId` scoping exists everywhere.
