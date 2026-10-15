package dev.octrow.meetcaptions

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTest {
    private fun transcript() = Transcript(timeFormat = { "t$it" })

    @Test
    fun growingTurnOfOneSpeakerIsOneEntry() {
        val t = transcript()
        t.add(listOf(Block("Аня", "привет как")), 1)
        t.add(listOf(Block("Аня", "привет как дела")), 2)
        assertEquals("Аня (t1)\nпривет как дела\n", t.text())
    }

    @Test
    fun speakerChangeStartsNewEntryAndKeepsLastWordsOfPrevious() {
        val t = transcript()
        t.add(listOf(Block("Аня", "привет как")), 1)
        t.add(listOf(Block("Аня", "привет как дела"), Block("Боб", "нормально")), 2)
        assertEquals("Аня (t1)\nпривет как дела\n\nБоб (t2)\nнормально\n", t.text())
    }

    @Test
    fun olderVisibleBlocksAreNotRepeated() {
        val t = transcript()
        t.add(listOf(Block("Аня", "раз два")), 1)
        t.add(listOf(Block("Аня", "раз два"), Block("Боб", "три четыре")), 2)
        t.add(listOf(Block("Аня", "раз два"), Block("Боб", "три четыре пять")), 3)
        assertEquals("Аня (t1)\nраз два\n\nБоб (t2)\nтри четыре пять\n", t.text())
    }

    @Test
    fun missingNameStillRecordsText() {
        val t = transcript()
        t.add(listOf(Block("", "текст без имени")), 1)
        assertEquals("? (t1)\nтекст без имени\n", t.text())
    }
}
