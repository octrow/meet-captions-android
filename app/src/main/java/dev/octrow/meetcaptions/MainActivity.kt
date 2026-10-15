package dev.octrow.meetcaptions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var captionLine: TextView
    private lateinit var enableButton: View
    private lateinit var restrictedHelp: View
    private lateinit var transcripts: LinearLayout
    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    // The user comes back here from system settings and from calls, so re-check every time.
    override fun onResume() {
        super.onResume()
        val on = isServiceEnabled()
        captionLine.setText(if (on) R.string.status_on else R.string.status_off)
        enableButton.visibility = if (on) View.GONE else View.VISIBLE
        restrictedHelp.visibility = if (on) View.GONE else View.VISIBLE
        showTranscripts()
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, CaptionService::class.java)
        return getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { ComponentName.unflattenFromString(it.id) == me }
    }

    private fun buildLayout(): ScrollView {
        restrictedHelp = restrictedSettingsHelp()
        transcripts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        return scrollingColumn(
            text(TextRole.Title, getString(R.string.app_name)),
            spaced(text(TextRole.Muted, getString(R.string.tagline)), STACK_GAP_DP / 2),
            spaced(captionBox(), SECTION_GAP_DP / 2 + STACK_GAP_DP),
            restrictedHelp,
            spaced(heading(R.string.section_transcripts), SECTION_GAP_DP),
            text(TextRole.Muted, getString(R.string.transcripts_folder)),
            transcripts,
            hairline(),
            expander(heading(R.string.section_regex), regexSettings()),
            hairline(),
            expander(heading(R.string.section_debug), debugSettings()),
        )
    }

    /** Centered column capped at [MAX_CONTENT_DP] so text doesn't stretch across a tablet or unfolded foldable. */
    private fun scrollingColumn(vararg views: View): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(GUTTER_DP), dp(SECTION_GAP_DP), dp(GUTTER_DP), dp(SECTION_GAP_DP))
            views.forEach(::addView)
        }
        val maxWidth = dp(MAX_CONTENT_DP)
        val width = if (resources.displayMetrics.widthPixels > maxWidth) maxWidth else FrameLayout.LayoutParams.MATCH_PARENT
        // targetSdk 35+ draws edge-to-edge: the outer view takes the system bar and IME insets.
        return ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content, FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
        }
    }

    /** Service status drawn as an on-screen caption: the app is the speaker, the status is its line. */
    private fun captionBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(CAPTION_RADIUS_DP).toFloat()
            setColor(getColor(R.color.caption_bg))
        }
        setPadding(dp(GUTTER_DP), dp(STACK_GAP_DP + 4), dp(GUTTER_DP), dp(GUTTER_DP))
        addView(text(TextRole.Speaker, getString(R.string.app_name)))
        captionLine = text(TextRole.Caption).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        addView(captionLine)
        enableButton = button(ButtonRole.OnCaption, R.string.enable_service).apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        addView(enableButton)
    }

    /**
     * Android 13+ blocks accessibility services of apps installed outside a store ("restricted setting");
     * the only way out is a menu item in App info that appears after the first blocked attempt.
     */
    private fun restrictedSettingsHelp(): View {
        val help = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(TextRole.Body, getString(R.string.restricted_help)))
            addView(button(ButtonRole.Text, R.string.open_app_info).apply {
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                }
            })
        }
        val title = text(TextRole.Body, getString(R.string.restricted_title)).apply { setTextColor(getColor(R.color.indigo)) }
        return spaced(expander(title, help), STACK_GAP_DP / 2)
    }

    // ponytail: lists the folder on the main thread; fine for a handful of files, move off-thread if it grows
    private fun showTranscripts() {
        transcripts.removeAllViews()
        val files = TranscriptProvider.recent(RECENT_TRANSCRIPTS)
        if (files.isEmpty()) transcripts.addView(spaced(text(TextRole.Body, getString(R.string.no_transcripts)), STACK_GAP_DP))
        files.forEach { transcripts.addView(transcriptRow(it)) }
    }

    private fun transcriptRow(file: File) = LinearLayout(this).apply {
        val uri = TranscriptProvider.uri(this@MainActivity, file)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(STACK_GAP_DP), 0, 0)
        addView(transcriptSummary(file, uri), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(button(ButtonRole.Text, R.string.share).apply {
            (layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri)
                launch(Intent.createChooser(send, null))
            }
        })
    }

    /** Date and first line; tapping opens the file. */
    private fun transcriptSummary(file: File, uri: Uri) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(text(TextRole.BodyStrong, title(file)))
        addView(text(TextRole.Muted, preview(file)).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        })
        setOnClickListener { launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/plain")) }
    }

    /** Session start time from the file name, in the user's locale. */
    private fun title(file: File): String {
        val start = runCatching { SimpleDateFormat(CaptionService.FILE_STAMP, Locale.US).parse(file.nameWithoutExtension)!!.time }
            .getOrDefault(file.lastModified())
        return DateUtils.formatDateTime(this, start, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH)
    }

    /** First caption line in quotes, skipping the "Speaker (time)" header. */
    private fun preview(file: File): String {
        val line = file.useLines { lines -> lines.drop(1).firstOrNull { it.isNotBlank() } }
        return if (line == null) getString(R.string.empty_transcript) else getString(R.string.quoted, line)
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun regexSettings(): View {
        val textRegex = regexField(prefs.idRegex, R.string.hint_text_regex)
        val nameRegex = regexField(prefs.nameRegex, R.string.hint_name_regex)
        val actions = LinearLayout(this).apply {
            addView(button(ButtonRole.Primary, R.string.save_regex).apply { setOnClickListener { saveRegex(textRegex, nameRegex) } })
            addView(button(ButtonRole.Text, R.string.reset_regex).apply { setOnClickListener { resetRegex(textRegex, nameRegex) } })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(TextRole.Muted, getString(R.string.regex_note)))
            addView(spaced(label(R.string.label_text_regex, textRegex), STACK_GAP_DP))
            addView(textRegex)
            addView(spaced(label(R.string.label_name_regex, nameRegex), STACK_GAP_DP))
            addView(nameRegex)
            addView(actions)
        }
    }

    private fun debugSettings() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(Switch(this@MainActivity).apply {
            style(TextRole.Body)
            setText(R.string.debug_switch)
            minHeight = dp(MIN_TOUCH_DP)
            isChecked = prefs.debugDump
            setOnCheckedChangeListener { _, on -> prefs.debugDump = on }
        })
        addView(text(TextRole.Muted, getString(R.string.debug_note)))
    }

    private fun regexField(value: String, hint: Int) = EditText(this).apply {
        id = View.generateViewId()
        style(TextRole.Body)
        setText(value)
        setHint(hint)
        isSingleLine = true
        typeface = Typeface.MONOSPACE
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }

    private fun heading(title: Int) = text(TextRole.Heading, getString(title)).apply { isAccessibilityHeading = true }

    private fun label(title: Int, field: View) = text(TextRole.Muted, getString(title)).apply { labelFor = field.id }

    private fun <T : View> spaced(view: T, topDp: Int): T = view.apply {
        layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(topDp) }
    }

    private fun saveRegex(text: EditText, name: EditText) {
        val invalid = listOf(text, name).filterNot(::validateShowingError)
        if (invalid.isNotEmpty()) {
            invalid.first().requestFocus()
            return
        }
        prefs.idRegex = text.text.toString()
        prefs.nameRegex = name.text.toString()
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun resetRegex(text: EditText, name: EditText) {
        text.setText(Prefs.DEFAULT_ID_REGEX)
        name.setText(Prefs.DEFAULT_NAME_REGEX)
        saveRegex(text, name)
    }

    /** Sets or clears the field's inline error; true when it holds a usable regex. */
    private fun validateShowingError(field: EditText): Boolean {
        val s = field.text.toString()
        val ok = s.isNotBlank() && runCatching { Regex(s) }.isSuccess
        field.error = if (ok) null else getString(R.string.bad_regex)
        return ok
    }

    private companion object {
        const val MAX_CONTENT_DP = 600
        const val CAPTION_RADIUS_DP = 6
        const val RECENT_TRANSCRIPTS = 5
    }
}
