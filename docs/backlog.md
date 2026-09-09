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
insights v2), plus **spec 14 (predefined food amount quick-buttons)**,
**spec 12 (photo-based ingredient capture / OCR, plus the Food Catalog's own
manual "Add new food" flow and alphabetical ordering that came with it)**, and
**spec 15 (home screen widget)**, have all shipped and been documented as real
features — see [app-functionality.md](app-functionality.md) for what each one does
today and its current testing status, and
[development-plan.md](development-plan.md) for when each was built. They've been
removed from this file since they're no longer "future" — this file now holds only
what's still pending. (Spec 15 has since been click-tested live on-device too —
placement, summary numbers, both quick-add buttons, and the reactive refresh path
all confirmed working; only the hourly midnight-rollover fallback remains
unobserved live — see app-functionality.md §14's Testing status entry.)

**Good DevEx and snappy UI design**, the one ongoing engineering-principle bullet
this file used to carry (rather than a discrete feature), has also been acted on and
removed from here — see `development-plan.md` §6 ("Build performance" / "UI
responsiveness") for the concrete Gradle build/configuration-cache changes and the
Compose-jank audit that came out of it. Being a principle rather than a one-time
feature, it isn't "done" forever — re-visit it if Gradle iteration time creeps back
up or a new screen introduces an un-keyed list or a main-thread DB call.

## Other ideas worth considering later

- Multi-dog support (would require introducing a `Dog` entity and scoping all
  queries). See spec 13 below.
- A top-level "Gallery" screen: a 2-column grid of every bowel-movement photo taken
  so far, so browsing "what did that look like a few weeks ago" doesn't mean
  scrolling through History looking for a 📷 marker. See spec 14 below.

## Specs for the above (draft — for review)

Nothing here is scheduled or approved; review and cut/edit before it gets promoted
into `development-plan.md`.

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

### 14. Gallery (photo grid)

Covers *"On the main screen, next to History and Insights, a new 'Gallery' option
... 2 columns of photos, ordered by date descending ... under each photo the date,
consistency, amount, and whether it was night, walk, inside home or garden ...
tapping a card opens the photo in a small window, closeable by tapping outside."*

**Status: not started — spec only, do not build yet.**

- **Problem:** photo attachment (spec 8, shipped) has no way to browse photos as a
  set — the only way to see one today is to find its entry in History (spotting the
  📷 marker) and open it for editing. A dedicated grid makes "how did this look a
  few weeks ago, has it changed" a scroll, not a hunt.
- **Entry point:** a third top-bar link on Home, next to **History** and
  **Insights** (`HomeScreen`'s `TopAppBar` `actions`) — same `TextButton` pattern,
  so `onViewGallery` slots in alongside `onViewHistory`/`onViewInsights`. New
  `Routes.GALLERY` + `CrAppNavHost` entry, following the exact existing pattern for
  Insights.
  - **Worth a real look before shipping, not just assuming it fits:** the top bar
    would then carry three text labels ("History", "Insights", "Gallery") plus the
    Settings gear, alongside the "💩 CrApp" title on the same row. Two labels + the
    gear already fit comfortably (confirmed on-device for Insights); a third
    should too on most phones, but check on the actual test device before treating
    this as settled — if it's tight, shortening a label (e.g. "Gallery" is already
    the shortest of the three) or letting `TopAppBar` scroll/wrap is the fallback,
    not immediately reaching for an overflow menu.
- **Data:** only bowel movements with a non-null `photoUri` — most movements won't
  have one, so this should be its own query, not a client-side filter over
  `observeAll()`. New `BowelMovementDao.observeAllWithPhoto()`:
  ```kotlin
  @Query("SELECT * FROM bowel_movement WHERE photoUri IS NOT NULL ORDER BY timestamp DESC")
  fun observeAllWithPhoto(): Flow<List<BowelMovement>>
  ```
  Ordering matches the ask ("date descending") for free, same as every other
  reverse-chronological list in the app. No new table, no migration.
- **Screen:** `ui/gallery/GalleryScreen.kt` + `GalleryViewModel.kt` (thin — collects
  `observeAllWithPhoto()` into a list, no derived state beyond that).
  - `LazyVerticalGrid(columns = GridCells.Fixed(2))`, `contentPadding` +
    `Arrangement.spacedBy` matching the app's existing ~12–16dp card-spacing
    convention (e.g. `HomeScreen`'s dashboard cards). Not a library, `foundation`'s
    own grid API covers this.
  - **Empty state:** no photos yet is the common case on a fresh install and not an
    error — a plain message in the same voice as other empty states (e.g.
    History's "No entries yet"), something like *"No photos yet — attach one next
    time you log a bowel movement."* Not a blank grid with no explanation.
  - **Each card:** a `Card` (rounded corners, matching `DashboardCard`/`StatTile`'s
    `RoundedCornerShape` convention), photo on top, caption text below inside the
    same card — not text overlaid on the image, which would fight for legibility
    against a busy photo. Suggested caption, up to 3 short lines
    (exact wording matches what's already used in `HistoryScreen`'s bowel-movement
    subtitle, for one voice across the app rather than inventing new phrasing):
    1. Date **and time** (`MMM d, yyyy h:mm a`, same formatter `HistoryScreen`
       already has — worth extracting to `ui/common` so both screens share it
       rather than duplicating the pattern). The ask says "the date"; showing time
       too is a deliberate addition, not scope creep — this dog can have several
       logged movements a day, so date-only would make same-day cards look
       identical and the grid harder to scan.
    2. `"Consistency ${n}"` + amount if set (e.g. `"Consistency 5 · Medium
       amount"`), same phrasing as History.
    3. Location + night tag if either is set (e.g. `"Walk · Night"`,
       `"Inside home"`), same `displayName`s as History; omitted entirely if
       neither is set, same "don't show an empty line" rule already used
       throughout this app's cards.
    Notes and blood/mucus flags are deliberately left off the card (History already
    covers full detail) — a gallery card's job is "which day was this and roughly
    what happened", not a full record.
  - **Thumbnail decoding needs real downsampling, not reused as-is:**
    `BowelMovementPhotoStore.loadThumbnail()` (used today for the single 80dp
    thumbnail on the log/edit screen) has no `inSampleSize`/bounded-decode logic —
    it decodes the *full* camera-resolution bitmap every time, "thumbnail" is
    really just its name. That's fine for one at a time; a full grid decoding N
    full-resolution bitmaps at once is a real memory/jank risk once there are more
    than a handful of photos. Add a properly downsampled decode path (e.g.
    `BitmapFactory.Options.inSampleSize` computed from the target cell size, or
    `ContentResolver.loadThumbnail()` on API 29+) for the grid specifically, and
    generalize the existing `PhotoThumbnail` composable to take a `Modifier`
    instead of its current hardcoded `.size(80.dp)`, so the grid reuses the same
    loading component instead of a second one. Keep the *full-size* viewer (next
    bullet) on a full/near-full decode — only the grid needs downsampling.
  - **Tapping a card** opens the full-size photo, matching the ask: a `Dialog`
    (Compose's own, not a new screen/route) showing the photo at its real aspect
    ratio (`ContentScale.Fit`, unlike the grid's cropped-square cells), on a scrim.
    `Dialog`'s default `dismissOnClickOutside = true` already gives "tap outside to
    close" for free — no custom gesture handling needed. Two closing affordances,
    not just one: tap-outside (as asked) *and* a visible close (✕) button/icon in a
    corner of the dialog, since tap-outside-to-dismiss isn't self-evident to every
    user and a modal with no visible way out is a common real complaint. Repeating
    the same caption text below the enlarged photo is a small, cheap addition worth
    including — without it, the context ("which entry was this") is lost the moment
    the grid is covered.
  - **Not in scope for this spec:** tap-to-edit from a gallery card (the ask is
    view-only; the existing edit path via History still works for that), and
    date-range/type filter chips matching History's own (nothing here needs
    filtering by anything except "has a photo", which the query already handles).
    Revisit either only if using the shipped version shows a real need.
- **Open questions:**
  - Should a broken/inaccessible photo (the file was removed outside the app, or a
    restored backup landed on a different device — see spec 8's own "photo not
    found" note) show a placeholder in its grid cell rather than a permanently
    spinning loader? Recommend yes, reusing the same graceful-fallback language
    spec 8 already established for this exact situation, rather than a new pattern.
  - Multiple photos per movement isn't something this app supports today (one
    `photoUri` per `bowel_movement` row) and this spec doesn't change that — each
    grid cell is still one movement, one photo. Worth noting only so it's not
    assumed the grid needs to plan for a movement having several.
- **Testing requirements:**
  - Pure logic worth extracting and unit-testing on its own: the caption-building
    function (given a `BowelMovement`, produce the up-to-3-line caption text) —
    same reasoning as this codebase's other extracted-for-testability functions
    (e.g. `countBowelMovementsToday`), and it's exactly the kind of formatting logic
    that's easy to get subtly wrong (the "omit a line if nothing to show" rules)
    without a few cases from a fresh photo/no-photo split.
  - Real camera capture can't be automated via `adb` on the test device (same
    limitation already documented for the photo-attachment and label-scan
    features) — but the gallery grid/dialog logic itself doesn't need a real photo
    capture to test: seed a `BowelMovement` with a `photoUri` directly via the
    repository (bypassing the camera) in an instrumented BDD-style test, and
    verify: it appears in the grid with the right caption; a movement with no
    `photoUri` doesn't appear at all; tapping the card opens the full-size dialog;
    tapping outside the dialog closes it. A real, physical photo capture into the
    gallery still needs a manual on-device pass, same caveat as the rest of this
    app's photo-related features.
  - Standard `connectedAndroidTest` caveat applies if any instrumented test touches
    the real database — back up first, per project memory.
- **Documentation requirements:**
  - New `app-functionality.md` section once built, documenting: the entry point,
    the grid/card layout and exactly what a caption shows (and what it deliberately
    leaves out), the full-size viewer's two close affordances, the empty state, and
    its testing status.
  - Update `app-functionality.md`'s Testing status section once click-tested.
  - No `development-plan.md` change needed unless this gets promoted into a phase —
    a `backlog.md` spec update (status line here) is enough while it's still
    pending.
