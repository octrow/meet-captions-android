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
    private var transcript = Transcript()
    private var sessionFile: File? = null
    private var lastSnapshot = emptyList<Block>()
    private var lastCaptionAt = 0L
    private var debug: DebugRecorder? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val prefs = Prefs(this)
        val now = System.currentTimeMillis()
        if (prefs.debugDump) debugRecorder().onEvent(event, windows) else debug = null
        val roots = meetRoots()
        if (roots.isEmpty()) return
        val collector = BlockCollector(prefs.idRegex, prefs.nameRegex)
        val snapshot = roots.flatMap(collector::collect)
        if (snapshot.isNotEmpty() && snapshot != lastSnapshot) record(snapshot, now)
    }

    // A new events file each time debug mode is switched on.
    private fun debugRecorder(): DebugRecorder =
        debug ?: DebugRecorder(outDir(), MEET_PACKAGES).also { debug = it }

    private fun record(snapshot: List<Block>, now: Long) {
        if (now - lastCaptionAt > SESSION_GAP_MS) startSession(now)
        lastSnapshot = snapshot
        lastCaptionAt = now
        transcript.add(snapshot, now)
        // Not a debounce: captions change every ~300 ms while people talk, so a reset timer never fires.
        if (!handler.hasCallbacks(saveRunnable)) handler.postDelayed(saveRunnable, SAVE_DELAY_MS)
    }

    override fun onInterrupt() = save()

    override fun onDestroy() {
        save()
        super.onDestroy()
    }

    // All Meet windows: a menu or bottom sheet sits above the call window in the list.
    private fun meetRoots(): List<AccessibilityNodeInfo> =
        windows.mapNotNull { w -> w.root?.takeIf { it.packageName in MEET_PACKAGES } }.reversed()

    private fun startSession(now: Long) {
        save()
        transcript = Transcript()
        sessionFile = File(outDir(), "${stamp(now)}.txt")
    }

    private fun save() {
        val file = sessionFile ?: return
        if (!transcript.isEmpty()) write(file, transcript.text())
    }

    private fun write(file: File, text: String) {
        try {
            file.writeText(text)
        } catch (e: Exception) {
            Log.e(TAG, "cannot write ${file.path}", e)
        }
    }


    companion object {
        private const val TAG = "MeetCaptions"
        // Meet calls also run inside Gmail (same conference UI library).
        private val MEET_PACKAGES = setOf(
            "com.google.android.apps.tachyon",
            "com.google.android.apps.meetings",
            "com.google.android.gm",
        )
        private const val SAVE_DELAY_MS = 3_000L
        // ponytail: a silence this long splits a call in two; detect call end from Meet's UI if that happens
        private const val SESSION_GAP_MS = 5 * 60_000L

        fun outDir(): File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "MeetCaptions",
        ).apply { mkdirs() }

        private fun stamp(ms: Long) = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(ms))
    }
}

/**
 * Walks the Meet window in screen order. A node whose id matches [nameRegex] sets the speaker
 * for the caption text nodes (id matches [textRegex]) that follow it.
 */
private class BlockCollector(textRegex: String, nameRegex: String) {
    private val text = Regex(textRegex, RegexOption.IGNORE_CASE)
    private val name = Regex(nameRegex, RegexOption.IGNORE_CASE)
    private val blocks = mutableListOf<Block>()
    private var speaker = ""

    fun collect(root: AccessibilityNodeInfo): List<Block> {
        visit(root)
        return blocks
    }

    private fun visit(node: AccessibilityNodeInfo) {
        val id = node.viewIdResourceName
        val value = node.text?.toString()
        if (id != null && !value.isNullOrBlank()) {
            if (name.containsMatchIn(id)) speaker = value
            else if (text.containsMatchIn(id)) blocks += Block.parse(speaker, value)
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it) }
    }
}
