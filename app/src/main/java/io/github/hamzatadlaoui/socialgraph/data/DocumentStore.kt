package io.github.hamzatadlaoui.socialgraph.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.util.UUID

/** What the system could tell us about a file when it was picked. */
data class PickedFile(val fileName: String, val originalName: String, val mimeType: String, val size: Long)

/**
 * The documents people have kept, one file each under `files/documents`.
 *
 * The same bargain as [PhotoStore]: a picked file is copied in rather than
 * pointed at, so that nothing in a dossier depends on a permission the system
 * can withdraw later. Unlike photos these are of any type at all, so the store
 * keeps the original extension and hands back what the system said the file was.
 */
class DocumentStore(context: Context) {

    private val app = context.applicationContext
    private val root = File(app.filesDir, DOCUMENTS)

    fun file(name: String): File = File(root, name)

    /**
     * Copies a picked file in, keeping its extension so that anything opening it
     * later has the same hint the user's file manager had.
     */
    fun save(uri: Uri): PickedFile? {
        root.mkdirs()
        val resolver = app.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val original = displayName(uri)
        val extension = original.substringAfterLast('.', "").take(EXTENSION_MAX)
        val name = UUID.randomUUID().toString() + if (extension.isEmpty()) "" else ".$extension"

        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                File(root, name).outputStream().use { input.copyTo(it) }
            } ?: return null
            PickedFile(
                fileName = name,
                originalName = original,
                mimeType = mime,
                size = file(name).length(),
            )
        }.onFailure {
            // A copy that fails silently is how a file appears to vanish.
            Log.w(TAG, "Could not copy in the picked file", it)
            delete(name)
        }.getOrNull()
    }

    /** Puts a document back where it was, when a backup is restored. */
    fun write(name: String, bytes: ByteArray) {
        root.mkdirs()
        runCatching { File(root, name).writeBytes(bytes) }
    }

    fun delete(name: String) {
        if (name.isEmpty()) return
        runCatching { file(name).delete() }
    }

    /** What every document on disk adds up to, for the warning before an export. */
    fun totalBytes(): Long =
        runCatching { root.listFiles()?.sumOf { it.length() } ?: 0L }.getOrDefault(0L)

    /**
     * Decodes an image document down to roughly [size] pixels on its shortest
     * side. Non-images give back null, which is what the icon fallback is for.
     */
    fun decode(name: String, size: Int): Bitmap? {
        val file = file(name).takeIf { it.isFile } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)

        val smallest = minOf(bounds.outWidth, bounds.outHeight).takeIf { it > 0 } ?: return null
        var sample = 1
        while (smallest / (sample * 2) >= size) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeFile(file.path, options) }.getOrNull()
    }

    /** The name the picker showed the user, or the last path segment if it will not say. */
    private fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()
        return fromProvider ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')
    }

    private companion object {
        const val TAG = "DocumentStore"
        const val DOCUMENTS = "documents"

        /** Long enough for ".jpeg" and short enough that a junk name cannot run away with it. */
        const val EXTENSION_MAX = 8
    }
}
