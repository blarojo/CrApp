# Installing CrApp on Your Phone via Android Studio

CrApp is a personal, sideloaded app — no Play Store involved. This is the day-to-day
way to get a build onto your phone: connect it to Android Studio and hit Run. See
[development-plan.md §9](development-plan.md#9-building-packaging-and-installing-on-your-phone)
for the full picture (including a command-line/`adb` alternative and cutting a signed
release build); this doc is just the Android Studio walkthrough, step by step.

## 1. One-time phone setup

1. **Settings → About phone**, then tap **Build number** 7 times. You'll see "You are
   now a developer."
2. **Settings → System → Developer options**, turn on **USB debugging**.

(If you don't see "Developer options" under System, it only appears after step 1.)

## 2. Connect your phone to your computer

Plug the phone into your computer with a USB cable that supports data transfer (some
cables are charge-only).

A prompt appears on the phone: **"Allow USB debugging?"** — check "Always allow from
this computer" and tap **Allow**. If you miss it, unplug and replug the cable to
trigger it again.

### No cable? Use wireless debugging instead
1. **Settings → System → Developer options → Wireless debugging** → turn it on.
2. Tap **Pair device with QR code** (or pairing code).
3. In Android Studio: **File → Settings → ... → Pair Devices Using Wi-Fi** (or the
   device dropdown's "Pair Devices Using Wi-Fi" option), scan the QR code / enter the
   pairing code.
4. Once paired, the phone shows up in the device dropdown the same as a USB
   connection, and you can leave the cable unplugged from then on (both devices need
   to stay on the same Wi-Fi network).

## 3. Open the project and run it

1. Open the `CrApp` folder in Android Studio (**File → Open**, point it at the repo
   root — the same folder as `settings.gradle.kts`).
2. Wait for the Gradle sync to finish (progress bar at the bottom). First sync after a
   fresh checkout can take a few minutes.
3. At the top toolbar, the **device dropdown** (next to the Run button) should now
   show your phone's model name. Select it.
   - If it's not listed: check the phone screen for an unanswered "Allow USB
     debugging?" prompt, or try a different USB cable/port.
4. Click the green **Run ▶** button (or **Shift+F10**).

Android Studio builds a debug APK, installs it on the phone, and launches it
automatically. Subsequent runs are much faster — only changed code gets rebuilt.

## 4. Iterating

- Change code, hit **Run ▶** again — it reinstalls over the existing app, keeping its
  data (your logged entries aren't wiped between installs).
- **Apply Changes** (the icon next to Run, or **Ctrl+Alt+F10**) can push some code
  changes to the already-running app without a full reinstall, for a faster loop —
  works for most Compose UI/logic tweaks, but a full Run is more reliable for
  structural changes (new files, dependency changes, database schema changes).

## Troubleshooting

| Problem | Try this |
|---|---|
| Phone doesn't appear in the device dropdown | Re-check USB debugging is on; try a different cable (must support data, not just charging); look for the "Allow USB debugging?" prompt on the phone |
| "INSTALL_FAILED_..." error | Uninstall the existing CrApp from the phone first, then Run again — this can happen if a build was previously installed with a different signing key |
| Gradle sync fails right after opening | Let it finish once with a working internet connection (it downloads dependencies the first time); afterwards it works offline |
| Run succeeds but the app doesn't open on the phone | Check the phone isn't showing a permission dialog underneath another screen; try opening it manually from the app drawer |
