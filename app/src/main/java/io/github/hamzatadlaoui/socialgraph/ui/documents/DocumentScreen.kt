package io.github.hamzatadlaoui.socialgraph.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
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
import io.github.hamzatadlaoui.socialgraph.model.Corner
import io.github.hamzatadlaoui.socialgraph.model.CropBox
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
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
                    draft = draft,
                    onBeginRegion = viewModel::beginRegion,
                    onDrawTo = viewModel::drawTo,
                    onMoveBy = viewModel::moveBy,
                    onDragCorner = viewModel::dragCorner,
                    onDragFinished = viewModel::saveDraft,
                    onSelect = viewModel::select,
                    onDeselect = viewModel::clearDraft,
                )

                val open = draft
                if (open != null && open.valid) {
                    val who = state.tagged.firstOrNull { it.tag.id == open.tagId }
                    EditingBar(
                        name = who?.person?.fullName,
                        canCrop = who != null,
                        onName = { picking = true },
                        onUseAsPhoto = { who?.let { viewModel.useAsPhoto(it) } },
                        onRemove = viewModel::removeDraft,
                        onDone = viewModel::clearDraft,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.drag_to_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
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
                OutlinedButton(onClick = { viewModel.tagWhole(); picking = true }) {
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
                        .clickable { onOpenPerson(tagged.person.id) }
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

    // Putting a name to whichever box is open.
    if (picking) {
        PersonPicker(
            people = state.people,
            photos = photos,
            onPick = { picking = false; viewModel.assign(it) },
            onDismiss = { picking = false },
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

/** What the finger took hold of when the drag began. */
private sealed interface Grab {
    data object New : Grab
    data object Move : Grab
    data class Handle(val corner: Corner) : Grab
}

/**
 * The picture, with every tag drawn over it and one of them open for editing.
 *
 * A box is never final: drag inside it to move it, drag a corner to resize it,
 * drag anywhere else to start another. The image is fitted inside the view, so
 * the drawn area is usually smaller than the box it sits in; [fit] works out
 * where it actually landed and every conversion in either direction goes
 * through that one rectangle.
 */
@Composable
private fun TaggableImage(
    fileName: String,
    files: DocumentStore,
    tagged: List<TaggedPerson>,
    draft: DocumentViewModel.Draft?,
    onBeginRegion: (Float, Float, Float, Float) -> Unit,
    onDrawTo: (Float, Float, Float, Float) -> Unit,
    onMoveBy: (Float, Float) -> Unit,
    onDragCorner: (Corner, Float, Float) -> Unit,
    onDragFinished: () -> Unit,
    onSelect: (TaggedPerson) -> Unit,
    onDeselect: () -> Unit,
) {
    var bitmap by remember(fileName) { mutableStateOf<Bitmap?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val measurer = rememberTextMeasurer()
    val accent = MaterialTheme.colorScheme.primary
    val editing = MaterialTheme.colorScheme.tertiary
    val labelColour = MaterialTheme.colorScheme.onSurface
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

        // Where the picture actually landed inside the view, worked out by the
        // tested geometry in model/ rather than here.
        val fitted = CropBox.fitted(image.width, image.height, viewSize.width, viewSize.height)
        val frame = Rect(fitted.left, fitted.top, fitted.right, fitted.bottom)
        val handle = with(LocalDensity.current) { HANDLE.toPx() }
        val reachPx = with(LocalDensity.current) { REACH.toPx() }
        // Touch slop converted once, so the hit-testing stays in fractions.
        val reach = if (frame.width > 0f) reachPx / frame.width else 0f

        val open = draft?.box?.takeIf { viewSize.width > 0 }
        val openRect = open?.let {
            Rect(
                left = frame.left + it.left * frame.width,
                top = frame.top + it.top * frame.height,
                right = frame.left + it.right * frame.width,
                bottom = frame.top + it.bottom * frame.height,
            )
        }

        var grab by remember { mutableStateOf<Grab>(Grab.New) }
        var anchor by remember { mutableStateOf(Offset.Zero) }

        // Read through these inside the gesture handlers rather than capturing
        // them: keying pointerInput on a value that the drag itself changes
        // restarts the detector mid-gesture, which cancels the very drag that
        // caused it - the box would never grow past the point it started at.
        val liveFrame by rememberUpdatedState(frame)
        val liveOpen by rememberUpdatedState(open)
        val liveReach by rememberUpdatedState(reach)
        val liveTagged by rememberUpdatedState(tagged)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val frameNow = liveFrame
                        if (frameNow.width <= 0f) return@detectTapGestures

                        val fx = (tap.x - frameNow.left) / frameNow.width
                        val fy = (tap.y - frameNow.top) / frameNow.height
                        // A tap inside the open box leaves it open; anywhere
                        // else either picks up another tag or puts this one down.
                        if (liveOpen?.contains(fx, fy) == true) return@detectTapGestures

                        val hit = liveTagged.firstOrNull { (tag, _) ->
                            !tag.whole &&
                                fx >= tag.left && fx <= tag.right &&
                                fy >= tag.top && fy <= tag.bottom
                        }
                        if (hit != null) onSelect(hit) else onDeselect()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { at ->
                            val frameNow = liveFrame
                            if (frameNow.width <= 0f) return@detectDragGestures
                            anchor = at
                            val fx = (at.x - frameNow.left) / frameNow.width
                            val fy = (at.y - frameNow.top) / frameNow.height
                            val box = liveOpen
                            val corner = box?.cornerAt(fx, fy, liveReach)
                            grab = when {
                                corner != null -> Grab.Handle(corner)
                                box?.contains(fx, fy) == true -> Grab.Move
                                else -> Grab.New
                            }
                            if (grab == Grab.New) onBeginRegion(fx, fy, fx, fy)
                        },
                        onDrag = { change, delta ->
                            val frameNow = liveFrame
                            if (frameNow.width <= 0f) return@detectDragGestures
                            change.consume()
                            val at = change.position
                            val fx = (at.x - frameNow.left) / frameNow.width
                            val fy = (at.y - frameNow.top) / frameNow.height

                            when (val held = grab) {
                                Grab.New -> onDrawTo(
                                    (anchor.x - frameNow.left) / frameNow.width,
                                    (anchor.y - frameNow.top) / frameNow.height,
                                    fx,
                                    fy,
                                )
                                Grab.Move -> onMoveBy(
                                    delta.x / frameNow.width,
                                    delta.y / frameNow.height,
                                )
                                is Grab.Handle -> onDragCorner(held.corner, fx, fy)
                            }
                        },
                        onDragEnd = { onDragFinished() },
                        onDragCancel = { onDragFinished() },
                    )
                },
        ) {
            for ((tag, person) in tagged) {
                if (tag.whole || tag.id == draft?.tagId) continue
                val box = Rect(
                    left = frame.left + tag.left * frame.width,
                    top = frame.top + tag.top * frame.height,
                    right = frame.left + tag.right * frame.width,
                    bottom = frame.top + tag.bottom * frame.height,
                )
                drawRect(accent, box.topLeft, box.size, style = Stroke(width = 2f))
                drawBrackets(box, accent, 3f)
                drawLabel(measurer, person.displayName, box, plate, labelColour)
            }

            // The one being worked on: amber, with corners to take hold of.
            if (openRect != null) {
                drawRect(editing, openRect.topLeft, openRect.size, style = Stroke(width = 2f))
                for (corner in corners(openRect)) {
                    drawRect(
                        color = editing,
                        topLeft = Offset(corner.x - handle / 2f, corner.y - handle / 2f),
                        size = Size(handle, handle),
                    )
                }
            }
        }
    }
}

private fun corners(box: Rect) = listOf(
    box.topLeft,
    Offset(box.right, box.top),
    Offset(box.left, box.bottom),
    box.bottomRight,
)

private fun near(at: Offset, corner: Offset, reach: Float): Boolean =
    kotlin.math.abs(at.x - corner.x) <= reach && kotlin.math.abs(at.y - corner.y) <= reach

/** The name on a small plate under its box, so an edge cannot swallow it. */
private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    name: String,
    box: Rect,
    plate: Color,
    colour: Color,
) {
    val laid = measurer.measure(
        text = name,
        style = TextStyle(fontFamily = Mono, fontSize = 11.sp, color = colour),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    val pad = 4f
    drawRect(
        color = plate,
        topLeft = Offset(box.left, box.bottom),
        size = Size(laid.size.width + pad * 2, laid.size.height + pad * 2),
    )
    drawText(laid, topLeft = Offset(box.left + pad, box.bottom + pad))
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

/**
 * What can be done with the box currently open. Sits under the picture rather
 * than in a dialog, so the box stays visible and adjustable while it is used.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditingBar(
    name: String?,
    canCrop: Boolean,
    onName: () -> Unit,
    onUseAsPhoto: () -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(
            text = stringResource(
                if (name == null) R.string.drag_to_adjust_new else R.string.drag_to_adjust,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        // Wraps rather than scrolls: these buttons all matter, and one of them
        // sliding off the right-hand edge is how you press the wrong one.
        FlowRow(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (name == null) {
                Button(onClick = onName) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.tag_someone),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                TextButton(onClick = onName) { Text(name) }
                if (canCrop) {
                    TextButton(onClick = onUseAsPhoto) {
                        Text(stringResource(R.string.use_as_photo))
                    }
                }
            }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.remove_tag)) }
            TextButton(onClick = onDone) { Text(stringResource(R.string.done_adjusting)) }
        }
    }
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

/** The corner squares: big enough to see, small enough not to cover the face. */
private val HANDLE = 12.dp

/** How close a finger has to land to count as taking hold of a corner. */
private val REACH = 28.dp
