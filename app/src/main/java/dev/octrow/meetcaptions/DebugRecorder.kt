package dev.octrow.meetcaptions

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug mode only: logs every Meet event and periodically dumps the window list plus Meet's node trees,
 * so the caption and speaker nodes can be found from one real call.
 */
class DebugRecorder(private val dir: File, private val meetPackages: Set<String>) {
    private val started = System.currentTimeMillis()
    private val eventsFile = File(dir, "events_${stamp(started, FILE_STAMP)}.txt")
    private var lastDumpAt = 0L

    fun onEvent(event: AccessibilityEvent, windows: List<AccessibilityWindowInfo>) {
        val now = System.currentTimeMillis()
        if (eventsFile.length() < MAX_EVENTS_BYTES) append(eventsFile, describe(event, now))
        if (now - lastDumpAt < DUMP_INTERVAL_MS) return
        lastDumpAt = now
        write(File(dir, "dump_${stamp(now, FILE_STAMP)}.txt"), dumpWindows(windows))
    }

    private fun describe(event: AccessibilityEvent, now: Long): String {
        val type = AccessibilityEvent.eventTypeToString(event.eventType)
        val source = event.source?.viewIdResourceName
        return "${stamp(now, CLOCK)} $type pkg=${event.packageName} cls=${event.className} src=$source " +
            "changes=${event.contentChangeTypes} text=${event.text} desc=${event.contentDescription}\n"
    }

    // Other apps' windows are listed by title only: their content is none of our business.
    private fun dumpWindows(windows: List<AccessibilityWindowInfo>): String = buildString {
        for (w in windows) {
            val root = w.root
            append("# window type=${w.type} layer=${w.layer} active=${w.isActive} title=${w.title} pkg=${root?.packageName}\n")
            if (root != null && root.packageName in meetPackages) append(dumpTree(root))
        }
    }

    private fun dumpTree(node: AccessibilityNodeInfo, depth: Int = 0): String = buildString {
        append("  ".repeat(depth))
        append("${node.className} id=${node.viewIdResourceName} text=${node.text} desc=${node.contentDescription}")
        append(" visible=${node.isVisibleToUser}\n")
        for (i in 0 until node.childCount) node.getChild(i)?.let { append(dumpTree(it, depth + 1)) }
    }

    private fun append(file: File, text: String) = safely(file) { file.appendText(text) }

    private fun write(file: File, text: String) = safely(file) { file.writeText(text) }

    private fun safely(file: File, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "cannot write ${file.path}", e)
        }
    }

    private companion object {
        const val TAG = "MeetCaptions"
        const val DUMP_INTERVAL_MS = 5_000L
        const val MAX_EVENTS_BYTES = 5L * 1024 * 1024
        const val FILE_STAMP = "yyyy-MM-dd_HH-mm-ss"
        const val CLOCK = "HH:mm:ss.SSS"

        fun stamp(ms: Long, pattern: String) = SimpleDateFormat(pattern, Locale.US).format(Date(ms))
    }
}
