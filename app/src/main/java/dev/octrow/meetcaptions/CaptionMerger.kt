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

    // ponytail: exact word match, ASR revising one of the first anchor words splits the block; fuzzy match if that shows up in real logs
    // The anchor may run past the block's end (new words), so a shorter overlap there counts too.
    private fun anchorIndex(anchor: List<String>): Int {
        val minOverlap = minOf(MIN_OVERLAP, anchor.size)
        for (i in block.size - minOverlap downTo 0) {
            val len = minOf(anchor.size, block.size - i)
            if (block.subList(i, i + len) == anchor.subList(0, len)) return i
        }
        return -1
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val MIN_OVERLAP = 2
    }
}
