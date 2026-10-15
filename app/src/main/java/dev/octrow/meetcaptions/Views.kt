package dev.octrow.meetcaptions

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// Design tokens live in res/values{,-night}/colors.xml; type scale and spacing live here.

/** Type scale 28 / 20 / 16 / 14 in Golos Text, one family, two weights. */
enum class TextRole(val sp: Float, val font: Int, val color: Int, val lineSpacing: Float = 1.25f) {
    Title(28f, R.font.golos_semibold, R.color.ink, 1.1f),
    Heading(20f, R.font.golos_semibold, R.color.ink),
    Body(16f, R.font.golos_regular, R.color.ink),
    BodyStrong(16f, R.font.golos_semibold, R.color.ink),
    Muted(14f, R.font.golos_regular, R.color.muted),
    Speaker(14f, R.font.golos_semibold, R.color.caption_speaker),
    Caption(20f, R.font.golos_regular, R.color.caption_text, 1.3f),
}

enum class ButtonRole(val fill: Int?, val text: Int) {
    Primary(R.color.indigo, R.color.on_indigo),
    OnCaption(R.color.caption_speaker, R.color.caption_bg),
    Text(null, R.color.indigo),
}

const val GUTTER_DP = 20
const val SECTION_GAP_DP = 32
const val STACK_GAP_DP = 12
const val MIN_TOUCH_DP = 48
private const val BUTTON_PAD_DP = 20
private const val RIPPLE_ALPHA = 0x29

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun TextView.style(role: TextRole) {
    textSize = role.sp
    typeface = resources.getFont(role.font)
    setTextColor(context.getColor(role.color))
    setLineSpacing(0f, role.lineSpacing)
}

fun Context.text(role: TextRole, value: CharSequence = "") = TextView(this).apply {
    style(role)
    text = value
}

/** Pill button sized to its label; [ButtonRole.Text] has no fill. */
fun Context.button(role: ButtonRole, label: Int) = Button(this, null, 0, 0).apply {
    setText(label)
    isAllCaps = false
    style(TextRole.BodyStrong)
    setTextColor(getColor(role.text))
    gravity = Gravity.CENTER
    minHeight = dp(MIN_TOUCH_DP)
    setPadding(dp(BUTTON_PAD_DP), 0, dp(BUTTON_PAD_DP), 0)
    val shape = GradientDrawable().apply {
        cornerRadius = dp(MIN_TOUCH_DP / 2).toFloat()
        setColor(role.fill?.let(::getColor) ?: 0)
    }
    val ripple = ColorStateList.valueOf((RIPPLE_ALPHA shl 24) or (getColor(role.text) and 0xFFFFFF))
    background = RippleDrawable(ripple, shape, shape)
    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(STACK_GAP_DP) }
}

fun Context.hairline() = View(this).apply {
    setBackgroundColor(getColor(R.color.line))
    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        .apply { topMargin = dp(SECTION_GAP_DP / 2) }
}

/** Collapsed section: tapping [header] shows or hides [content], with a chevron that turns. */
fun Context.expander(header: TextView, content: View) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    layoutTransition = LayoutTransition()
    content.visibility = View.GONE
    addView(expanderRow(header, content))
    addView(content)
}

private fun Context.expanderRow(header: TextView, content: View) = LinearLayout(this).apply {
    val chevron = text(TextRole.Heading, "›").apply { setTextColor(header.textColors) }
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(MIN_TOUCH_DP)
    background = selectableBackground()
    addView(header, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    addView(chevron)
    stateDescription = getString(R.string.collapsed)
    setOnClickListener {
        val open = content.visibility != View.VISIBLE
        content.visibility = if (open) View.VISIBLE else View.GONE
        chevron.animate().rotation(if (open) 90f else 0f)
        stateDescription = getString(if (open) R.string.expanded else R.string.collapsed)
    }
}

private fun Context.selectableBackground() =
    obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).run {
        getDrawable(0).also { recycle() }
    }
