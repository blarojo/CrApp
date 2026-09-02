# CrApp — Development Plan

## 1. Purpose

CrApp is a personal Android app for tracking a sick dog's bowel movements, food, and
medications, so patterns (frequency, consistency, triggers) can be spotted and shared
with a vet. It is a single-user, single-dog, offline-first app — no backend, no account
system, no internet dependency.

Core needs:
- Log a bowel movement with timestamp, consistency, and notes.
- Log food eaten (what, when, how much).
- Log medications given (what, dose, when).
- Browse history over time.
- Export everything to CSV for sharing with a vet or analyzing later.

## 2. MVP Feature Set

| Feature | Description |
|---|---|
| Log bowel movement | Timestamp (defaults to now, editable), consistency score, color, presence of blood/mucus (yes/no), free-text notes |
| Log food | Timestamp, food picked from a dropdown of previously-used foods (or "Add new"), amount, meal or treat |
| Log medication | Timestamp, medication name, dose + unit, notes |
| History view | Chronological list of all entries, filterable by type and date range |
| Edit / delete entry | All logged entries must be correctable — mistakes happen mid-crisis |
| CSV export | Export all data (or a date range) to a CSV file, shareable via Android's share sheet |
| Local storage only | All data stays on-device in a local database; no network calls |

Explicitly **out of scope for MVP** (tracked in [future-features.md](future-features.md)):
multi-dog support, photos, reminders/notifications, analytics/dashboards, ingredient
insights, cloud sync.

### Consistency scale

Use the **Purina Fecal Scoring Chart**, the standard veterinary 7-point scale for dogs
(1 = very hard/dry, 7 = liquid, no texture). This is more clinically useful to a vet than
the human Bristol scale and it's a single tap on a 1–7 selector — fast to log, which
matters when you're doing this multiple times a day under stress.

## 3. Data Model

Single dog, so no `Dog` entity in the MVP. Bowel movements and medications are plain
log tables; food gets a small catalog table (`food`) so entries can be picked from a
dropdown instead of retyped every time. Each row is independently timestamped and
editable.

**`bowel_movement`**

| Field | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| timestamp | Instant / epoch millis | when it happened |
| consistency | Int (1–7) | Purina fecal score |
| color | String? | optional, free text or small preset list |
| hasBlood | Boolean | quick flag, clinically relevant |
| hasMucus | Boolean | quick flag |
| notes | String? | free text |

**`food`** — catalog of known foods, built up as you go

| Field | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| name | String | e.g. "Hill's I/D", "boiled chicken"; unique |
| brand | String? | optional |

**`food_entry`**

| Field | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| timestamp | Instant | |
| foodId | Long (FK → food.id) | selected via dropdown |
| amount | String? | free text, e.g. "1/2 cup" — avoid forcing units |
| mealType | Enum | MEAL / TREAT |

Food dropdown UX: the food-logging screen shows a searchable dropdown of existing
`food` rows (sorted by most-recently-used, so the common cases are one tap), plus an
"Add new" option at the bottom. Picking "Add new" opens a small inline field for name
(+ optional brand) — on save, it inserts into `food` and immediately selects it for
the entry being logged, so adding a never-before-seen food doesn't interrupt the log
flow. This also gives the future ingredient-insights feature (see
[future-features.md](future-features.md)) a natural place to hang ingredient data
later, without needing a schema change.

**`medication_entry`**

| Field | Type | Notes |
|---|---|---|
| id | Long (PK, autogenerate) | |
| timestamp | Instant | |
| name | String | e.g. "Metronidazole" |
| dose | String? | free text, e.g. "250mg" |
| notes | String? | |

Keep all "amount"/"dose" fields as free text rather than structured numeric+unit pairs
for the MVP — real-world dosing instructions and food portions are inconsistently
described, and forcing structure here adds UI friction for no immediate benefit. This
can be revisited later if the food-ingredient-insights feature needs structured data.

## 4. Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3) — fastest to build a snappy, small-surface-area
  UI with, and it's the current standard, so tooling/support is best.
- **Architecture:** MVVM — `ViewModel` per screen, `StateFlow` for UI state.
- **Persistence:** Room (SQLite) — local, zero-config, well-documented, built-in
  migration support if the schema evolves.
- **Async:** Kotlin Coroutines + Flow (Room emits `Flow` natively).
- **Navigation:** Navigation Compose, single-Activity app.
- **CSV export:** hand-rolled writer (no library needed for this small a schema) +
  Storage Access Framework (`ACTION_CREATE_DOCUMENT`) or `Intent.ACTION_SEND` to hand
  the file off to email/Drive/Files — avoids needing broad storage permissions.
- **Min/target SDK:** minSdk 26 (Android 8.0, covers virtually all real devices),
  targetSdk = latest stable at time of build.
- **No backend, no third-party analytics, no ads.**

## 5. Project Structure

```
CrApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/crapp/
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/            # Room database, DAOs
│   │   │   │   │   ├── model/         # Entity classes
│   │   │   │   │   └── repository/    # Repository layer wrapping DAOs
│   │   │   │   ├── ui/
│   │   │   │   │   ├── log/           # Entry-logging screens (bowel/food/med)
│   │   │   │   │   ├── history/       # History/list + filters
│   │   │   │   │   ├── export/        # CSV export screen/flow
│   │   │   │   │   └── common/        # Shared composables, theme
│   │   │   │   ├── export/            # CsvWriter, file-sharing intent helpers
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/
│   │   ├── test/                      # Unit tests (DAO, CsvWriter)
│   │   └── androidTest/                # Instrumented/Compose UI tests
│   └── build.gradle.kts
├── docs/
│   ├── development-plan.md
│   └── future-features.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

## 6. Development Environment Setup

1. Install **Android Studio** (latest stable, "Ladybug" or newer) — bundles the JDK,
   SDK manager, and emulator.
2. On first launch, use the SDK Manager to install:
   - Android SDK Platform (targetSdk version)
   - Android SDK Build-Tools
   - Android SDK Platform-Tools (includes `adb`)
3. Create the project: **New Project → Empty Activity (Compose)**, package name
   `com.crapp`, minSdk 26, language Kotlin.
4. Initialize git in the project root (`git init`) and commit the generated skeleton
   before making changes, so there's a clean baseline.
5. Add a `.gitignore` (Android Studio generates a good default — excludes
   `local.properties`, `build/`, `.gradle/`, `*.apk`, `*.aab`, keystores).

No emulator is strictly required — since this app only matters on your actual phone,
prefer testing on the physical device from day one (see §9).

## 7. Development Phases

**Phase 0 — Scaffolding** ✅ Complete

Empty Compose project builds and runs on-device. Git repo initialized.

**Phase 1 — Data layer** ✅ Complete

Room entities (`BowelMovement`, `Food`, `FoodEntry`, `MedicationEntry`), DAOs with
insert/update/delete/query-all-as-Flow, `AppDatabase` singleton, repository layer.
Unit-test DAOs with an in-memory Room database.

**Phase 2 — Logging screens** ✅ Complete

Three entry forms (bowel movement, food, medication), each backed by a `ViewModel`.
Fast-entry UX matters most here: sensible defaults (timestamp = now), minimal required
fields, big touch targets, one thumb usable. Food entry uses the dropdown + "Add new"
flow described in §3, backed by the `food` catalog table.

**Phase 3 — History view** ✅ Complete

Combined, reverse-chronological list of all entry types with icons/color-coding by
type, filter by type and date range, tap to edit, swipe or long-press to delete.

**Phase 4 — CSV export** ⬜ Not started

`CsvWriter` that serializes all three tables (or a date-filtered subset) into a single
CSV (or one file per table — decide once the shape of the data is clearer). Wire to
`ACTION_CREATE_DOCUMENT` so the user picks where to save, and/or `ACTION_SEND` to push
straight to email/Drive/Messages.

**Phase 5 — Polish** ⬜ Not started

Empty states, basic input validation, app icon, dark mode support (Compose Material 3
gives this close to free), simple home screen with today's summary (movement count,
last logged time).

**Phase 6 — On-device install & real-world testing** ⬜ Not started

Sideload to phone (see §9), use it for real for a few days, fix friction points found
through actual use — this is more valuable than synthetic testing given the app's
purpose.

Phases are sequential but each is small; expect Phases 0–4 to be a working, useful app.
Phase 5 onward is refinement.

## 8. CSV Export Design

- Trigger: a button on the History screen ("Export") and/or a dedicated Export screen
  with a date-range picker (defaults to "all time").
- Format: standard comma-separated with a header row per entity type. Simplest option:
  three separate files (`bowel_movements.csv`, `food_entries.csv`,
  `medication_entries.csv`) zipped or shared together — avoids awkward column
  alignment from mixing row shapes in one file. Revisit if a single unified timeline
  file turns out to be more useful in practice.
- Escaping: quote fields containing commas/newlines/quotes per RFC 4180; free-text
  `notes` fields need this.
- Delivery: write to app-scoped cache, then hand off via `FileProvider` +
  `Intent.ACTION_SEND` (share sheet) — works for email, Drive, Messages, etc. without
  requesting `WRITE_EXTERNAL_STORAGE`. Optionally also support
  `ACTION_CREATE_DOCUMENT` for a direct "Save to Downloads" flow.

## 9. Building, Packaging, and Installing on Your Phone

### 9.1 Enable developer mode on the phone (one-time)
1. Settings → About phone → tap **Build number** 7 times → "You are now a developer."
2. Settings → System → Developer options → enable **USB debugging**.

### 9.2 Fastest loop during development: run directly from Android Studio
1. Connect phone via USB, accept the "Allow USB debugging?" prompt on the phone.
2. Phone appears in Android Studio's device dropdown.
3. Click **Run ▶** — builds a debug APK, installs it, launches it. This is the
   day-to-day workflow; no manual packaging needed.
4. Alternative to USB: **wireless debugging** (Developer options → Wireless debugging →
   pair with QR/code) — same workflow, no cable.

### 9.3 Manual debug install via command line (optional)
```
adb devices                 # confirm phone is detected
./gradlew assembleDebug     # builds app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 9.4 Producing a "real" release build for long-term personal use

A debug build is fine indefinitely for a personal-use app, but a signed release build
is smaller, faster, and won't nag about debug status. To produce one:

1. **Generate a signing keystore** (one-time, keep this file and its passwords safe —
   losing it means you can't update the app later without uninstalling first):
   ```
   keytool -genkeypair -v -keystore crapp-release.jks -alias crapp \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. In Android Studio: **Build → Generate Signed App Bundle / APK → APK**, point it at
   the keystore, build a `release` variant.
3. Install it the same way: `adb install -r app-release.apk`, or copy the APK to the
   phone and open it (requires allowing "install unknown apps" for whichever app you
   used to transfer it — Files, Drive, etc.).
4. Store `crapp-release.jks` outside the git repo (or encrypted) — never commit a
   keystore or its passwords.

### 9.5 Not needed for this project
Google Play Console / Play Store distribution is unnecessary overhead for a
single-user personal app — sideloading (§9.2–9.4) is simpler and gives full control.
This can be revisited if you ever want to share the app with others.

## 10. Testing Strategy

Given the app's purpose (tracking a sick dog in real time), prioritize it actually
working correctly over test coverage for its own sake:

- **Unit tests:** Room DAOs (in-memory DB), CSV writer (escaping, correctness against
  known input/output pairs).
- **Compose UI tests:** logging a bowel movement end-to-end, editing/deleting an
  entry — the flows you'll run most often.
- **Manual, on-device testing:** the most important test is using the real app on the
  real phone during Phase 6, since that's the actual usage pattern.

## 11. Suggested Build Order (condensed)

1. Scaffold project, confirm it runs on your phone.
2. Room entities + DAOs + repository, unit tested.
3. Bowel movement logging screen — get the single most important flow working
   end-to-end first, install it, use it for real.
4. Food and medication logging screens.
5. History/list view with edit and delete.
6. CSV export.
7. Polish, then cut a signed release build for daily use.

See [future-features.md](future-features.md) for the backlog beyond this plan.
