# Changelog

## 0.1.0 (unreleased)

First public version.

- Saves Google Meet live captions to `Documents/MeetCaptions/<date_time>.txt` with speaker names and timestamps.
- Works in the Meet app and in Meet calls inside Gmail (Android 16).
- Merges caption updates: growing text, rewritten tail, scrolling, late speaker names, unfinished last word.
- Transcript list in the app with share and open; adjustable resource-id regexes for when Meet changes its layout.
- English and Russian interface, following the phone language.
- Debug mode that writes window dumps and events for reporting layout changes.
