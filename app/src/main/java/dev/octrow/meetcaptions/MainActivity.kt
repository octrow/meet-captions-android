package dev.octrow.meetcaptions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout(Prefs(this)))
    }

    private fun buildLayout(prefs: Prefs): ScrollView {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val textRegex = EditText(this).apply { setText(prefs.idRegex) }
        val nameRegex = EditText(this).apply { setText(prefs.nameRegex) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(TextView(this@MainActivity).apply {
                text = "1. Включите службу «Meet Captions» в специальных возможностях.\n" +
                    "2. В Meet включите субтитры (язык: русский).\n" +
                    "Файлы: ${CaptionService.outDir().path}"
            })
            addView(Button(this@MainActivity).apply {
                text = "Открыть специальные возможности"
                setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            })
            addView(TextView(this@MainActivity).apply { text = "\nRegex id текста субтитров:" })
            addView(textRegex)
            addView(TextView(this@MainActivity).apply { text = "Regex id имени говорящего:" })
            addView(nameRegex)
            addView(Button(this@MainActivity).apply {
                text = "Сохранить regex"
                setOnClickListener { saveRegex(prefs, textRegex.text.toString(), nameRegex.text.toString()) }
            })
            addView(CheckBox(this@MainActivity).apply {
                text = "Отладка: дамп экрана Meet раз в 10 с"
                isChecked = prefs.debugDump
                setOnCheckedChangeListener { _, on -> prefs.debugDump = on }
            })
        }
        // targetSdk 35+ draws edge-to-edge: the outer view takes the system bar insets, the inner keeps its padding.
        return ScrollView(this).apply {
            fitsSystemWindows = true
            addView(content)
        }
    }

    private fun saveRegex(prefs: Prefs, text: String, name: String) {
        val ok = listOf(text, name).all { it.isNotBlank() && runCatching { Regex(it) }.isSuccess }
        if (ok) {
            prefs.idRegex = text
            prefs.nameRegex = name
        }
        Toast.makeText(this, if (ok) "Сохранено" else "Некорректный regex", Toast.LENGTH_SHORT).show()
    }
}
