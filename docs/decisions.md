# Decisions

**English** | [Русский](decisions.ru.md)

Goal: automatically save a transcript of every Google Meet call, with names and times,
on Ubuntu 24.04 and on a Samsung S24 Ultra.

Constraints that drive the choice:

- meetings are held **in Russian**;
- 95% of the time the user is **not the organizer**;
- there is a personal Google AI Pro account, but no Workspace.

## Options considered

| Option | Result | Why |
|---|---|---|
| Gemini "Take notes for me" / Transcripts in Meet | Rejected | Russian is not among the supported languages (en, fr, de, it, ja, ko, pt, es). Access depends on the **organizer's** plan, not the participant's. And it produces notes, not a verbatim transcript. |
| Desktop browser extension (TranscripTonic) | **Adopted for Ubuntu** | Reads captions straight from the page, language and role don't matter, and in Auto mode it saves every meeting with `personName` and `timestamp`. |
| Extension in Firefox for Android (meet-transcript from AMO) | Rejected | The add-on is marked as desktop Firefox only. Web Meet on a phone redirects to the app. |
| Bot participant (Vexa and similar) | Rejected | Everyone sees the bot, and the organizer has to let it in. Needs your own GPU server or someone else's cloud. |
| Record audio and run my own speech recognition | Rejected | More complex, needs consent to record audio, fights with the call over the microphone. Meet captions are already recognized text. |
| **Accessibility service** that reads captions from the screen | **Adopted for Android** | Works with the native app, doesn't depend on role or plan, records no audio. |

A search for existing solutions (repo-scout, October 2026) found no ready-made Android implementation.

## Decisions inside the app

- **No dependencies.** Plain `View`s, no AndroidX or Compose: a settings screen with three fields doesn't justify libraries.
- **Files go to `Documents/MeetCaptions`** via `java.io.File`. On Android 11+ an app can create
  and overwrite its own files in `Documents/` without permissions (tested on an Android 15 emulator and on the S24).
  The files are visible to a file manager and to Syncthing, unlike the app's folder under `Android/data`.
- **`isAccessibilityTool="true"`.** Starting with Android 16, views with `filterTouchesWhenObscured` (tapjacking protection)
  become `accessibilityDataSensitive` and are hidden from services that don't declare themselves a tool.
  The Meet call screen is protected exactly this way: without the flag the service sees neither events nor content.
- **Selectors are configured as regular expressions** in the app, no rebuild needed: Meet's element ids
  are undocumented and can change with an update.
- **The speaker name is taken from the text.** Real Meet writes `Name: text` into a single `TextView`.
  Lookup of a separate name element is kept in case the format changes.
- **Session = 5-minute pause in captions.** There is no reliable "call ended" signal. A long silence during a call
  will split it into two files; if that becomes a problem, the end of the call needs to be detected from the Meet UI.
- **Debug mode** writes an event log and window dumps, so selectors can be fixed from a single real call.
