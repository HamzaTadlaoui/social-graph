package io.github.hamzatadlaoui.socialgraph.ui.events

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.EventEntity

/**
 * Every occasion recorded, newest first - the one place an event is made,
 * the same way the document shelf is the one place a file is filed. A
 * person's profile only ever shows what already exists here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    onOpenEvent: (String) -> Unit,
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val months = stringArrayResource(R.array.months).toList()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_events)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createDraft { onOpenEvent(it.id) } }) {
                Icon(Icons.Default.Add, stringResource(R.string.add_event))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(stringResource(R.string.search_events)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (events.isEmpty()) {
                Empty(searching = viewModel.query.isNotBlank())
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events, key = { it.id }) { event ->
                    EventRow(event, months, onClick = { onOpenEvent(event.id) })
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: EventEntity, months: List<String>, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(
                text = event.title.ifBlank { stringResource(R.string.untitled_event) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

@Composable
private fun Empty(searching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (searching) R.string.no_documents_found else R.string.no_events_yet),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
