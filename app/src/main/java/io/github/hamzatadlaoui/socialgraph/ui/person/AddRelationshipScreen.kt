package io.github.hamzatadlaoui.socialgraph.ui.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.ui.Avatar

/**
 * Pick what the tie is, then who it is with. The other person does not have to
 * exist yet - typing a name nobody answers to offers to create them, which is
 * what lets a whole family be entered without leaving the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRelationshipScreen(
    viewModel: AddRelationshipViewModel,
    subjectName: String,
    photos: PhotoStore,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_relationship)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.relationship_subject, subjectName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = stringResource(viewModel.type.pickerLabel()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.relationship_kind)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = true,
                    ).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RelationshipType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.pickerLabel())) },
                            onClick = {
                                viewModel.onTypeChange(type)
                                expanded = false
                            },
                        )
                    }
                }
            }

            if (viewModel.type == RelationshipType.CUSTOM) {
                OutlinedTextField(
                    value = viewModel.customLabel,
                    onValueChange = viewModel::onCustomLabelChange,
                    label = { Text(stringResource(R.string.relationship_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.which_person)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            LazyColumn {
                val exactMatch = matches.any { it.fullName.equals(query.trim(), ignoreCase = true) }
                if (query.isNotBlank() && !exactMatch) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.createAndLink(query, onDone) }
                                .padding(16.dp),
                        ) {
                            Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.create_and_link, query.trim()),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                items(matches, key = { it.id }) { person ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.linkTo(person.id, onDone) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Avatar(person.fullName, person.photo, photos, size = 40.dp)
                        Text(person.fullName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
