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

## 5. Using it day to day (you don't need to stay connected)

The app that lands on your phone in step 3 is already a real, standalone app — not a
preview or a "debug mode" that only works while tethered. Once **Run ▶** finishes:

- It's installed under its own **CrApp** app icon in your app drawer/home screen, like
  any other app.
- **Unplug the cable (or leave wireless debugging) and use it normally** — logging
  entries, viewing History, exporting — none of that needs Android Studio, a
  connection, or your computer at all. Android Studio is only involved when you want
  to install a *new* build.
- Your logged data stays on the phone between app opens, restarts, and even the next
  time you reinstall a new build over it via Run ▶ — reinstalling doesn't clear it.

This "debug" build is genuinely fine to keep using for as long as you like — it's the
same app, just built with debug settings (which mainly affects build speed and signing
key, not functionality). Come back to Android Studio and hit **Run ▶** again only when
you have a new version of the code to install.

### Optional: a signed "release" build instead

If you'd rather have a smaller, slightly faster build with no debug overhead (not
required — purely optional polish):

1. **Build → Generate Signed App Bundle / APK…** in Android Studio, choose **APK**.
2. First time: click **Create new...** to generate a signing keystore (a file that
   proves future updates are really you — save it somewhere safe outside this repo;
   losing it means a future release build can't update this one without uninstalling
   first). Fill in the password fields and a validity of e.g. 25+ years.
3. Choose the **release** build variant, finish the wizard.
4. Android Studio shows a **"locate"** link when it's done — the APK is under
   `app/release/`. Either tap **locate**, copy it to your phone, and open it there
   (allow "install unknown apps" for whichever app you use to open it), or install it
   over USB the same way as §9.3 in development-plan.md
   (`adb install -r app/release/app-release.apk`).
5. Installing this over the debug build first requires uninstalling the debug one —
   they use different signing keys, so `INSTALL_FAILED_UPDATE_INCOMPATIBLE` is
   expected if you skip that.

See development-plan.md §9.4 for more detail on this path.

## Troubleshooting

| Problem | Try this |
|---|---|
| Phone doesn't appear in the device dropdown | Re-check USB debugging is on; try a different cable (must support data, not just charging); look for the "Allow USB debugging?" prompt on the phone |
| "INSTALL_FAILED_..." error | Uninstall the existing CrApp from the phone first, then Run again — this can happen if a build was previously installed with a different signing key |
| Gradle sync fails right after opening | Let it finish once with a working internet connection (it downloads dependencies the first time); afterwards it works offline |
| Run succeeds but the app doesn't open on the phone | Check the phone isn't showing a permission dialog underneath another screen; try opening it manually from the app drawer |
