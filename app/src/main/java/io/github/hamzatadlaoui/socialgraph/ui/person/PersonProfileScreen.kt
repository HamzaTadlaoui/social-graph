package io.github.hamzatadlaoui.socialgraph.ui.person

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.DocumentEntity
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore
import io.github.hamzatadlaoui.socialgraph.data.EventEntity
import io.github.hamzatadlaoui.socialgraph.data.FactEntity
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.graph.Kinship
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.ui.Avatar
import io.github.hamzatadlaoui.socialgraph.ui.documents.FileThumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The tabs a profile splits into - one rail, one kind of thing per icon, the
 * way the game this app's look is drawn after keeps a person's identity,
 * their contacts and their free-text notes as separate tabs rather than
 * folding everything into one list. [Facts] covers Background, Personality,
 * Activities and anything the person has typed a category name for
 * themselves; everything else is a single fixed tab.
 */
sealed interface ProfileTab {
    data object PersonalInfo : ProfileTab
    data object Relationships : ProfileTab
    data object Events : ProfileTab
    data class Facts(val category: String) : ProfileTab
    data object Notes : ProfileTab
    data object AppearsIn : ProfileTab
}

/**
 * The dossier: one person, everything known about them, and every tie they
 * have. Section 5.2 - the screen the whole app exists to open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonProfileScreen(
    viewModel: PersonProfileViewModel,
    photos: PhotoStore,
    files: DocumentStore,
    onOpenDocument: (String) -> Unit,
    onEdit: () -> Unit,
    onAddRelationship: () -> Unit,
    onOpenPerson: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onAddEvent: () -> Unit,
    onBack: () -> Unit,
) {
    val person by viewModel.person.collectAsStateWithLifecycle()
    val ties by viewModel.ties.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val implied by viewModel.implied.collectAsStateWithLifecycle()
    val facts by viewModel.facts.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val taggedPhotos by viewModel.taggedPhotos.collectAsStateWithLifecycle()
    val months = stringArrayResource(R.array.months).toList()
    var selectedTab by remember { mutableStateOf<ProfileTab>(ProfileTab.Relationships) }
    var confirmTarget by remember { mutableStateOf<ImpliedTie?>(null) }
    var pickingPhoto by remember { mutableStateOf(false) }
    var addingCategory by remember { mutableStateOf(false) }

    val customCategories = remember(facts) {
        facts.map { it.category }.distinct().filterNot { id -> FactCategory.byId(id) != null }
    }

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
            // Only makes sense while looking at ties or events - floating it
            // over Personal info or Notes would offer an action that does
            // nothing useful there.
            when (selectedTab) {
                ProfileTab.Relationships -> ExtendedFloatingActionButton(
                    onClick = onAddRelationship,
                    icon = { Icon(Icons.Default.PersonAdd, null) },
                    text = { Text(stringResource(R.string.add_relationship)) },
                )
                ProfileTab.Events -> ExtendedFloatingActionButton(
                    onClick = onAddEvent,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.add_event)) },
                )
                else -> Unit
            }
        },
    ) { padding ->
        val subject = person ?: return@Scaffold

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Header(
                person = subject,
                photos = photos,
                hasTaggedPhotos = taggedPhotos.isNotEmpty(),
                onChoosePhoto = { pickingPhoto = true },
            )
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxSize()) {
                ProfileRail(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    customCategories = customCategories,
                    onAddCategory = { addingCategory = true },
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (val tab = selectedTab) {
                        ProfileTab.PersonalInfo -> PersonalInfoTab(subject, months)
                        ProfileTab.Relationships -> RelationshipsTab(
                            ties = ties,
                            implied = implied,
                            photos = photos,
                            onOpenPerson = onOpenPerson,
                            onUnlink = viewModel::unlink,
                            onConfirmImplied = { implied2, label ->
                                when (implied2.kinship) {
                                    Kinship.PARTNERS_CHILD,
                                    Kinship.PARENTS_PARTNER,
                                    Kinship.SIBLINGS_PARENT,
                                    Kinship.SIBLING,
                                    Kinship.HALF_SIBLING,
                                    -> confirmTarget = implied2
                                    else -> viewModel.confirmImplied(implied2, label)
                                }
                            },
                        )
                        ProfileTab.Events -> EventsTab(events, months, onOpenEvent = onOpenEvent)
                        is ProfileTab.Facts -> if (tab.category == FactCategory.PERSONALITY.id) {
                            PersonalityTabContent(
                                facts = facts.filter { it.category == tab.category },
                                onAddFact = { text -> viewModel.addFact(tab.category, text) },
                                onDeleteFact = viewModel::deleteFact,
                            )
                        } else {
                            FactsTabContent(
                                category = tab.category,
                                facts = facts.filter { it.category == tab.category },
                                onAddFact = viewModel::addFact,
                                onDeleteFact = viewModel::deleteFact,
                            )
                        }
                        ProfileTab.Notes -> NotesTab(subject.notes)
                        ProfileTab.AppearsIn -> AppearsInTab(documents, files, onOpenDocument)
                    }
                }
            }
        }

        // "Partner's child" and "parent's partner" collapse two distinct
        // people into one tie once confirmed - who is whose child stops
        // being ambiguous the moment it is recorded - so those two ask
        // outright rather than recording on the first tap.
        confirmTarget?.let { target ->
            val question = when (target.kinship) {
                Kinship.PARTNERS_CHILD -> stringResource(R.string.confirm_partners_child, target.other.displayName)
                Kinship.PARENTS_PARTNER, Kinship.SIBLINGS_PARENT ->
                    stringResource(R.string.confirm_parents_partner, target.other.displayName)
                Kinship.SIBLING, Kinship.HALF_SIBLING ->
                    stringResource(R.string.confirm_sibling, target.other.displayName)
                else -> ""
            }
            val label = stringResource(target.kinship.label())
            AlertDialog(
                onDismissRequest = { confirmTarget = null },
                title = { Text(stringResource(R.string.follow_up_title)) },
                text = { Text(question) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.confirmImplied(target, label)
                        confirmTarget = null
                    }) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmTarget = null }) { Text(stringResource(R.string.no)) }
                },
            )
        }

        if (pickingPhoto) {
            TaggedPhotoPicker(
                photos = taggedPhotos,
                files = files,
                onPick = { taggedPhoto ->
                    viewModel.useTaggedPhotoAsProfile(taggedPhoto)
                    pickingPhoto = false
                },
                onDismiss = { pickingPhoto = false },
            )
        }

        if (addingCategory) {
            var categoryName by remember { mutableStateOf("") }
            var factText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { addingCategory = false },
                title = { Text(stringResource(R.string.new_category)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text(stringResource(R.string.category_name)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = factText,
                            onValueChange = { factText = it },
                            label = { Text(stringResource(R.string.add_fact)) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addFact(categoryName, factText)
                            selectedTab = ProfileTab.Facts(categoryName.trim())
                            addingCategory = false
                        },
                        enabled = categoryName.isNotBlank() && factText.isNotBlank(),
                    ) { Text(stringResource(R.string.add_category)) }
                },
                dismissButton = {
                    TextButton(onClick = { addingCategory = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

/** The left-hand rail: one icon per tab, every kind of information a peer of every other. */
@Composable
private fun ProfileRail(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    customCategories: List<String>,
    onAddCategory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryTab(
            icon = Icons.Default.Badge,
            label = stringResource(R.string.tab_personal_info),
            selected = selected == ProfileTab.PersonalInfo,
            onClick = { onSelect(ProfileTab.PersonalInfo) },
        )
        CategoryTab(
            icon = Icons.Default.Groups,
            label = stringResource(R.string.tab_relationships),
            selected = selected == ProfileTab.Relationships,
            onClick = { onSelect(ProfileTab.Relationships) },
        )
        CategoryTab(
            icon = Icons.Default.Event,
            label = stringResource(R.string.tab_events),
            selected = selected == ProfileTab.Events,
            onClick = { onSelect(ProfileTab.Events) },
        )
        for (category in FactCategory.entries) {
            CategoryTab(
                icon = category.icon,
                label = stringResource(category.label),
                selected = selected == ProfileTab.Facts(category.id),
                onClick = { onSelect(ProfileTab.Facts(category.id)) },
            )
        }
        for (category in customCategories) {
            CategoryTab(
                icon = CUSTOM_CATEGORY_ICON,
                label = category,
                selected = selected == ProfileTab.Facts(category),
                onClick = { onSelect(ProfileTab.Facts(category)) },
            )
        }
        IconButton(onClick = onAddCategory) {
            Icon(Icons.Default.Add, stringResource(R.string.new_category))
        }
        CategoryTab(
            icon = Icons.AutoMirrored.Filled.Notes,
            label = stringResource(R.string.field_notes),
            selected = selected == ProfileTab.Notes,
            onClick = { onSelect(ProfileTab.Notes) },
        )
        CategoryTab(
            icon = Icons.Default.PermMedia,
            label = stringResource(R.string.appears_in),
            selected = selected == ProfileTab.AppearsIn,
            onClick = { onSelect(ProfileTab.AppearsIn) },
        )
    }
}

@Composable
private fun Header(
    person: PersonEntity,
    photos: PhotoStore,
    hasTaggedPhotos: Boolean,
    onChoosePhoto: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Box {
            Avatar(person.fullName, person.photo, photos, size = 88.dp)
            // Only offered once there is something to pick from - otherwise
            // it is a button that always opens onto an empty list.
            if (hasTaggedPhotos) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
                ) {
                    IconButton(onClick = onChoosePhoto, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.choose_from_tagged_photos),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        Text(person.fullName, style = MaterialTheme.typography.headlineSmall)
    }
}

/**
 * A person's identity, read-only - name through occupation. Editing stays
 * where every other field on this person is already edited: the pencil in
 * the top bar, into the same form this reads back from.
 */
@Composable
private fun PersonalInfoTab(person: PersonEntity, months: List<String>) {
    val unknown = stringResource(R.string.unknown)
    val notSet = stringResource(R.string.not_set)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        InfoRow(stringResource(R.string.field_name), person.fullName)
        InfoRow(stringResource(R.string.field_nickname), person.nickname.ifBlank { notSet }, muted = person.nickname.isBlank())
        InfoRow(stringResource(R.string.field_pronouns), person.pronouns.ifBlank { notSet }, muted = person.pronouns.isBlank())
        InfoRow(stringResource(R.string.field_born), person.birth.format(months, unknown), muted = !person.birth.isKnown)
        InfoRow(stringResource(R.string.field_died), person.death.format(months, unknown), muted = !person.death.isKnown)
        InfoRow(stringResource(R.string.field_address), person.address.ifBlank { notSet }, muted = person.address.isBlank())
        InfoRow(stringResource(R.string.field_occupation), person.occupation.ifBlank { notSet }, muted = person.occupation.isBlank())
    }
}

@Composable
private fun InfoRow(label: String, value: String, muted: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        )
    }
}

/** Every tie recorded outright, grouped the way a person would say them, then what follows from them. */
@Composable
private fun RelationshipsTab(
    ties: List<Tie>,
    implied: List<ImpliedTie>,
    photos: PhotoStore,
    onOpenPerson: (String) -> Unit,
    onUnlink: (Tie) -> Unit,
    onConfirmImplied: (ImpliedTie, String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val grouped = ties.groupBy { it.relationship.type }
        for (type in relationshipSectionOrder) {
            val group = grouped[type].orEmpty()
            if (group.isEmpty()) continue

            item {
                SectionTitle(title = stringResource(type.sectionTitle()), count = group.size)
            }
            items(group, key = { it.relationship.id }) { tie ->
                TieRow(
                    tie = tie,
                    photos = photos,
                    onOpen = { onOpenPerson(tie.other.id) },
                    onUnlink = { onUnlink(tie) },
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

        // Ties the app worked out rather than being told. Kept apart from
        // the recorded ones and worded as a consequence, so the page never
        // pretends to know something nobody entered.
        if (implied.isNotEmpty()) {
            item {
                SectionTitle(title = stringResource(R.string.implied_ties), count = implied.size)
            }
            items(implied, key = { it.other.id }) { tie ->
                val kinshipLabel = stringResource(tie.kinship.label())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(tie.other.id) }
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Avatar(tie.other.displayName, tie.other.photo, photos, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tie.other.fullName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = listOfNotNull(
                                kinshipLabel,
                                tie.throughName?.let { stringResource(R.string.implied_through, it) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onConfirmImplied(tie, kinshipLabel) }) {
                        Icon(Icons.Default.Check, stringResource(R.string.make_official))
                    }
                }
            }
        }

        // Room for the floating button to sit without covering the last row.
        item { Column(Modifier.padding(bottom = 88.dp)) {} }
    }
}

@Composable
private fun NotesTab(notes: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = notes.ifBlank { stringResource(R.string.no_notes_yet) },
            style = MaterialTheme.typography.bodyMedium,
            color = if (notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        )
    }
}

/** Every event this person was marked present at - read-only, fed from the Events tab, section 4.4. */
@Composable
private fun EventsTab(events: List<EventEntity>, months: List<String>, onOpenEvent: (String) -> Unit) {
    if (events.isEmpty()) {
        Text(
            text = stringResource(R.string.no_events_for_person),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events, key = { it.id }) { event ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEvent(event.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text(
                        text = event.title.ifBlank { stringResource(R.string.untitled_event) },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val subtitle = listOfNotNull(
                        event.date.takeIf { it.isKnown }?.format(months),
                        event.location.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearsInTab(
    documents: List<DocumentEntity>,
    files: DocumentStore,
    onOpenDocument: (String) -> Unit,
) {
    if (documents.isEmpty()) {
        Text(
            text = stringResource(R.string.not_tagged_in_any_photos),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(documents, key = { it.id }) { document ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDocument(document.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FileThumb(document.fileName, document.mimeType, files, size = 40.dp)
                Text(
                    text = document.label.ifBlank { stringResource(R.string.untitled_document) },
                    style = MaterialTheme.typography.bodyLarge,
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

/** One fact category's content: its list, plus the inline row that adds to it. */
@Composable
private fun FactsTabContent(
    category: String,
    facts: List<FactEntity>,
    onAddFact: (category: String, text: String) -> Unit,
    onDeleteFact: (id: String) -> Unit,
) {
    var draft by remember(category) { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        for (fact in facts) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                Text(
                    text = fact.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDeleteFact(fact.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.delete_fact),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.fact_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onAddFact(category, draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.add_fact))
            }
        }
    }
}

/**
 * Personality traits as chips rather than a line-per-trait list - a trait
 * is a word or two, not a sentence, so tapping one away and typing a new one
 * on a fresh line fits the content better than the plain fact list the other
 * categories use.
 */
@Composable
private fun PersonalityTabContent(
    facts: List<FactEntity>,
    onAddFact: (text: String) -> Unit,
    onDeleteFact: (id: String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (fact in facts) {
                InputChip(
                    selected = false,
                    onClick = { onDeleteFact(fact.id) },
                    label = { Text(fact.text) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.delete_fact),
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.fact_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onAddFact(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.add_fact))
            }
        }
    }
}

@Composable
private fun CategoryTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Every photo this person is tagged in, cropped to their part of it exactly
 * the way choosing one will save it - a picker should never promise a crop
 * that picking does not actually produce.
 */
@Composable
private fun TaggedPhotoPicker(
    photos: List<TaggedPhoto>,
    files: DocumentStore,
    onPick: (TaggedPhoto) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.pick_a_photo), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (photos.isEmpty()) {
                    Text(
                        text = stringResource(R.string.not_tagged_in_any_photos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 360.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gridItems(photos, key = { it.tag.id }) { taggedPhoto ->
                            TaggedPhotoThumb(
                                taggedPhoto = taggedPhoto,
                                files = files,
                                onClick = { onPick(taggedPhoto) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaggedPhotoThumb(taggedPhoto: TaggedPhoto, files: DocumentStore, onClick: () -> Unit) {
    val pixels = with(LocalDensity.current) { 192.dp.roundToPx() }
    var bitmap by remember(taggedPhoto.tag.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(taggedPhoto.tag.id, pixels) {
        bitmap = withContext(Dispatchers.IO) {
            files.decode(taggedPhoto.document.fileName, pixels)
                ?.let { full -> PhotoStore.previewCrop(full, taggedPhoto.tag) }
        }
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
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
