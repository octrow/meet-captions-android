package dev.octrow.meetcaptions

import android.accessibilityservice.AccessibilityService
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reads Meet's on-screen captions and keeps them in Documents/MeetCaptions/<start time>.txt. */
class CaptionService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { save() }
    private var merger = CaptionMerger()
    private var sessionFile: File? = null
    private var lastSnapshot = ""
    private var lastCaptionAt = 0L
    private var lastDumpAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val root = meetRoot() ?: return
        val prefs = Prefs(this)
        val now = System.currentTimeMillis()
        if (prefs.debugDump && now - lastDumpAt > DUMP_INTERVAL_MS) {
            lastDumpAt = now
            write(File(outDir(), "dump_${stamp(now)}.txt"), dump(root))
        }
        val idRegex = Regex(prefs.idRegex, RegexOption.IGNORE_CASE)
        val snapshot = buildList { collectCaptions(root, idRegex, this) }.joinToString(" ")
        if (snapshot.isBlank() || snapshot == lastSnapshot) return
        if (now - lastCaptionAt > SESSION_GAP_MS) startSession(now)
        lastSnapshot = snapshot
        lastCaptionAt = now
        merger.add(snapshot)
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, SAVE_DELAY_MS)
    }

    override fun onInterrupt() = save()

    override fun onDestroy() {
        save()
        super.onDestroy()
    }

    private fun meetRoot(): AccessibilityNodeInfo? =
        windows.firstNotNullOfOrNull { w -> w.root?.takeIf { it.packageName in MEET_PACKAGES } }

    private fun collectCaptions(node: AccessibilityNodeInfo, idRegex: Regex, out: MutableList<String>) {
        val id = node.viewIdResourceName
        val text = node.text?.toString()
        if (id != null && text != null && idRegex.containsMatchIn(id)) {
            out.add(text)
            return
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectCaptions(it, idRegex, out) }
    }

    private fun startSession(now: Long) {
        save()
        merger = CaptionMerger()
        sessionFile = File(outDir(), "${stamp(now)}.txt")
    }

    private fun save() {
        val file = sessionFile ?: return
        if (!merger.isEmpty()) write(file, merger.text())
    }

    private fun write(file: File, text: String) {
        try {
            file.writeText(text)
        } catch (e: Exception) {
            Log.e(TAG, "cannot write ${file.path}", e)
        }
    }

    private fun dump(node: AccessibilityNodeInfo, depth: Int = 0): String = buildString {
        append("  ".repeat(depth))
        append("${node.className} id=${node.viewIdResourceName} text=${node.text} desc=${node.contentDescription}\n")
        for (i in 0 until node.childCount) node.getChild(i)?.let { append(dump(it, depth + 1)) }
    }

    companion object {
        private const val TAG = "MeetCaptions"
        private val MEET_PACKAGES = setOf("com.google.android.apps.tachyon", "com.google.android.apps.meetings")
        private const val SAVE_DELAY_MS = 3_000L
        private const val DUMP_INTERVAL_MS = 10_000L
        private const val SESSION_GAP_MS = 30 * 60_000L

        fun outDir(): File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "MeetCaptions",
        ).apply { mkdirs() }

        private fun stamp(ms: Long) = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(ms))
    }
}
