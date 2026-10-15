# Troubleshooting

**English** | [Русский](troubleshooting.ru.md)

## Won't install

**Google Play Protect blocks installing from a file.** The app is not from the store and is signed with a debug key.
Options: install with `adb install` (no need to touch Play Protect), or temporarily turn Play Protect off
in Play Store → profile → Play Protect. You can turn it back on after installing; if it offers
to uninstall the app, choose to keep it.

**Samsung Auto Blocker.** Settings → Security and privacy → Auto Blocker blocks
apps that don't come from a store. Turn it off while installing.

## Service won't turn on

You see "App was denied access" / "Restricted setting": Android 13+ doesn't let an app installed from a file
use accessibility right away.

1. Try to turn the service on once (the option below won't appear otherwise).
2. Settings → Apps → **Meet Captions** → ⋮ in the top right → **Allow restricted settings**.
3. Go back to Accessibility → Installed apps → Meet Captions → on.

If the ⋮ menu doesn't have that option, reinstall via `adb`: such installs are not marked as restricted.

```bash
adb uninstall dev.octrow.meetcaptions
adb install app-debug.apk
```

## No file after a call

Check in this order:

1. The service is on: Accessibility → Meet Captions → on. It stays on after `adb install -r`.
2. Battery → **Unrestricted** for Meet Captions.
3. Captions were on in the call and someone spoke. No speech, no captions.
4. The call screen was open. A minimized call (Meet tab in Gmail, picture-in-picture) has no captions on screen.
5. The call runs in a supported package: `com.google.android.gm` (Gmail) or `com.google.android.apps.tachyon` (Meet).
   To see the call's current package:
   ```bash
   adb shell dumpsys activity activities | grep -m1 topResumedActivity
   ```
   If it's another package, add it to `MEET_PACKAGES` (`CaptionService.kt`) and `android:packageNames`
   (`res/xml/accessibility_service.xml`).

If all of that checks out and there are no files even in debug mode (no `events_*.txt`), the service isn't receiving events:
see `isAccessibilityTool` in [decisions.md](decisions.md).

## `?` instead of names, or text stuck to the name

Meet changed the caption format. Turn on debug mode and go through the dumps (below).

## Meet updated and recording stopped

1. Open Meet Captions and turn on **"Записывать события и дампы окон"** (record events and window dumps) in the «Отладка» (Debug) section.
2. Make a call with captions on and a minute or two of speech from two devices. Turn debug mode off.
3. Pull the files:
   ```bash
   adb pull /sdcard/Documents/MeetCaptions/
   ```
   - `events_*.txt`: every Meet event with time, type, class, source id and text.
   - `dump_*.txt`, every 5 s: all windows on screen (only the title for other apps) and the full tree of the Meet windows.
4. Find the line with the caption text:
   ```bash
   grep -h 'text=.*[а-я]' dump_*.txt | grep -v 'text=null' | sort -u
   grep -h src= events_*.txt | grep -o 'src=[^ ]*' | sort | uniq -c | sort -rn
   ```
   The first command looks for Cyrillic text in the dumps; for captions in another language, change the `[а-я]` pattern.
   The source that updates most often while someone talks is usually the captions.
5. Put part of its id into the «Regex id текста субтитров» (caption text id regex) field and tap «Сохранить» (Save). If the name is in a separate node,
   put that node's id into «Regex id имени говорящего» (speaker name id regex).
6. If the caption node has `id=null` (Compose without a resource id), a regex won't help: the code needs a different selector.

Delete the dumps when you're done: they contain the account's email and the participants' names.

## Call split into two files

There were no captions for more than 5 minutes (silence, screen sharing). The constant is `SESSION_GAP_MS` in `CaptionService.kt`.

## Duplicated lines in the file

Merging found no continuation: Meet corrected one of the first three words of the turn. If this happens often,
send a fragment of the file and the snapshots from `dump_*.txt`; they become a test in `CaptionMergerTest`.
