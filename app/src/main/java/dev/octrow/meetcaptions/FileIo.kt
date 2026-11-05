package dev.octrow.meetcaptions

import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val TAG = "MeetCaptions"

/**
 * Writes [text] to a temp file next to [file], then renames it over [file], so a kill
 * mid-write cannot truncate the only copy. Returns false (and logs) on failure.
 */
fun writeAtomic(file: File, text: String): Boolean {
    val tmp = File(file.parentFile, file.name + ".tmp")
    return try {
        tmp.writeText(text)
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        true
    } catch (e: Exception) {
        runCatching { tmp.delete() }
        runCatching { Log.e(TAG, "cannot write ${file.path}", e) }
        false
    }
}
