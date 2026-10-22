package dev.octrow.meetcaptions

/**
 * Turns a stream of caption-area snapshots into a transcript without duplicates.
 *
 * Meet keeps a few caption lines on screen: they grow, the recognizer revises the last
 * words, and old lines scroll off the top. Each snapshot is the whole visible caption text.
 * We find where the snapshot's first words sit in the current block: the part before that
 * scrolled away, the rest is replaced by the snapshot. No match means the block is gone.
 */
class CaptionMerger(private val anchorWords: Int = 3) {
    private val committed = StringBuilder()
    private var block: List<String> = emptyList()

    fun add(snapshot: String) {
        val words = snapshot.split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return
        val start = anchorIndex(words.take(anchorWords))
        if (start < 0) {
            commit()
            block = words
        } else {
            block = block.take(start) + words
        }
    }

    /** Ends the current block, e.g. when Meet closes or captions stop for a while. */
    fun commit() {
        if (block.isEmpty()) return
        committed.append(block.joinToString(" ")).append('\n')
        block = emptyList()
    }

    fun text(): String = committed.toString() + block.joinToString(" ")

    fun isEmpty(): Boolean = committed.isEmpty() && block.isEmpty()

    // ponytail: ASR replacing one of the first anchor words still splits the block; fuzzy match if real logs show it
    // A full anchor match anywhere wins; only then may the anchor run past the block's end (new words).
    private fun anchorIndex(anchor: List<String>): Int {
        val key = anchor.map(::normalize)
        val words = block.map(::normalize)
        val full = (words.size - key.size downTo 0).firstOrNull { words.subList(it, it + key.size) == key }
        if (full != null) return full
        val minOverlap = minOf(MIN_OVERLAP, key.size, words.size)
        // An overlap as long as the anchor only differs by a completed last word: it needs MIN_OVERLAP
        // words, or a one-word snapshot ("отлично") would continue any block ending in "о".
        return (words.size - 1 downTo words.size - key.size)
            .filter { it >= 0 && words.size - it >= minOverlap }
            .filter { words.size - it < key.size || words.size - it >= MIN_OVERLAP }
            .firstOrNull { tailMatches(words.subList(it, words.size), key.subList(0, words.size - it)) } ?: -1
    }

    // Meet shows a word while it is still being recognized ("провер" before "проверка").
    private fun tailMatches(tail: List<String>, key: List<String>): Boolean =
        tail.dropLast(1) == key.dropLast(1) && key.last().startsWith(tail.last())

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val PUNCTUATION = Regex("[\\p{P}\\p{S}]")
        const val MIN_OVERLAP = 2

        /** Meet rewrites case and punctuation of earlier words, so they must not break a match. */
        fun normalize(word: String) = word.lowercase().replace(PUNCTUATION, "")
    }
}
