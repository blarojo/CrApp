# CrApp — Future Features Backlog

Ideas beyond the MVP described in [development-plan.md](development-plan.md). Nothing
here is scheduled — add to this list freely as things come up; prune or promote items
into active development as needed.

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

## Specs for the above (draft — for review)

Nothing here is scheduled or approved; review and cut/edit before either of these
gets promoted into `development-plan.md`.

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
