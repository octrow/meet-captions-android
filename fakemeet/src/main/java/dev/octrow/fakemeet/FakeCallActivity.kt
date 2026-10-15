package dev.octrow.fakemeet

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView

/** Shows the caption updates Meet would show: growing text, a punctuation rewrite, a speaker change. */
class FakeCallActivity : Activity() {
    private val script = listOf(
        "Аня" to "привет как",
        "Аня" to "привет как дела",
        "Аня" to "Привет, как дела? Начнём",
        "Боб" to "да",
        "Боб" to "да, я подготовил",
        "Боб" to "да, я подготовил цифры",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = TextView(this).apply { id = R.id.speaker_name }
        val text = TextView(this).apply { id = R.id.caption_text }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            addView(name)
            addView(text)
        })
        val handler = Handler(Looper.getMainLooper())
        script.forEachIndexed { i, (speaker, caption) ->
            handler.postDelayed({
                name.text = speaker
                text.text = caption
            }, (i + 1) * STEP_MS)
        }
    }

    private companion object {
        const val STEP_MS = 1_000L
    }
}
