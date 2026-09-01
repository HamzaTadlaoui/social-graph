package io.github.hamzatadlaoui.socialgraph.ui.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.ui.Avatar

/**
 * Adding someone should take seconds (section 17), so the name field is the
 * only one that matters, it takes focus straight away, and Save is live the
 * moment it has anything in it. Everything below it can stay empty forever.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditScreen(
    viewModel: PersonEditViewModel,
    photos: PhotoStore,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val form = viewModel.form
    var confirmingDelete by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        viewModel::onPhotoPicked,
    )

    LaunchedEffect(Unit) {
        if (viewModel.isNew) nameFocus.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isNew) R.string.add_person else R.string.edit_person,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete))
                        }
                    }
                    TextButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = form.canSave,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.clickable {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Avatar(
                    name = form.displayName,
                    photo = form.photo,
                    photos = photos,
                    size = 72.dp,
                )
                Text(
                    text = stringResource(R.string.choose_photo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Field(
                value = form.displayName,
                onValueChange = { text -> viewModel.update { it.copy(displayName = text) } },
                label = R.string.field_name,
                modifier = Modifier.focusRequester(nameFocus),
            )
            Field(
                value = form.lastName,
                onValueChange = { text -> viewModel.update { it.copy(lastName = text) } },
                label = R.string.field_last_name,
            )
            Field(
                value = form.nickname,
                onValueChange = { text -> viewModel.update { it.copy(nickname = text) } },
                label = R.string.field_nickname,
            )
            Field(
                value = form.pronouns,
                onValueChange = { text -> viewModel.update { it.copy(pronouns = text) } },
                label = R.string.field_pronouns,
            )
            Field(
                value = form.born,
                onValueChange = { text -> viewModel.update { it.copy(born = text) } },
                label = R.string.field_born,
                supporting = R.string.field_born_hint,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Switch(
                    checked = form.isMe,
                    onCheckedChange = { on -> viewModel.update { it.copy(isMe = on) } },
                )
                Column {
                    Text(stringResource(R.string.this_is_me))
                    Text(
                        text = stringResource(R.string.this_is_me_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Field(
                value = form.notes,
                onValueChange = { text -> viewModel.update { it.copy(notes = text) } },
                label = R.string.field_notes,
                singleLine = false,
            )
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.delete_person_title, form.displayName)) },
            text = { Text(stringResource(R.string.delete_person_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDeleted) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    supporting: Int? = null,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        supportingText = supporting?.let { { Text(stringResource(it)) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = modifier.fillMaxWidth(),
    )
}
