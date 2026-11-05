package dev.octrow.meetcaptions

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileIoTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun writesTextAndLeavesNoTempFile() {
        val f = File(tmp.root, "a.txt")
        assertTrue(writeAtomic(f, "hello"))
        assertEquals("hello", f.readText())
        assertEquals(listOf("a.txt"), tmp.root.list()!!.toList())
    }

    @Test
    fun replacesOldContent() {
        val f = tmp.newFile("a.txt").apply { writeText("old old old") }
        assertTrue(writeAtomic(f, "new"))
        assertEquals("new", f.readText())
    }

    @Test
    fun failureReturnsFalseWhenFolderIsMissing() {
        val f = File(tmp.root, "missing-dir/a.txt")
        assertFalse(writeAtomic(f, "x"))
        assertFalse(f.exists())
    }

    @Test
    fun failedTempWriteKeepsOldFile() {
        val f = tmp.newFile("a.txt").apply { writeText("old") }
        tmp.newFolder("a.txt.tmp") // the temp file cannot be created
        assertFalse(writeAtomic(f, "new"))
        assertEquals("old", f.readText())
    }
}
