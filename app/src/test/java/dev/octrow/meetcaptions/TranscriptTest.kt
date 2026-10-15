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

    @Test
    fun blockWithMissingNameContinuesCurrentSpeaker() {
        val t = transcript()
        t.add(listOf(Block("Аня", "раз два")), 1)
        t.add(listOf(Block("", "раз два три")), 2)
        t.add(listOf(Block("Аня", "раз два три четыре")), 3)
        assertEquals("Аня (t1)\nраз два три четыре\n", t.text())
    }

    @Test
    fun newNameOverStaleTextDoesNotCopyPreviousTurn() {
        val t = transcript()
        t.add(listOf(Block("Аня", "привет как дела")), 1)
        t.add(listOf(Block("Боб", "привет как дела")), 2)
        t.add(listOf(Block("Боб", "да")), 3)
        assertEquals("Аня (t1)\nпривет как дела\n\nБоб (t2)\nда\n", t.text())
    }

    @Test
    fun inlineSpeakerPrefixIsParsed() {
        assertEquals(listOf(Block("Dan P", "так проверка")), Block.parse("", "Dan P: так проверка"))
        assertEquals(
            listOf(Block("Вы", "раз два"), Block("Аня", "три")),
            Block.parse("", "Вы: раз два\nАня: три"),
        )
        assertEquals(listOf(Block("Боб", "без префикса")), Block.parse("Боб", "без префикса"))
    }

    @Test
    fun speakerChangeInRealGmailSnapshotsStartsNewTurn() {
        val t = transcript()
        listOf(
            "Dan P: 1 2 3 4 5",
            "Dan P: 1 2 3 4 5 проверка проверка",
            "Вы: проверка проверка 15 16",
            "Вы: проверка проверка 15 16 проверка",
        ).forEachIndexed { i, s -> t.add(Block.parse("", s), i.toLong()) }
        assertEquals("Dan P (t0)\n1 2 3 4 5 проверка проверка\n\nВы (t2)\nпроверка проверка 15 16 проверка\n", t.text())
    }
}
