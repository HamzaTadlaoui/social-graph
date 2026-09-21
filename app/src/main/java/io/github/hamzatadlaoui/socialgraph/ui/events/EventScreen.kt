package io.github.hamzatadlaoui.socialgraph.ui.events

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.EventPhotoEntity
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.ui.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One event, view and edit combined - no separate mode, the same as every
 * field on a person is edited by simply typing into it. Section 4.4-adjacent:
 * the structured counterpart to a free-text fact, section 4.3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(
    viewModel: EventViewModel,
    photos: PhotoStore,
    onOpenPerson: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val attendees by viewModel.attendees.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val eventPhotos by viewModel.photosOnEvent.collectAsStateWithLifecycle()

    var confirmDelete by remember { mutableStateOf(false) }
    var pickingAttendee by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
        viewModel::addPhotos,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.title.ifBlank { stringResource(R.string.untitled_event) },
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
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_event))
                    }
                },
            )
        },
    ) { padding ->
        if (event == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize())
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.dateText,
                onValueChange = viewModel::onDateChange,
                label = { Text(stringResource(R.string.field_date)) },
                supportingText = { Text(stringResource(R.string.field_born_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text(stringResource(R.string.field_location)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.field_description)) },
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(stringResource(R.string.section_attendees))
            for (attendee in attendees) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(attendee.person.id) }
                        .padding(vertical = 4.dp),
                ) {
                    Avatar(attendee.person.fullName, attendee.person.photo, photos, size = 40.dp)
                    Text(attendee.person.fullName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeAttendee(attendee.attendeeId) }) {
                        Icon(Icons.Default.Close, stringResource(R.string.remove_attendee))
                    }
                }
            }
            TextButton(onClick = { pickingAttendee = true }) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.add_attendee), modifier = Modifier.padding(start = 8.dp))
            }

            SectionHeader(stringResource(R.string.section_photos))
            PhotoGrid(
                eventPhotos = eventPhotos,
                photos = photos,
                onAdd = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemove = viewModel::removePhoto,
            )
        }
    }

    if (pickingAttendee) {
        AttendeePicker(
            candidates = candidates,
            photos = photos,
            onPick = { personId ->
                viewModel.addAttendee(personId)
                pickingAttendee = false
            },
            onCreateAndAdd = { name ->
                viewModel.createAndAddAttendee(name)
                pickingAttendee = false
            },
            onDismiss = { pickingAttendee = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    stringResource(
                        R.string.delete_person_title,
                        viewModel.title.ifBlank { stringResource(R.string.untitled_event) },
                    ),
                )
            },
            text = { Text(stringResource(R.string.delete_event_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDeleted) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PhotoGrid(
    eventPhotos: List<EventPhotoEntity>,
    photos: PhotoStore,
    onAdd: () -> Unit,
    onRemove: (EventPhotoEntity) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.heightIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItems(eventPhotos, key = { it.id }) { photo ->
            EventPhotoThumb(photo, photos, onClick = { onRemove(photo) })
        }
        item {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add_photo))
            }
        }
    }
}

@Composable
private fun EventPhotoThumb(photo: EventPhotoEntity, photos: PhotoStore, onClick: () -> Unit) {
    val pixels = with(LocalDensity.current) { 192.dp.roundToPx() }
    var bitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photo.id, pixels) {
        bitmap = withContext(Dispatchers.IO) { photos.decode(photo.fileName, pixels) }
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = stringResource(R.string.remove_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Who to mark present - search among everyone not already attending, or
 * type a name nobody answers to and create them on the spot, the same
 * "type it and they exist" flow adding a relationship already offers.
 */
@Composable
private fun AttendeePicker(
    candidates: List<PersonEntity>,
    photos: PhotoStore,
    onPick: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var term by remember { mutableStateOf("") }
    val shown = remember(term, candidates) {
        if (term.isBlank()) candidates else candidates.filter { it.fullName.contains(term, ignoreCase = true) }
    }
    val exactMatch = shown.any { it.fullName.equals(term.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_attendee)) },
        text = {
            Column {
                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.which_person)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (term.isNotBlank() && !exactMatch) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCreateAndAdd(term) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Icon(Icons.Default.PersonAdd, null)
                                Text(
                                    text = stringResource(R.string.create_and_add_attendee, term.trim()),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
