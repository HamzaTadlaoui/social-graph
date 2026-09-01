package io.github.hamzatadlaoui.socialgraph.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.ui.drawBrackets
import io.github.hamzatadlaoui.socialgraph.ui.drawGrid
import io.github.hamzatadlaoui.socialgraph.ui.theme.Mono
import io.github.hamzatadlaoui.socialgraph.R
import io.github.hamzatadlaoui.socialgraph.graph.Point
import kotlin.math.hypot
import kotlin.math.min

/**
 * The network as a picture (section 5.3): whoever is at the centre, everyone
 * within the chosen number of hops, and the ties between them. Pan with a
 * finger, pinch to zoom, tap a face to do something with it.
 *
 * Drawn by hand on a canvas rather than with a graph library - it is a few
 * dozen lines, and it keeps the dependency list short enough for F-Droid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    onOpenPerson: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val measurer = rememberTextMeasurer()

    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    var filterOpen by remember { mutableStateOf(false) }

    val surface = MaterialTheme.colorScheme.surface
    val plateColour = MaterialTheme.colorScheme.surfaceVariant
    val nodeColour = MaterialTheme.colorScheme.primary
    val rootColour = MaterialTheme.colorScheme.tertiary
    val edgeColour = MaterialTheme.colorScheme.outline
    val gridColour = MaterialTheme.colorScheme.outlineVariant
    val labelColour = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.root?.fullName ?: stringResource(R.string.tab_graph)) },
                actions = {
                    Box {
                        IconButton(onClick = { filterOpen = true }) {
                            Icon(Icons.Default.FilterList, stringResource(R.string.filter_ties))
                        }
                        DropdownMenu(filterOpen, onDismissRequest = { filterOpen = false }) {
                            TieFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.label)) },
                                    onClick = {
                                        viewModel.onFilterChange(option)
                                        filterOpen = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Scrollable: the labels are short, but a large font scale or a long
            // translation should push the row sideways rather than crush a chip.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                listOf(1, 2, 3).forEach { hops ->
                    FilterChip(
                        selected = viewModel.depth == hops,
                        onClick = { viewModel.onDepthChange(hops) },
                        label = { Text(stringResource(R.string.hops, hops)) },
                    )
                }
            }

            if (state.isEmpty) {
                Empty()
                return@Column
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panChange, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.4f, 4f)
                                pan += panChange
                            }
                        }
                        .pointerInput(state.nodes) {
                            detectTapGestures { tap ->
                                val radius = min(size.width, size.height) / 2f * 0.78f
                                val centre = Offset(size.width / 2f, size.height / 2f)
                                selected = state.nodes
                                    .minByOrNull { node ->
                                        val at = node.at.toScreen(centre, radius, scale, pan)
                                        hypot(tap.x - at.x, tap.y - at.y)
                                    }
                                    ?.takeIf { node ->
                                        val at = node.at.toScreen(centre, radius, scale, pan)
                                        hypot(tap.x - at.x, tap.y - at.y) < 48.dp.toPx() * scale
                                    }
                                    ?.person?.id
                            }
                        },
                ) {
                    val radius = min(size.width, size.height) / 2f * 0.78f
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    val places = state.nodes.associate {
                        it.person.id to it.at.toScreen(centre, radius, scale, pan)
                    }

                    drawGrid(gridColour, 24.dp.toPx() * scale, pan)

                    for (edge in state.edges) {
                        val from = places[edge.fromId] ?: continue
                        val to = places[edge.toId] ?: continue
                        drawLine(edgeColour, from, to, strokeWidth = 1.5f * scale)
                    }

                    for (node in state.nodes) {
                        val at = places[node.person.id] ?: continue
                        val isRoot = node.person.id == state.root?.id
                        val isSelected = node.person.id == selected
                        val half = (if (isRoot) 9f else 7f) * scale
                        val marker = Rect(at - Offset(half, half), Size(half * 2f, half * 2f))

                        // A square marker, punched out of the edges running under it.
                        drawRect(surface, marker.inflate(2f * scale).topLeft, marker.inflate(2f * scale).size)
                        drawRect(
                            color = if (isRoot) rootColour else nodeColour,
                            topLeft = marker.topLeft,
                            size = marker.size,
                        )
                        // The centre of the network wears a reticle, so it stays
                        // findable once the drawing has been panned around.
                        if (isRoot) {
                            drawBrackets(marker, rootColour, 1.5f * scale, gap = 7f * scale)
                        }

                        val plate = drawPersonName(
                            measurer = measurer,
                            name = node.person.displayName,
                            at = at,
                            below = half + 7f * scale,
                            background = plateColour,
                            border = if (isRoot) rootColour else edgeColour,
                            colour = labelColour,
                            scale = scale,
                        )
                        if (isSelected) {
                            drawBrackets(
                                Rect(marker.left, marker.top, marker.right, plate.bottom),
                                nodeColour,
                                2f * scale,
                                gap = 5f * scale,
                            )
                        }
                    }
                }

                val person = state.nodes.firstOrNull { it.person.id == selected }?.person
                if (person != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(person.fullName, style = MaterialTheme.typography.titleMedium)
                            Row {
                                TextButton(onClick = { onOpenPerson(person.id) }) {
                                    Text(stringResource(R.string.open_profile))
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.centreOn(person.id)
                                        selected = null
                                        scale = 1f
                                        pan = Offset.Zero
                                    },
                                ) {
                                    Text(stringResource(R.string.centre_here))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Unit-circle place to a pixel on screen, after the user's pan and zoom. */
private fun Point.toScreen(centre: Offset, radius: Float, scale: Float, pan: Offset) = Offset(
    x = centre.x + x * radius * scale + pan.x,
    y = centre.y + y * radius * scale + pan.y,
)

/**
 * The name on a small plate under its marker, so it stays readable where an
 * edge passes behind it. Returns the plate so the caller can bracket it.
 */
private fun DrawScope.drawPersonName(
    measurer: TextMeasurer,
    name: String,
    at: Offset,
    below: Float,
    background: Color,
    border: Color,
    colour: Color,
    scale: Float,
): Rect {
    val style = TextStyle(
        fontFamily = Mono,
        fontSize = 11.sp * scale.coerceIn(0.8f, 1.6f),
        color = colour,
    )
    val laid = measurer.measure(
        text = name,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = (110.dp.toPx() * scale).toInt().coerceAtLeast(1)),
    )

    val padX = 5f * scale
    val padY = 3f * scale
    val plate = Rect(
        offset = Offset(
            x = at.x - laid.size.width / 2f - padX,
            y = at.y + below - padY,
        ),
        size = Size(laid.size.width + padX * 2f, laid.size.height + padY * 2f),
    )

    drawRect(background, plate.topLeft, plate.size)
    drawRect(border, plate.topLeft, plate.size, style = Stroke(width = 1f * scale))
    drawText(laid, topLeft = Offset(plate.left + padX, plate.top + padY))
    return plate
}

@Composable
private fun Empty() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.graph_needs_people),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
