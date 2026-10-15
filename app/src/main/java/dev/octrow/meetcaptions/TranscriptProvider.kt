package dev.octrow.meetcaptions

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Read-only access to transcripts for apps we open or share them with (FileProvider without AndroidX).
 * Files written through java.io.File are not in MediaStore, so there is no system URI to hand out.
 */
class TranscriptProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = MIME

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(fileOf(uri), ParcelFileDescriptor.MODE_READ_ONLY)

    // Share targets (Telegram, Gmail) ask for the name and size before reading.
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?, sort: String?): Cursor {
        val file = fileOf(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns, 1).apply {
            addRow(columns.map { if (it == OpenableColumns.SIZE) file.length() else file.name })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<String>?) = 0
    override fun delete(uri: Uri, selection: String?, args: Array<String>?) = 0

    /** Only a transcript name is accepted, so a crafted path can't reach other files. */
    private fun fileOf(uri: Uri): File {
        val name = uri.lastPathSegment
        if (name == null || !NAME.matches(name)) throw FileNotFoundException(uri.toString())
        return File(CaptionService.outDir(), name).takeIf { it.isFile } ?: throw FileNotFoundException(uri.toString())
    }

    companion object {
        private const val MIME = "text/plain"
        /** Session files written by [CaptionService]; debug dumps share the folder and are skipped. */
        private val NAME = Regex("""\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.txt""")

        fun recent(limit: Int): List<File> =
            CaptionService.outDir().listFiles { f -> NAME.matches(f.name) }.orEmpty()
                .sortedByDescending { it.name }
                .take(limit)

        fun uri(context: Context, file: File): Uri =
            Uri.Builder().scheme("content").authority("${context.packageName}.transcripts").appendPath(file.name).build()
    }
}
