package io.github.hamzatadlaoui.socialgraph.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The faces the hand-drawn canvases need, decoded once and keyed on the file
 * name rather than on the person, so two people sharing a photograph decode it
 * once and a rename costs nothing.
 *
 * Decoding happens off the main thread and at a size fit for a node on a graph,
 * not for a full-screen picture: a network of forty people should not pull forty
 * camera-resolution bitmaps into memory to draw forty small squares.
 */
@Composable
fun rememberPortraits(
    photos: PhotoStore,
    fileNames: List<String>,
    sizePx: Int,
): Map<String, ImageBitmap> {
    val wanted = remember(fileNames) { fileNames.filter { it.isNotEmpty() }.distinct().sorted() }
    var loaded by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    LaunchedEffect(wanted, sizePx) {
        if (wanted.isEmpty()) {
            loaded = emptyMap()
            return@LaunchedEffect
        }
        loaded = withContext(Dispatchers.IO) {
            wanted.mapNotNull { name ->
                photos.decode(name, sizePx)?.let { name to it.asImageBitmap() }
            }.toMap()
        }
    }
    return loaded
}

/**
 * Draws [image] to fill [side] pixels square at [left], [top], taking the middle
 * of the picture when it is not square itself - the same centre crop an avatar
 * gets, so a face does not stretch just because it is on a graph.
 */
fun DrawScope.drawPortrait(
    image: ImageBitmap,
    left: Float,
    top: Float,
    side: Float,
) {
    val source = minOf(image.width, image.height).coerceAtLeast(1)
    drawImage(
        image = image,
        srcOffset = IntOffset((image.width - source) / 2, (image.height - source) / 2),
        srcSize = IntSize(source, source),
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(side.toInt().coerceAtLeast(1), side.toInt().coerceAtLeast(1)),
    )
}
