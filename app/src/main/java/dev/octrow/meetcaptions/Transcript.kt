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
    }

    private val turns = mutableListOf<Turn>()

    fun add(blocks: List<Block>, now: Long) {
        val live = blocks.lastOrNull() ?: return
        val current = turns.lastOrNull()
        if (current?.speaker == live.speaker) {
            current.merger.add(live.text)
            return
        }
        blocks.getOrNull(blocks.size - 2)
            ?.takeIf { it.speaker == current?.speaker }
            ?.let { current?.merger?.add(it.text) }
        turns += Turn(live.speaker, now).apply { merger.add(live.text) }
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
