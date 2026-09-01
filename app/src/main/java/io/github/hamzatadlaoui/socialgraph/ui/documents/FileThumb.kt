package io.github.hamzatadlaoui.socialgraph.ui.documents

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A picture of the file where there can be one, and the closest icon where
 * there cannot. Decoding happens off the main thread and is keyed on the file
 * name, the same bargain [io.github.hamzatadlaoui.socialgraph.ui.Avatar] makes.
 */
@Composable
fun FileThumb(
    fileName: String,
    mimeType: String,
    files: DocumentStore,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val isImage = mimeType.startsWith("image/")
    var bitmap by remember(fileName, pixels) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(fileName, pixels, isImage) {
        bitmap = if (!isImage) null else withContext(Dispatchers.IO) {
            files.decode(fileName, pixels)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                imageVector = iconFor(mimeType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The nearest icon the material set has for what this file claims to be. */
fun iconFor(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> Icons.Default.Image
    mimeType.startsWith("audio/") -> Icons.Default.AudioFile
    mimeType.startsWith("video/") -> Icons.Default.VideoFile
    mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
    mimeType.startsWith("text/") -> Icons.Default.Article
    else -> Icons.Default.Description
}

/** "1.4 MB", near enough for a list row. */
fun readableSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "%.0f kB".format(bytes / 1_024.0)
    bytes < 1_073_741_824 -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "%.1f GB".format(bytes / 1_073_741_824.0)
}
