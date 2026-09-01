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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.ui.Avatar

/**
 * The dossier: one person, everything known about them, and every tie they
 * have. Section 5.2 - the screen the whole app exists to open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonProfileScreen(
    viewModel: PersonProfileViewModel,
    photos: PhotoStore,
    onEdit: () -> Unit,
    onAddRelationship: () -> Unit,
    onOpenPerson: (String) -> Unit,
    onBack: () -> Unit,
) {
    val person by viewModel.person.collectAsStateWithLifecycle()
    val ties by viewModel.ties.collectAsStateWithLifecycle()
    val months = stringArrayResource(R.array.months).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.fullName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit_person))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRelationship,
                icon = { Icon(Icons.Default.PersonAdd, null) },
                text = { Text(stringResource(R.string.add_relationship)) },
            )
        },
    ) { padding ->
        val subject = person ?: return@Scaffold

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item { Header(subject, photos, months) }

            // Grouped the way a person would say them: family, then everyone else.
            val grouped = ties.groupBy { it.relationship.type }
            for (type in relationshipSectionOrder) {
                val group = grouped[type].orEmpty()
                if (group.isEmpty()) continue

                item {
                    SectionTitle(
                        title = stringResource(type.sectionTitle()),
                        count = group.size,
                    )
                }
                items(group, key = { it.relationship.id }) { tie ->
                    TieRow(
                        tie = tie,
                        photos = photos,
                        onOpen = { onOpenPerson(tie.other.id) },
                        onUnlink = { viewModel.unlink(tie) },
                    )
                }
            }

            if (subject.notes.isNotBlank()) {
                item { SectionTitle(stringResource(R.string.field_notes)) }
                item {
                    Text(
                        text = subject.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            if (ties.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_relationships_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Room for the floating button to sit without covering the last row.
            item { Column(Modifier.padding(bottom = 88.dp)) {} }
        }
    }
}

@Composable
private fun Header(person: PersonEntity, photos: PhotoStore, months: List<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Avatar(person.fullName, person.photo, photos, size = 88.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(person.fullName, style = MaterialTheme.typography.headlineSmall)
            if (person.nickname.isNotBlank()) {
                Text(
                    text = "“${person.nickname}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (person.birth.isKnown) {
                Text(
                    text = stringResource(
                        R.string.born_on,
                        person.birth.format(months, stringResource(R.string.unknown)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (person.pronouns.isNotBlank()) {
                Text(
                    text = person.pronouns,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int? = null) {
    Column {
        HorizontalDivider()
        Text(
            text = if (count == null) title else "$title ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun TieRow(
    tie: Tie,
    photos: PhotoStore,
    onOpen: () -> Unit,
    onUnlink: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Avatar(tie.other.fullName, tie.other.photo, photos, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(tie.other.fullName, style = MaterialTheme.typography.bodyLarge)
            val label = tie.relationship.customLabel
            if (tie.relationship.type == RelationshipType.CUSTOM && label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onUnlink) {
            Icon(Icons.Default.LinkOff, stringResource(R.string.remove_relationship))
        }
    }
}
