package dev.octrow.meetcaptions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout(Prefs(this)))
    }

    private fun buildLayout(prefs: Prefs): LinearLayout {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val regex = EditText(this).apply { setText(prefs.idRegex) }
        return LinearLayout(this).apply {
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
            addView(TextView(this@MainActivity).apply { text = "\nRegex id узлов субтитров:" })
            addView(regex)
            addView(Button(this@MainActivity).apply {
                text = "Сохранить regex"
                setOnClickListener { saveRegex(prefs, regex.text.toString()) }
            })
            addView(CheckBox(this@MainActivity).apply {
                text = "Отладка: дамп экрана Meet раз в 10 с"
                isChecked = prefs.debugDump
                setOnCheckedChangeListener { _, on -> prefs.debugDump = on }
            })
        }
    }

    private fun saveRegex(prefs: Prefs, value: String) {
        val ok = runCatching { Regex(value) }.isSuccess && value.isNotBlank()
        if (ok) prefs.idRegex = value
        Toast.makeText(this, if (ok) "Сохранено" else "Некорректный regex", Toast.LENGTH_SHORT).show()
    }
}
