package dev.octrow.meetcaptions

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    /** Matched against view resource ids to find caption text nodes. */
    var idRegex: String
        get() = sp.getString("id_regex", DEFAULT_ID_REGEX)!!
        set(v) = sp.edit().putString("id_regex", v).apply()

    var debugDump: Boolean
        get() = sp.getBoolean("debug_dump", false)
        set(v) = sp.edit().putBoolean("debug_dump", v).apply()

    companion object {
        // ponytail: guessed id, confirm from a debug dump of a real call
        const val DEFAULT_ID_REGEX = "caption"
    }
}
