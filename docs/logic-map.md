# Logic map

How a Meet caption on the phone screen becomes a speaker-labelled text file.
The map describes the code as it is today, step by step.
Each step says what it decides, where the flow branches, and which module does it.

Read `docs/architecture.md` and `docs/decisions.md` first for the terms and the reasons.
Known gaps and fixes in progress are in `docs/plans/2026-11-05-logic-map-fixes.md`.

Rule: if you change a step or a branch, update this file in the same commit.
`scripts/check-logic-map.sh` checks that every `Module:` line points to a real module.

Paths in `Module:` start at `app/src/main/java/dev/octrow/meetcaptions/`.

## The flow at a glance

```
AccessibilityEvent from Meet / Gmail
  │
  ├─ 1  Event filter ........... which events reach the service at all
  ├─ 2  Debug dump ............. is the raw event/tree log switched on
  ├─ 3  Meet windows ........... which windows count as Meet
  ├─ 4  Collect blocks ......... which nodes are caption text and speaker name
  ├─ 5  Parse block text ....... does the text carry the speaker inline
  ├─ 6  Change and session ..... is this a new snapshot, does it start a new file
  ├─ 7  Speaker turn ........... which turn gets the snapshot
  ├─ 8  Merge into turn text ... which words are new, which are a revision
  ├─ 9  Save ................... when and where the file is written
  └─ 10 Recording notice ....... what the user sees while it records
  │
Documents/MeetCaptions/<start>.txt
```

There is no network, LLM or account. The optional parts are debug mode (step 2), which writes extra files, and the notification permission (step 10).

## Where the flow splits by Meet UI variant

The split is by Meet UI variant. The split is decided in one place for the node ids (two regexes in `Prefs`) and in two places for the speaker name (name node and inline "Name: " in the text).

| Variant | Switched on by | Steps it changes | What changes |
|---|---|---|---|
| Name in its own node | node id matches name regex (default `speaker\|participant_name`) | 4, 7 | name node sets the speaker for the text nodes after it |
| Name inside the text | text matches `^[^:]{1,40}:\s+.+$` | 5, 7 | the inline name wins over the name node |
| No name at all | neither of the above | 7 | speaker is blank; a blank name continues the current turn, a first blank turn prints as `?` |
| Meet inside Gmail | package `com.google.android.gm` | 1, 3, 5 | same conference UI; the Gmail snapshots in `TranscriptTest` carry the name inline (step 5). Every Gmail event is read, call or not (#11) |
| Custom ids | user edits the regexes on the main screen | 4 | other ids are used from the next event on |

Example: "Dan P: так проверка" in a caption node with no name node gives speaker "Dan P", text "так проверка".

Module: `Prefs`, `Transcript`, `CaptionService`

## Entry points

| Entry | Trigger | Notes |
|---|---|---|
| Accessibility service | each event from the three packages of step 1 (Gmail too, in or out of a call) | the main flow; starts when the user enables it in system settings |
| Launcher activity | app icon, notification tap, service settings link | status, regex settings, debug switch, last 5 transcripts; asks for the notification permission once per launch on Android 13+ (feeds step 10); otherwise touches the flow only through `Prefs` |
| Content provider | share or open from the main screen | read-only access to session files |

Defaults: text regex `caption`, name regex `speaker|participant_name`, debug off.

Module: `CaptionService`, `MainActivity`, `TranscriptProvider`, `Prefs`

## Steps

### 1. Event filter

Decides: which events reach `onAccessibilityEvent`.
In: system events. Out: events from three packages.
Branches:
- package in `com.google.android.apps.tachyon`, `com.google.android.apps.meetings`, `com.google.android.gm` → delivered, all event types, events of one type at most once per 300 ms (`notificationTimeout`);
- any other package → never delivered.
Fails: nothing to fail; if the service is off, no events and no file.
Module: `CaptionService`

### 2. Debug dump

Decides: whether raw events and node trees are written.
Branches:
- `debugDump` on → a `DebugRecorder` is created on first use; every event is appended to `events_<stamp>.txt` (stops at 5 MB); every 5 s (only when an event arrives) the window list and Meet's node trees go to a new `dump_<stamp>.txt`, so a call leaves one file per 5 s with no cap (#10);
- off → the recorder is dropped, so the next switch-on starts a new events file.
Fails: write errors are logged only.
Runs on every delivered event, before step 3, so also while Meet has no window.
Side effect: the dump holds the visible text of the call, in the same folder as transcripts. Gmail is in the package set, so whenever Gmail is open its whole node tree (mail list, message text) is dumped too (#11); other apps' windows are listed by title only.
Module: `DebugRecorder`, `CaptionService`

### 3. Meet windows

Decides: which windows are read.
In: all windows (`flagRetrieveInteractiveWindows`). Out: roots whose package is in the Meet set, back window first.
Branches:
- no Meet root (call minimized, or a full-screen app covers it) → the event is ignored;
- picture-in-picture is not special-cased: the PiP window is a Meet root and is read; it records only if Meet keeps caption nodes there;
- one or more roots → all of them are read, so a menu above the call window does not hide the captions.
Module: `CaptionService`

### 4. Collect blocks

Decides: which nodes are caption text and which are the speaker name.
In: roots. Out: `List<Block>` in tree order (depth-first, back window first); the flow treats the last one as the live block.
Branches (per node with a view id and non-blank text):
- id matches the name regex → sets the current speaker;
- else id matches the text regex → one or more blocks with the current speaker;
- else ignored.
Both regexes are case-insensitive "contains" matches, read from `Prefs` on every event. The name check comes first.
Ids come from `flagReportViewIds`. Visibility is not checked: nodes off screen or not important for accessibility are read too.
The speaker is not reset between roots (#6).
Fails: a node without id or text is skipped; an empty list ends the event.
Module: `CaptionService`, `Prefs`

### 5. Parse block text

Decides: who speaks in one caption text.
In: speaker from step 4, node text. Out: blocks, one per non-blank line.
Branches:
- line matches `^([^:]{1,40}):\s+(.+)$` → speaker and text from the line, the name node is ignored;
- else → speaker from the name node (may be blank).
Module: `Transcript`

### 6. Change and session

Decides: whether the snapshot is new and whether it opens a new file.
Branches:
- snapshot empty or equal to the last recorded one → ignored;
- more than 5 minutes (`SESSION_GAP_MS`) since the last recorded snapshot, or the first one → the old session is saved, a new `Transcript` and a new file `<yyyy-MM-dd_HH-mm-ss>.txt` start;
- otherwise → same session.
The new file name uses the time of the first snapshot, not the call start. There is no call-end signal.
All state is in memory: a service restart (disabled and enabled, process killed) starts a new file even mid-call.
Module: `CaptionService`

### 7. Speaker turn

Decides: which turn the snapshot belongs to. Only the last block is the live one.
In: `List<Block>`. Out: changed turns.
Branches:
- live speaker blank and a turn exists → live speaker becomes the current turn's speaker;
- live speaker equals the current turn's → the live text goes to that turn;
- different speaker → the second-to-last block is first given to the old turn, if it has the old speaker; then a new turn starts with the time `now`;
- the new turn's text equals the old turn's last text → it is not copied (the name view updated before the text view).
No snapshot is ever matched to an older turn: a speaker who returns gets a new turn.
Fails: empty block list → nothing happens.
Module: `Transcript`

### 8. Merge into turn text

Decides: which words of a snapshot are new.
In: snapshot text of one turn. Out: the turn's block of words plus committed blocks.
Each snapshot is the whole visible text. The first 3 words (anchor) are looked up in the current block, case and punctuation ignored.
Branches:
- full anchor found (last occurrence wins) → the block keeps the words before it and takes the snapshot;
- anchor runs past the block's end: the block's last words equal the snapshot's first words, except that the last overlapping block word only has to be a prefix of the snapshot's word ("провер" then "проверка") → same as above. The overlap needs 2 words; 1 word is enough only when the block has one word and the snapshot has 2+. A one-word snapshot never takes this branch ("мы говорили о" then "отлично" is a new block);
- no match → the block is committed as a line and the snapshot starts a new block;
- empty snapshot → ignored.
Module: `CaptionMerger`

### 9. Save

Decides: when the file is written.
Branches:
- first change after a save → one write is scheduled in 3 s; later changes do not move it;
- `onInterrupt`, `onDestroy`, new session → immediate write. `onDestroy` does not cancel a pending write, so it still runs up to 3 s later (#9);
- no file or an empty transcript → nothing is written.
The file is rewritten whole each time; its folder is `Documents/MeetCaptions`, created on demand.
Fails: a write error is logged (`MeetCaptions` tag) and swallowed; the flow goes on and step 10 still runs.
Module: `CaptionService`, `Transcript`

### 10. Recording notice

Decides: whether the user sees that captions are being saved.
Branches:
- notifications allowed → a silent low-importance notice with the last line, private on the lock screen, removed after 60 s without a new save;
- notifications denied → no notice, no other signal.
Shown after each save, so it reflects the last snapshot, not necessarily the saved text. Removed in `onDestroy`, but a write still pending then shows it again for 60 s (#9).
Module: `RecordingNotice`

### Output

File `Documents/MeetCaptions/<start>.txt`: per turn a header `Name (HH:mm:ss)` (name `?` if blank), the merged text, an empty line.
Debug files `events_*.txt` and `dump_*.txt` share the folder; the provider and the main screen list only names matching the session pattern.
Module: `Transcript`, `TranscriptProvider`

## Side flows

### Main screen

Asks once per launch for the notification permission (Android 13+). Shows service state (re-checked in `onResume`), the enable button and the restricted-settings help while the service is off, the last 5 transcripts (open or share through the provider), the two regex fields and the debug switch.
Saving a regex checks that it compiles and is not blank; reset restores the defaults.
Feeds: steps 2 and 4 through `Prefs`.
Module: `MainActivity`, `Views`, `Prefs`

### Transcript provider

Serves only names that match the session-file pattern and exist in the folder; any other path gives `FileNotFoundException`. Read-only; insert, update and delete do nothing.
Module: `TranscriptProvider`

### Fake call

The `fakemeet` module is a test app that shows scripted captions in nodes `speaker_name` and `caption_text` (the default regexes match them).
Its applicationId is `com.google.android.apps.tachyon`, so it passes step 1; it cannot be installed next to real Meet. Not part of `app`.

## Known gaps

- A snapshot shorter than the anchor can cut the block at an earlier equal word (#1).
- One revised word among the first 3 splits the block and repeats text (#2).
- A caption with a colon in its first 40 characters is read as a speaker name (#3).
- Words of a block other than the last two are lost when the speaker changes between snapshots (#4).
- A failed write is not visible: the notice still says it records (#5).
- The notice comes back for 60 s after the service is turned off (#9).
- Debug mode writes a new dump file every 5 s with no cap (#10) and dumps Gmail's content when Gmail is open (#11).
