package dev.octrow.meetcaptions

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionMergerTest {
    @Test
    fun growingCaptionIsNotDuplicated() {
        val m = CaptionMerger()
        m.add("привет как")
        m.add("привет как дела")
        m.add("привет как дела у тебя")
        assertEquals("привет как дела у тебя", m.text())
    }

    @Test
    fun revisedTailIsReplaced() {
        val m = CaptionMerger()
        m.add("мы начнём в пять")
        m.add("мы начнём в пятницу утром")
        assertEquals("мы начнём в пятницу утром", m.text())
    }

    @Test
    fun scrolledOffLinesAreKept() {
        val m = CaptionMerger()
        m.add("первая строка текста вторая строка")
        m.add("вторая строка и третья")
        assertEquals("первая строка текста вторая строка и третья", m.text())
    }

    @Test
    fun unrelatedSnapshotStartsNewBlock() {
        val m = CaptionMerger()
        m.add("один два три четыре")
        m.add("совсем другой текст")
        assertEquals("один два три четыре\nсовсем другой текст", m.text())
    }

    @Test
    fun commitIsIdempotent() {
        val m = CaptionMerger()
        m.add("слово")
        m.commit()
        m.commit()
        assertEquals("слово\n", m.text())
    }
}
