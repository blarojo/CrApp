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
manual "Add new food" flow and alphabetical ordering that came with it)**,
**spec 15 (home screen widget)**, and **the Gallery photo grid** (also numbered
spec 14 in this file — the number got reused once the original spec 14 shipped and
was removed; see app-functionality.md §15 for the feature itself), have all shipped
and been documented as real features — see
[app-functionality.md](app-functionality.md) for what each one does today and its
current testing status, and [development-plan.md](development-plan.md) for when
each was built. They've been removed from this file since they're no longer
"future" — this file now holds only what's still pending. (Spec 15 has since been
click-tested live on-device too — placement, summary numbers, both quick-add
buttons, and the reactive refresh path all confirmed working; only the hourly
midnight-rollover fallback remains unobserved live — see app-functionality.md
§14's Testing status entry. The Gallery spec was also click-tested live, and that
pass caught and fixed a real bug in its thumbnail-downsampling logic — see
app-functionality.md §15's Testing status entry for detail.)

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

