package io.github.hamzatadlaoui.socialgraph.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.ui.Avatar
import io.github.hamzatadlaoui.socialgraph.ui.drawBrackets
import io.github.hamzatadlaoui.socialgraph.ui.theme.Mono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One document, and who is in it.
 *
 * For a picture that means drawing a box round a face and giving it a name -
 * several times over for a group photograph. The box is kept as fractions of
 * the image, so it means the same thing however the picture is later scaled.
 * Everything that is not a picture is tagged as a whole instead, which is the
 * only sensible thing to say about a PDF or a recording.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel,
    files: DocumentStore,
    photos: PhotoStore,
    onOpenPerson: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingRegion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var openTag by remember { mutableStateOf<TaggedPerson?>(null) }
    val document = state.document

    val cannotOpen = stringResource(R.string.cannot_open_file)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = document?.label?.ifBlank { stringResource(R.string.untitled_document) }
                            ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        open(context, files, document?.fileName, document?.mimeType, cannotOpen)
                    }) {
                        Icon(Icons.Default.OpenInNew, stringResource(R.string.open_file))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_document))
                    }
                },
            )
        },
    ) { padding ->
        if (document == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (document.isImage) {
                TaggableImage(
                    fileName = document.fileName,
                    files = files,
                    tagged = state.tagged,
                    onRegionDrawn = viewModel::regionDrawn,
                    onTagTapped = { openTag = it },
                )
                Text(
                    text = stringResource(R.string.drag_to_tag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                NonImage(document.mimeType, document.originalName, document.sizeBytes)
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.tagged_count,
                        state.tagged.size,
                        state.tagged.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { viewModel.tagWhole() }) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.tag_whole_file),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            for (tagged in state.tagged) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openTag = tagged }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Avatar(tagged.person.displayName, tagged.person.photo, photos, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tagged.person.fullName, style = MaterialTheme.typography.bodyLarge)
                        if (tagged.tag.whole) {
                            Text(
                                text = stringResource(R.string.tagged_here),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    // Picking who a freshly drawn box belongs to.
    if (pending != null) {
        PersonPicker(
            people = state.people,
            photos = photos,
            onPick = { viewModel.tagPending(it) },
            onDismiss = { viewModel.cancelRegion() },
        )
    }

    openTag?.let { tagged ->
        TagActions(
            tagged = tagged,
            canCrop = document?.isImage == true && !tagged.tag.whole,
            onOpenPerson = { openTag = null; onOpenPerson(tagged.person.id) },
            onUseAsPhoto = { viewModel.useAsPhoto(tagged); openTag = null },
            onRemove = { viewModel.untag(tagged.tag.id); openTag = null },
            onDismiss = { openTag = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_document)) },
            text = { Text(document?.label.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.delete_document)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

/**
 * The picture, with every tag drawn over it and a finger-drag making a new one.
 *
 * The image is fitted inside the view, so the drawn area is usually smaller than
 * the box it sits in; [fit] works out where it actually landed and every
 * conversion in either direction goes through that one rectangle.
 */
@Composable
private fun TaggableImage(
    fileName: String,
    files: DocumentStore,
    tagged: List<TaggedPerson>,
    onRegionDrawn: (Float, Float, Float, Float) -> Unit,
    onTagTapped: (TaggedPerson) -> Unit,
) {
    var bitmap by remember(fileName) { mutableStateOf<Bitmap?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var dragFrom by remember { mutableStateOf<Offset?>(null) }
    var dragTo by remember { mutableStateOf<Offset?>(null) }

    val measurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary
    val marked = MaterialTheme.colorScheme.tertiary
    val onAccent = MaterialTheme.colorScheme.onSurface
    val plate = MaterialTheme.colorScheme.surface

    LaunchedEffect(fileName) {
        bitmap = withContext(Dispatchers.IO) { files.decode(fileName, LARGE) }
    }

    val image = bitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .aspectRatio(
                ratio = if (image != null && image.height > 0) {
                    (image.width.toFloat() / image.height).coerceIn(0.5f, 2f)
                } else {
                    1.4f
                },
            )
            .onSizeChanged { viewSize = it },
    ) {
        if (image == null) return@Box

        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        val frame = fit(image.width, image.height, viewSize)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(frame, tagged) {
                    detectTapGestures { tap ->
                        if (frame.width <= 0f) return@detectTapGestures
                        val fx = (tap.x - frame.left) / frame.width
                        val fy = (tap.y - frame.top) / frame.height
                        tagged.firstOrNull { (tag, _) ->
                            !tag.whole &&
                                fx >= tag.left && fx <= tag.right &&
                                fy >= tag.top && fy <= tag.bottom
                        }?.let(onTagTapped)
                    }
                }
                .pointerInput(frame) {
                    detectDragGestures(
                        onDragStart = { dragFrom = it; dragTo = it },
                        onDrag = { change, _ -> dragTo = change.position },
                        onDragCancel = { dragFrom = null; dragTo = null },
                        onDragEnd = {
                            val from = dragFrom
                            val to = dragTo
                            dragFrom = null
                            dragTo = null
                            if (from != null && to != null && frame.width > 0f) {
                                onRegionDrawn(
                                    (from.x - frame.left) / frame.width,
                                    (from.y - frame.top) / frame.height,
                                    (to.x - frame.left) / frame.width,
                                    (to.y - frame.top) / frame.height,
                                )
                            }
                        },
                    )
                },
        ) {
            for ((tag, person) in tagged) {
                if (tag.whole) continue
                val box = Rect(
                    left = frame.left + tag.left * frame.width,
                    top = frame.top + tag.top * frame.height,
                    right = frame.left + tag.right * frame.width,
                    bottom = frame.top + tag.bottom * frame.height,
                )
                drawRect(accent, box.topLeft, box.size, style = Stroke(width = 2f))
                drawBrackets(box, accent, 3f)

                val label = measurer.measure(
                    text = person.displayName,
                    style = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = onAccent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val pad = 4f
                drawRect(
                    color = plate,
                    topLeft = Offset(box.left, box.bottom),
                    size = Size(label.size.width + pad * 2, label.size.height + pad * 2),
                )
                drawText(label, topLeft = Offset(box.left + pad, box.bottom + pad))
            }

            // The box currently under the finger.
            val from = dragFrom
            val to = dragTo
            if (from != null && to != null) {
                val box = Rect(
                    left = minOf(from.x, to.x),
                    top = minOf(from.y, to.y),
                    right = maxOf(from.x, to.x),
                    bottom = maxOf(from.y, to.y),
                )
                drawRect(marked, box.topLeft, box.size, style = Stroke(width = 2f))
            }
        }
    }
}

/** Where a [ContentScale.Fit] image of this shape actually lands inside [view]. */
private fun fit(imageWidth: Int, imageHeight: Int, view: IntSize): Rect {
    if (imageWidth <= 0 || imageHeight <= 0 || view.width <= 0 || view.height <= 0) {
        return Rect(0f, 0f, 0f, 0f)
    }
    val scale = minOf(view.width.toFloat() / imageWidth, view.height.toFloat() / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    val left = (view.width - width) / 2f
    val top = (view.height - height) / 2f
    return Rect(left, top, left + width, top + height)
}

@Composable
private fun NonImage(mimeType: String, originalName: String, size: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = iconFor(mimeType),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(originalName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = listOf(mimeType, readableSize(size)).filter { it.isNotEmpty() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The searchable list of everyone, for putting a name to a box. */
@Composable
private fun PersonPicker(
    people: List<io.github.hamzatadlaoui.socialgraph.data.PersonEntity>,
    photos: PhotoStore,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var term by remember { mutableStateOf("") }
    val shown = remember(term, people) {
        if (term.isBlank()) people else people.filter { it.fullName.contains(term, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.who_is_this)) },
        text = {
            Column {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_people)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.id }) { person ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(person.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Avatar(person.displayName, person.photo, photos, size = 36.dp)
                            Text(person.fullName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun TagActions(
    tagged: TaggedPerson,
    canCrop: Boolean,
    onOpenPerson: () -> Unit,
    onUseAsPhoto: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tagged.person.fullName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenPerson) {
                    Text(stringResource(R.string.open_profile))
                }
                if (canCrop) {
                    TextButton(onClick = onUseAsPhoto) {
                        Text(stringResource(R.string.use_as_photo))
                    }
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove_tag))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

/** Hands the file to whatever else on the phone can read it. */
private fun open(
    context: android.content.Context,
    files: DocumentStore,
    fileName: String?,
    mimeType: String?,
    failure: String,
) {
    if (fileName.isNullOrEmpty()) return
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            files.file(fileName),
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType?.ifEmpty { null } ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }.onFailure {
        if (it is ActivityNotFoundException || it is IllegalArgumentException) {
            android.widget.Toast.makeText(context, failure, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

/** Enough pixels to tag a face in a group photograph without holding the original in memory. */
private const val LARGE = 1400
