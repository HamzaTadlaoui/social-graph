package io.github.hamzatadlaoui.socialgraph.ui.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.data.DocumentStore

/**
 * The shelf: everything filed, newest first, with a picture of it where there
 * can be one. Any kind of file is allowed - the picker is opened on every MIME
 * type - because a birth certificate and a voice note are the same sort of
 * thing to this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    files: DocumentStore,
    onOpenDocument: (String) -> Unit,
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val failed = stringResource(R.string.document_failed)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        viewModel.add(uri) { saved ->
            // A file that cannot be copied in has to say so: silence here reads
            // as the button simply not working.
            if (saved == null) {
                if (uri != null) {
                    android.widget.Toast
                        .makeText(context, failed, android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                onOpenDocument(saved.id)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_files)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Default.Add, stringResource(R.string.add_document))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text(stringResource(R.string.search_documents)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (documents.isEmpty()) {
                Empty(searching = viewModel.query.isNotBlank())
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(documents, key = { it.id }) { document ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDocument(document.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        FileThumb(document.fileName, document.mimeType, files, size = 48.dp)
                        Column {
                            Text(
                                text = document.label.ifBlank {
                                    stringResource(R.string.untitled_document)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val size = readableSize(document.sizeBytes)
                            if (size.isNotEmpty()) {
                                Text(
                                    text = size,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
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
            text = stringResource(
                if (searching) R.string.no_documents_found else R.string.no_documents_yet,
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
