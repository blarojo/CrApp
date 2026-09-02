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

- Store whether poos were in a walk or at home or at night to compare data
- Display how many times she got up at night and how many poos she did on a walk - may be overlap with 1st
- Store and show her energy levels
- When the dog walker walks her we only get information of how many poos. Add an option on the + menu to add a walk info (just time of walk and number of bowel movements)
- Android watch feature - Add functionality for me to be able to enter data on an app on my watch. When opened, it should just display the poo icon, number of bowel movements on a given day and a + sign to add bowel movements. Food and the rest can be added only from the phone app.
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

## Specs for the above (draft — for review)

Each spec below expands one bullet from the previous section into something buildable.
Nothing here is scheduled or approved; review and cut/edit before any of it gets
promoted into `development-plan.md`. Specs are grouped by the schema/entity they'd
touch, since several turn out to be facets of the same change.

### 1. Bowel movement context: location + time-of-day

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
  spec 3 below), or stay independent? Recommend independent — not every walk poo comes
  from a walker-logged walk.

### 2. Energy level logging

Covers *"Store and show her energy levels."*

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
  the same way as spec 1's `Location`:

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

### 3. Walker-logged walk summary

Covers *"When the dog walker walks her we only get information of how many poos. Add
an option on the + menu to add a walk info (just time of walk and number of bowel
movements)."*

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
  can roll up "walk movements" from here instead of (or in addition to) spec 1's
  `location == "walk"` flag on individually-logged movements. **This overlaps with
  spec 1 and needs a decision**: are walker-reported counts kept as a separate summary
  row (this spec), or reconciled into `location` on real `bowel_movement` rows? Doing
  both risks double-counting on the dashboard.
- **Export:** add to CSV export and `BackupSerializer`, same as spec 2.

### 4. Wear OS companion app

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

### 5. Reminders / notifications

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
- **Open questions:** should the reminder also consider "night" hours (spec 1) so it
  doesn't fire a false alarm overnight before a sleeping human could act on it anyway?

### 6. Photo attachment on bowel movement entries

- **Data model:** add `photoUri: String?` to `bowel_movement` (stores a reference to
  the image, not the bytes themselves) — another `MIGRATION_2_3`-style schema bump,
  could potentially combine with spec 1's migration if both are built together.
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
  spec 7's OCR ask). Thumbnail shown in the History list row and in an entry's edit
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

### 7. Photo-based ingredient capture (OCR)

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

### 8. Multi-dog support

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

### 9. Structured dose/amount fields

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

### 10. Tap-to-inspect + adjustable dashboard window

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
  just one of these ten to promote into `development-plan.md`.
