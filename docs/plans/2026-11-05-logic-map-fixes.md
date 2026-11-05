# Plan: logic map findings

Findings from writing `docs/logic-map.md` on 2026-11-05.
The map describes the code as it is; this file says what is wrong with it and what we propose. New findings are appended.
A fixed finding gets ✅ and the map is updated in the same commit.

Severity: HIGH: wrong result for the user. MEDIUM: worse result than it could be. LOW: noise, debt, docs.
"Question": not sure if bug or intent; the owner decides. "Not confirmed": needs a live run or a test.

## Findings

| # | Map step | What is wrong | We propose | Severity |
|---|---|---|---|---|
| 1 | 8 | `CaptionMerger.kt:19-24,40-42`: a snapshot shorter than 3 words has a shorter anchor; a full match anywhere in the block wins. Block "я думаю да это так", snapshot "да" → block becomes "я думаю да", "это так" is lost (checked by reading the code). The same cut happens when a new caption line of the same speaker starts with 1-3 words that occur earlier in the block; short snapshots make it far more likely. Not confirmed on real Meet (does Meet start a new block for the same speaker?). | Add a test. Require the anchor to start at the block's tail when the snapshot is shorter than `anchorWords`, or treat it as a new block. | MEDIUM |
| 2 | 8 | `CaptionMerger.kt:39` (`ponytail` note): ASR revising one of the first 3 words gives no match, so the whole snapshot is committed as a new block and the text appears twice. Question: acceptable until real logs show it? | Collect a debug dump of a long call; fuzzy-match one differing anchor word if it shows up. | MEDIUM |
| 3 | 5, 7 | `Transcript.kt:11,14-19`: any line with `:` followed by whitespace within 40 characters is a speaker ("Итак вот что: сделаем" gives speaker "Итак вот что") and overrides the name node. Not confirmed. | Apply the inline form only when the name node gave no speaker, or when it matches a known participant. | MEDIUM |
| 4 | 7 | `Transcript.kt:48-50`: on a speaker change only the second-to-last block is given to the old turn; with two changes between two snapshots (events are throttled to 300 ms) the middle turn and the tail of the old one are lost. Multi-line node text also makes only its last line "live" (`Transcript.kt:14`). Not confirmed. | Walk all blocks above the live one and route each to its turn. | MEDIUM |
| 5 | 9, 10 | `CaptionService.kt:75-81,68-73`: `write` swallows the exception, then `save` shows the "recording" notice anyway. A full disk or missing storage access looks like a working recording. `outDir()` ignores the `mkdirs` result (`CaptionService.kt:98-101`). | Return success from `write`; show a different notice or none on failure. | MEDIUM |
| 6 | 4, 3 | `CaptionService.kt:33,111-120`: one `BlockCollector` serves all Meet roots and returns its own mutable list, so with 2+ roots the first root's blocks are added twice and the speaker leaks into the next root. The last two blocks stay right in the common case (menu above the call window). | Create a collector per root. | LOW |
| 7 | 7 | `Transcript.kt:52-53`: a new turn whose text equals the old turn's last text is created empty; if no more text comes, the file has a header with no text. | Drop turns that stay empty on save. | LOW |
| 8 | 4 | `Prefs.kt:23-24` (`ponytail`): default text id `caption` is a guess; it matches any node id containing "caption" that has text. Question: confirmed on the real Meet UI? | Confirm from a debug dump and write the real id in `docs/architecture.md`. | LOW |
| 9 | 9, 10 | `CaptionService.kt:47,52-56`: `onDestroy` saves and cancels the notice but leaves `saveRunnable` on the main handler. If a write was pending, it runs up to 3 s after the service is off, writes again and calls `notice.show`, so the "recording" notice comes back for 60 s while nothing records. Found in review. | `handler.removeCallbacks(saveRunnable)` before the final save in `onDestroy`. | LOW |
| 10 | 2 | `DebugRecorder.kt:24-26`: each 5 s dump goes to a new `dump_<stamp>.txt`; nothing caps the count or total size (the 5 MB cap is only on one events file, and each switch-on starts a new one). An hour-long call leaves ~720 full-tree files in `Documents/MeetCaptions`. Found in review. | One dump file per recorder with the same size cap, or keep the last N dumps. | LOW |
| 11 | 1, 2, 3 | `CaptionService.kt:87-91`, `DebugRecorder.kt:36-42`: Gmail is in the Meet package set, so outside calls every Gmail event walks Gmail's whole tree, and in debug mode its full tree (mail list, message text) is dumped, against the "other apps by title only" intent of `dumpWindows`. Question: accept for debug mode? Found in review. | In debug dumps, dump a Gmail tree only when it holds a node matching the caption or name regex. | LOW |

## Order of work

1. #1, #2, #3, #4 together: they share `CaptionMerger` and `Transcript` tests.
2. #5 alone: `CaptionService` and `RecordingNotice`.
3. #6, #7, #8 as cleanup.
4. #9 (one line in `onDestroy`), #10, #11 with debug mode.

## Done when

- [ ] every finding has ✅ or a "keep as is" decision with a reason;
- [ ] Known gaps in the map lists every open HIGH and MEDIUM finding.
