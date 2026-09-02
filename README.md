# CrApp

A personal Android app for tracking a sick dog's bowel movements, food, and
medications — so patterns (frequency, consistency, triggers) can be spotted and
shared with a vet. Single-user, single-dog, offline-first: all data stays on-device,
no backend, no account system.

## Status

Phase 2 complete: the Room data layer (Phase 1) and the three logging screens —
bowel movement, food (dropdown + "Add new"), and medication (Phase 2) — are built,
wired to a Navigation Compose graph, and verified end-to-end on a physical device.
No history/edit view or CSV export yet — see
[docs/development-plan.md](docs/development-plan.md) §7 for the phased build order.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), MVVM
- Room (SQLite) for local persistence
- minSdk 26, compileSdk/targetSdk 35
- No network access — the app doesn't request the `INTERNET` permission

## Getting started

1. Install Android Studio and the Android SDK — see
   [docs/development-plan.md §6](docs/development-plan.md#6-development-environment-setup).
2. Open this folder (`C:\Dev\CrApp`) in Android Studio and let Gradle sync.
3. Connect your phone via USB with USB debugging enabled, then hit **Run ▶**.

Full instructions for building, signing, and sideloading a release build onto your
phone are in [docs/development-plan.md §9](docs/development-plan.md#9-building-packaging-and-installing-on-your-phone).

## Docs

- [docs/development-plan.md](docs/development-plan.md) — full plan: feature set,
  data model, architecture, build phases, CSV export design, packaging/install steps.
- [docs/future-features.md](docs/future-features.md) — backlog of ideas beyond the
  current plan.
