package io.github.hamzatadlaoui.socialgraph.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * The photos people have been given, one file each under `files/photos`.
 *
 * A picked photo is copied in rather than pointed at. The same reasoning as the
 * NFC reader's page store: a content URI's permission can be taken away by the
 * system or by the user tidying up their gallery, and a face that vanishes from
 * a dossier is worse than the copy costing a few hundred kilobytes.
 */
class PhotoStore(context: Context) {

    private val app = context.applicationContext
    private val root = File(app.filesDir, PHOTOS)

    fun file(name: String): File = File(root, name)

    /** Copies a picked photo in, handing back the name to store on the person. */
    fun save(uri: Uri): String? {
        root.mkdirs()
        val name = "${UUID.randomUUID()}.jpg"
        return runCatching {
            app.contentResolver.openInputStream(uri)?.use { input ->
                File(root, name).outputStream().use { input.copyTo(it) }
            } ?: return null
            name
        }.getOrNull()
    }

    /** Puts a photo back where it was, when a backup is restored. */
    fun write(name: String, bytes: ByteArray) {
        root.mkdirs()
        runCatching { File(root, name).writeBytes(bytes) }
    }

    fun delete(name: String) {
        if (name.isEmpty()) return
        runCatching { file(name).delete() }
    }

    /**
     * Crops [tag]'s region out of the picture at [source] and saves the
     * result as a new photo, handing back its name the way [save] does. This
     * is the one moment a crop is written to disk: the tag itself stays a
     * rectangle, so re-tagging never leaves a stale copy behind. A tag with
     * no region of its own ([DocumentTagEntity.whole]) copies the whole
     * picture in instead - see [previewCrop].
     */
    fun cropFrom(source: File, tag: DocumentTagEntity): String? {
        val full = runCatching { BitmapFactory.decodeFile(source.path) }.getOrNull() ?: return null
        val cropped = previewCrop(full, tag)
        root.mkdirs()
        val name = "${UUID.randomUUID()}.jpg"
        return runCatching {
            File(root, name).outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            name
        }.getOrNull()
    }

    /**
     * Decodes a photo down to roughly [size] pixels on its shortest side, which
     * is all a thumbnail or an avatar ever needs and keeps whole albums out of
     * memory at once.
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

    companion object {
        private const val PHOTOS = "photos"

        /**
         * The part of [bitmap] that [tag] marks out, or the whole bitmap when
         * the tag has no region of its own. Shared by [cropFrom], which saves
         * this to disk, and by a photo picker's live preview, so a preview
         * never promises a crop that saving would not actually produce.
         */
        fun previewCrop(bitmap: Bitmap, tag: DocumentTagEntity): Bitmap {
            if (tag.whole) return bitmap
            val x = (tag.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val y = (tag.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val w = ((tag.right - tag.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
            val h = ((tag.bottom - tag.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
            return Bitmap.createBitmap(bitmap, x, y, w, h)
        }
    }
}
