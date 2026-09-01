package io.github.hamzatadlaoui.socialgraph.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Someone's face, or their initials when there is no photo yet.
 *
 * Decoding happens off the main thread and is keyed on the file name, so
 * scrolling a long list does not re-read the same JPEG over and over.
 */
@Composable
fun Avatar(
    name: String,
    photo: String,
    photos: PhotoStore,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    var bitmap by remember(photo, pixels) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photo, pixels) {
        bitmap = if (photo.isEmpty()) null else withContext(Dispatchers.IO) {
            photos.decode(photo, pixels)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RectangleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
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
            Text(
                text = initials(name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** "Reda Berrada" becomes "RB"; one word gives one letter; nothing gives "?". */
internal fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> "?"
        1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}
