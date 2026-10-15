package dev.octrow.meetcaptions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One caption block on screen: who speaks and what Meet currently shows for them. */
data class Block(val speaker: String, val text: String)

/**
 * Speaker turns built from caption snapshots. Meet appends the live block at the bottom,
 * blocks above it are finished turns, so only the last block (and, on a speaker change,
 * the one before it) can carry new words.
 */
class Transcript(private val timeFormat: (Long) -> String = ::clock) {
    private class Turn(val speaker: String, val startedAt: Long) {
        val merger = CaptionMerger()
        var lastText = ""

        fun add(text: String) {
            merger.add(text)
            lastText = text
        }
    }

    private val turns = mutableListOf<Turn>()

    fun add(blocks: List<Block>, now: Long) {
        val current = turns.lastOrNull()
        // A name node can vanish for one snapshot; that must not split the speaker's turn.
        val live = blocks.lastOrNull()
            ?.let { if (it.speaker.isBlank() && current != null) it.copy(speaker = current.speaker) else it }
            ?: return
        if (current?.speaker == live.speaker) {
            current.add(live.text)
            return
        }
        blocks.getOrNull(blocks.size - 2)
            ?.takeIf { it.speaker == current?.speaker }
            ?.let { current?.add(it.text) }
        // Name and text views update separately: a new name can arrive over the previous speaker's text.
        val staleText = live.text == current?.lastText
        turns += Turn(live.speaker, now).apply { if (!staleText) add(live.text) }
    }

    fun isEmpty(): Boolean = turns.isEmpty()

    fun text(): String = turns.joinToString("\n") { turn ->
        val name = turn.speaker.ifBlank { "?" }
        "$name (${timeFormat(turn.startedAt)})\n${turn.merger.text().trim()}\n"
    }

    private companion object {
        fun clock(ms: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ms))
    }
}
