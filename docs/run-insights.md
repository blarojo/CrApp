# Running the CrApp Insights skill

Step-by-step instructions to generate an insights report and view it in the app.
See [app-functionality.md §13](app-functionality.md#13-insights-ai-generated-report)
for the design this implements, and
[`.claude/skills/crapp-insights/SKILL.md`](../.claude/skills/crapp-insights/SKILL.md)
for the skill's own authoritative workflow (this doc is the practical "how do I
actually run it" companion to that spec).

This is a one-shot, offline process — nothing runs inside the app or reads the
phone directly. You export data from CrApp, hand the files to Claude Code, and
upload back whatever it produces.

> **On your phone, without a computer?** See
> [run-insights-mobile.md](run-insights-mobile.md) instead — same idea, but using
> the Claude phone app directly with a self-contained prompt instead of Claude Code.

## 1. Export the data from CrApp

1. Open CrApp on your phone.
2. Tap the gear icon → **Settings** → **Export CSV**.
3. Share the CSVs to somewhere your computer can get to — email them to
   yourself, save to Google Drive/Downloads, or use whatever share target gets
   files onto the machine you run Claude Code on. Five files are produced:
   `bowel_movements.csv`, `food_entries.csv`, `medication_entries.csv`,
   `energy_entries.csv`, `walk_entries.csv`.
4. On your computer, save all five into one folder (e.g.
   `C:\Users\blaro\Downloads\crapp-export\`). You don't need all five to run
   the skill — it'll work with whatever subset you have and note the gap — but
   the more of them present, the better the analysis.

## 2. Run the skill in Claude Code

1. Open a Claude Code session in (or pointed at) the CrApp repo, so it can read
   `.claude/skills/crapp-insights/SKILL.md`.
2. Ask Claude to run the `crapp-insights` skill, pointing it at the export
   folder from step 1. For example:

   > Run the crapp-insights skill on the CSVs in
   > C:\Users\blaro\Downloads\crapp-export\

   (Or use the skill directly if your Claude Code setup supports invoking it by
   name, e.g. `/crapp-insights C:\Users\blaro\Downloads\crapp-export\`.)
3. Claude parses the CSVs itself, then delegates the actual progression/
   correlation analysis to the strongest available model via a subagent — this
   step can take a little while for a real reasoning pass, that's expected.
4. When it finishes, Claude writes **two files**, named from today's date, e.g.:
   - `crapp_insights_2026-09-07.md` — a plain-language report you can read
     directly (or print/email) without opening the app.
   - `crapp_insights_2026-09-07.json` — the file the app uploads and renders.

   Ask Claude where it saved them if it doesn't say — by default, expect them
   either in the export folder itself or in the working directory Claude Code
   is running from.

## 3. Upload the report into CrApp

1. Get the `.json` file (not the `.md` one) onto your phone — the same way you
   got the CSVs off it, in reverse (email attachment, Drive, etc.). Save it
   somewhere the phone's file picker can see, e.g. Downloads.
2. Open CrApp → tap **Insights** (top of the Home screen, next to History —
   also reachable via the gear icon → Settings → Insights).
3. Tap **Upload Report**, and pick the `.json` file.
4. You should see:
   - A short summary paragraph, and (if the data had a real gap) a ⚠️
     data-completeness note right under it.
   - Up to two headed groups of findings: **🩺 Bowel movement progression** and
     **🍗 Mood, food & bowel correlations** (a heading is simply absent if the
     data didn't support any finding for it — that's expected, not a bug).
   - Trend charts below the findings (consistency over time, movements per
     week, etc.).

The report stays on the app after this — you don't need to re-upload it just
from closing and reopening CrApp.

## Doing this again later

There's no incremental state to manage — every run is a full pass over
whatever CSVs you hand it. To refresh the in-app insights after logging more
data: repeat step 1 with a fresh export, repeat step 2, then upload the new
`.json` in step 3. Uploading a new report replaces the previous one (CrApp
only keeps the most recent).
