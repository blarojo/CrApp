# CrApp — Backlog

Ideas beyond the MVP described in [development-plan.md](development-plan.md). Nothing
here is scheduled — add to this list freely as things come up; prune or promote items
into active development as needed. (Formerly `future-features.md` — renamed once it
started holding more than just "not built yet" ideas, e.g. specs already promoted but
still pending a merge.)

## Shipped features

Specs 1–11 from an earlier draft of this file (bowel movement amount, tap-to-inspect +
adjustable dashboard window, location + night-time, energy logging, walker-logged
walks, the Wear OS companion app, reminders/notifications, photo attachment,
structured ingredient data, structured dose/amount fields, and AI-generated
insights v2) have all shipped and been documented as real features — see
[app-functionality.md](app-functionality.md) for what each one does today and its
current testing status, and [development-plan.md](development-plan.md) for when each
was built. They've been removed from this file since they're no longer "future" —
this file now holds only what's still pending.

## Backlog

- [ ] **Good DevEx and snappy UI design** — keep the app fast and pleasant to build on
  and use: quick Gradle build/iteration times, minimal boilerplate, responsive Compose
  UI with no jank on the logging screens (these get used multiple times a day, so any
  friction compounds). An ongoing engineering principle rather than a discrete feature
  to implement/test.

## Other ideas worth considering later

- Photo-based ingredient capture (OCR a label photo) for the Food Catalog, on top of
  the manual/pasted-text entry already shipped — needs a camera + text-recognition
  capability (e.g. ML Kit), a real dependency addition, so deferred until the
  text-entry version shows it's worth the jump. See spec 12 below.
- Multi-dog support (would require introducing a `Dog` entity and scoping all
  queries). See spec 13 below.
- Predefined food amount quick-buttons ("1 cup", "1 tin (400g)") on the food-logging
  screen, to save a few taps on the most common portions. See spec 14 below.
- A home screen widget for quick-glance counts and one-tap logging, without opening
  the app first. See spec 15 below.

## Specs for the above (draft — for review)

Nothing here is scheduled or approved; review and cut/edit before any of these gets
promoted into `development-plan.md`.

### 12. Photo-based ingredient capture (OCR)

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

### 13. Multi-dog support

- **Data model:** the big one — introduces a `Dog` entity and a `dogId` FK on
  `bowel_movement`, `food_entry`, `medication_entry`, `energy_entry`, and
  `walk_entry`. `Food` (the catalog) and its `ingredients` likely stay
  dog-independent (a food is a food regardless of which dog ate it), but `FoodEntry`
  needs `dogId`.
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

### 14. Predefined food amount quick-buttons

Covers *"Pre defined food amounts, please add a button for '1 cup' and '1 tin
(400g)'."*

**Status: not started — spec only, do not build yet.**

- **Problem:** `FoodLogScreen` already has a structured amount value+unit pair
  (spec 10, shipped — see `app-functionality.md` §2) with a unit dropdown that
  includes `cup` and `tin (400g)` among its options, but every entry still requires
  typing/picking the numeric value and unit from scratch each time, even though in
  practice most entries are one of a small number of common portions. This is
  exactly the kind of repeated-friction case the backlog's "Good DevEx and snappy UI
  design" principle calls out.
- **Requirements:**
  1. Add two quick-fill buttons/chips on `FoodLogScreen`, labeled **"1 cup"** and
     **"1 tin (400g)"**, positioned near the existing amount fields (structured
     value/unit + free-text amount).
  2. Tapping a button fills **both** the structured fields (`amountValue = 1.0`,
     `amountUnit = "cup"` or `"tin (400g)"`) **and** the free-text `amount` field
     (`"1 cup"` / `"1 tin (400g)"`) in one tap, so a report or CSV row that only
     looks at the free-text field still reads correctly.
  3. The fields stay directly editable after a quick-fill tap — this is a starting
     point, not a locked value. Tapping a different quick-fill button, or the same
     one again, simply overwrites the fields (no toggle/undo state to track).
  4. Quick-fill buttons don't replace the existing manual entry path — someone
     feeding a different amount (half a tin, 2 cups) still uses the existing numeric
     field + unit dropdown as today.
- **Data model:** none — reads/writes the existing `FoodEntry.amount` /
  `amountValue` / `amountUnit` fields (spec 10). No migration needed.
- **UI:** two `FilterChip`- or `AssistChip`-style tap targets (matching the app's
  existing chip-selector convention used throughout the logging screens), likely a
  small `Row` directly above or below the amount fields on `FoodLogScreen`. Exact
  copy: "1 cup" and "1 tin (400g)" (matches the user's own wording verbatim, and the
  existing `amountUnit` option strings, so no new unit string needs inventing).
- **Open questions:**
  - Should the list of quick-fill buttons be a hardcoded pair, or backed by a small
    list so more can be added later without another spec (e.g. a per-food "usual
    amount" inferred from history)? Recommend a small `data class QuickAmount(label,
    value, unit)` list literal to start — trivial to extend later, no premature
    abstraction (no catalog/DB table) for just two fixed buttons.
  - Do the two buttons apply globally, or should they eventually vary by selected
    food (e.g. treats logged by count, not cup/tin)? Out of scope for this spec —
    ship the two fixed, food-independent buttons first; revisit if usage shows a
    food-specific need.
- **Testing requirements:**
  - Unit test (`FoodLogViewModelTest` or equivalent): tapping each quick-fill button
    sets `amountValue`, `amountUnit`, and `amount` to the expected values on the
    screen's UI state; confirm a subsequent manual edit still overrides them (no
    stale/locked state).
  - On-device click test: tap "1 cup", confirm both the structured unit dropdown and
    the free-text field reflect it, save, confirm it round-trips correctly through
    History's edit view and through CSV export's `amount`/`amount_value`/
    `amount_unit` columns. Repeat for "1 tin (400g)". Confirm manual entry (a food
    logged without tapping a quick-fill button) is unaffected.
- **Documentation requirements:**
  - Update `app-functionality.md` §2 (Food logging + Food Catalog) to describe the
    two quick-fill buttons as a shipped feature once built, including their exact
    labels and what they fill.
  - Update `app-functionality.md`'s Testing status section once click-tested.
  - No `development-plan.md` change needed unless this gets promoted into a phase —
    a `backlog.md` spec update (status line here) is enough while it's still pending.

### 15. Home screen widget

Covers *"Create a widget which I can add to my phone home screen ... summary
information ... Add bowel movement ... Add food."*

**Status: not started — spec only, do not build yet.**

- **Problem:** every log entry today requires opening the app first, even for the
  two most frequent actions (bowel movement, food). A home screen widget gives an
  at-a-glance summary and one-tap logging without that step — directly serving the
  "used multiple times a day" pattern this app is built around.
- **Requirements:**
  1. **Summary row** — the same at-a-glance numbers `HomeScreen`'s "Today" hero card
     already shows: today's bowel-movement count, plus (when non-zero, matching that
     card's existing "also today: …" convention) today's food and energy counts.
     Example, matching the user's own wording: "💩 6 · 🍗 1 · ⚡ 1". Medication/walk
     counts follow the same "only shown if non-zero today" rule as the phone
     dashboard, for consistency and to keep the widget compact.
  2. **"Add BM" button** — opens the app directly on the Log Bowel Movement screen
     (reuses the same deep-link mechanism `MainActivity` already has for reminder
     notifications — see `MainActivity.EXTRA_OPEN_LOG_BOWEL_MOVEMENT` — rather than
     inventing a second path to the same destination).
  3. **"Add Food" button** — opens the app directly on the Log Food screen (a new
     equivalent `MainActivity` extra + `CrAppNavHost` deep-link, following the exact
     pattern the bowel-movement one already establishes).
  4. The widget reflects new entries logged from the phone app itself without
     needing to be manually removed/re-added (see "Refresh strategy" below).
- **Data model:** none new — reads today's counts from the existing DAOs
  (`BowelMovementDao`, `FoodDao`, `EnergyEntryDao`), the same queries
  `HomeViewModel` already runs for the "Today" card. No migration.
- **Architecture:**
  - Build on **Jetpack Glance** (the Compose-based widget API), not classic
    `RemoteViews` — keeps the same UI toolkit/idiom as the rest of the app rather
    than introducing a second, older widget-authoring style.
  - New `widget/` package in the existing `app` module (this is a phone-launcher
    surface, not a separate installable app/process the way `:wear` is — no new
    Gradle module needed).
  - `MainActivity` gains one new intent extra (`EXTRA_OPEN_LOG_FOOD`, alongside the
    existing `EXTRA_OPEN_LOG_BOWEL_MOVEMENT`) so both quick-add buttons deep-link the
    same way.
- **Refresh strategy (needs a decision before implementation):**
  - Android widgets don't get live pushes on data change for free — a Glance widget
    needs an explicit `update`/`updateAll` call. Recommend calling that from the
    existing repository write paths (bowel movement / food / energy insert-update-
    delete already funnel through a small number of repository methods) rather than
    polling on a timer, so the widget reflects a change made from the app almost
    immediately, and so a stale/battery-wasting periodic poll isn't needed.
  - Also register a periodic fallback refresh (e.g. hourly via `WorkManager`, not
    the legacy `updatePeriodMillis` widget metadata field, to stay consistent with
    how Reminders' periodic job is already built) to correct for the local day
    rolling over past midnight while the widget sits idle and unchanged.
- **UI:** compact — target Android's standard minimum resizable widget footprint
  (roughly 2x1 or 2x2 cells) since this widget is meant to be glanced at, not to
  replace the app. Follows the app's existing color scheme/typography where Glance's
  more limited theming API allows (Glance widgets don't get the full Compose
  Material 3 theme — expect a deliberately simpler visual treatment, not a
  pixel-perfect match to `HomeScreen`).
- **Open questions:**
  - Widget-add flow: does Android's system widget picker need a custom preview
    image? (Recommend yes — a real preview, not the default gray placeholder, so the
    widget is recognizable in the picker.)
  - Multiple widget instances: out of scope to support meaningfully different
    configurations per instance (e.g. one widget per hypothetical future dog, see
    spec 13) — one configuration, add it as many times as the user wants, all
    showing the same data.
- **Testing requirements:**
  - Unit test the counts-for-today calculation the widget uses — ideally by
    extracting/reusing the exact logic `HomeViewModel` already has (not a
    reimplementation that could drift), and unit-testing that shared function
    directly against a fake/in-memory repository for a few count combinations
    (zero entries, all-categories-populated, only-bowel-movements).
  - Glance's own UI is not meaningfully unit-testable end-to-end (no headless
    widget-host test harness comparable to Compose UI testing) — so this needs a
    real on-device pass:
    - Add the widget to a real home screen; confirm the summary numbers match the
      app's own Today card at the same moment.
    - Tap "Add BM"; confirm the app opens directly on the Log Bowel Movement screen
      (not Home first).
    - Tap "Add Food"; confirm the app opens directly on the Log Food screen.
    - Log a new bowel movement from inside the app, return to the home screen
      without removing/re-adding the widget, confirm the count updated (verifies the
      refresh-on-write path, not just the widget's initial render).
    - Leave the widget untouched across a local-midnight rollover (or simulate by
      changing the device clock), confirm the periodic fallback refresh resets
      "today's" count rather than continuing to show the previous day's number.
  - Standard `connectedAndroidTest` caveat applies if any instrumented test touches
    the real database — back up first, per project memory.
- **Documentation requirements:**
  - New `app-functionality.md` section once built (e.g. "§16. Home screen widget"),
    documenting: what it shows, the two buttons and where they deep-link, the
    refresh strategy/its limits (not instantaneous across a midnight rollover
    without the fallback job), and its testing status.
  - Update `app-functionality.md`'s Testing status section once click-tested.
  - Update `docs/install-on-phone.md` if adding the widget to a home screen needs
    any one-time step beyond the standard Android "long-press home screen → Widgets"
    flow.
  - No `development-plan.md` change needed unless this gets promoted into a phase —
    a `backlog.md` spec update (status line here) is enough while it's still
    pending.
