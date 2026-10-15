# Architecture

**English** | [Русский](architecture.ru.md)

Package `dev.octrow.meetcaptions`, module `app/`. The Meet test double lives in module `fakemeet/`.

```
accessibility event ──► CaptionService ──► BlockCollector ──► Block.parse ──► Transcript ──► file
(Meet / Gmail)           Meet windows       tree walk          "Name: text"     turns         every 3 s
                              │                                                  │
                              └──► DebugRecorder (debug mode only)               └── CaptionMerger per turn
```

## Components

| File | What it does |
|---|---|
| `CaptionService.kt` | The service. On every event from Meet it takes all Meet windows, builds a caption snapshot, hands it to `Transcript` and schedules a save. Manages sessions (files). |
| `BlockCollector` (in `CaptionService.kt`) | Walks the window tree in on-screen order. A node whose id matches the name regex sets the current speaker; a node whose id matches the text regex yields one or more blocks. |
| `Transcript.kt` → `Block.parse` | Parses caption text line by line; a `Name: ` prefix overrides the name taken from a separate node. |
| `Transcript.kt` → `Transcript` | List of turns (name, start time, merger). Decides whether a turn continues or a new one starts. |
| `CaptionMerger.kt` | Merges consecutive snapshots of one turn without duplicates. |
| `DebugRecorder.kt` | Debug mode: `events_*.txt` and `dump_*.txt`. |
| `Prefs.kt` | Settings: text regex, name regex, debug. |
| `MainActivity.kt` | Main screen: service status and enable button, restricted-settings help, transcript list (open, share), the two regexes, debug switch. |
| `res/xml/accessibility_service.xml` | Service config: all event types, Meet and Gmail packages, `isAccessibilityTool`, `notificationTimeout=300`. |

## How Meet shows captions (S24, Android 16, Meet 379)

- A call started from the Meet tab in Gmail runs in package `com.google.android.gm`, activity
  `com.google.android.libraries.communications.conference.ui.callui.SingleCallActivity`.
  From the Meet app it is `com.google.android.apps.tachyon`. The call UI is the same.
- Container `…:id/captions_manager_placeholder` → `captions_manager_fragment_container` → `TextView` `…:id/captions_text`.
- `captions_text` holds one current turn: `Dan P: так проверка с другого девайс 40 41`.
  When the speaker changes, the whole text is replaced with `Вы: …` ("You: …").
- Updates arrive as `TYPE_WINDOW_CONTENT_CHANGED` with source `captions_text`, roughly every 0.2–1 s.
- The last word can show up unfinished ("провер" → "проверка"), and earlier words can change case
  and punctuation ("привет как" → "Привет, как").

Defaults: text regex `caption`, name regex `speaker|participant_name`.

## Merging captions

**`CaptionMerger`** keeps the current block of words. For each new snapshot it takes the first 3 words (the anchor) and looks for them in the block:

1. Comparison ignores case and punctuation.
2. It first looks for a full match of the anchor (the last one in the block). Everything before it has already scrolled up and is kept;
   the rest is replaced by the snapshot. This handles both appended and corrected words.
3. If there is no full match, the anchor may run past the end of the block (new words): at least
   2 words must match (or all of them if the block is shorter), and the last word of the block may be the beginning of a word in the snapshot.
4. No match: the block is closed as a line and the snapshot starts a new one.

**`Transcript`** works with the last block of the snapshot (the live turn):

- same name: the snapshot goes to the current turn's merger;
- empty name: treated as the current speaker (the name may have disappeared for a moment);
- new name: a new turn with a timestamp; the second-to-last block of the snapshot is appended to the old turn;
- if the text under the new name matches the last text of the previous turn (the name updated before the text),
  that text is not copied.

## Saving

- Session file: `Documents/MeetCaptions/yyyy-MM-dd_HH-mm-ss.txt`, named after the time of the first turn.
- Saves happen at most once every 3 seconds, even during continuous speech (new events do not reset the timer).
- A new session starts after 5 minutes without captions.
- The file is rewritten in full each time, so its size is the text of one meeting.
- The service also saves in `onInterrupt` and `onDestroy`.

## File format

```
Имя (ЧЧ:ММ:СС)
текст реплики
(строки, если склейка не нашла продолжения)

Другое имя (ЧЧ:ММ:СС)
…
```

That is: speaker name with the turn's start time (`HH:MM:SS`), then the text. Extra lines appear when merging found no continuation. The next speaker follows after a blank line.

`?` in place of a name means no name was found, neither in the text nor in a separate node.
