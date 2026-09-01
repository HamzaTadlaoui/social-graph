package io.github.hamzatadlaoui.socialgraph.ui.family

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.ui.drawBrackets
import io.github.hamzatadlaoui.socialgraph.ui.drawGrid
import io.github.hamzatadlaoui.socialgraph.ui.theme.Mono
import io.github.hamzatadlaoui.socialgraph.R

/**
 * How big a tree is, in pixels on this particular screen. Kept in one place and
 * derived from the display density, because the plates have to be large enough
 * to hold a name at the body text size - guessing in raw pixels is how names
 * end up spilling out of their boxes.
 */
private data class TreeMetrics(
    val columnWidth: Float,
    val rowHeight: Float,
    val boxWidth: Float,
    val boxHeight: Float,
    val grid: Float,
)

private fun Density.treeMetrics() = TreeMetrics(
    columnWidth = 104.dp.toPx(),
    rowHeight = 96.dp.toPx(),
    boxWidth = 92.dp.toPx(),
    boxHeight = 46.dp.toPx(),
    grid = 24.dp.toPx(),
)

/**
 * The traditional view (section 5.4): parents above, partners beside, children
 * below, and any person in it can become the one it is drawn around.
 *
 * Descent is drawn the way a genealogy chart draws it - straight down from the
 * couple, along a horizontal bus, then straight down into each child - rather
 * than as diagonals fanning out, which stop being readable past two children.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    onOpenPerson: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val measurer = rememberTextMeasurer()

    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }

    val plate = MaterialTheme.colorScheme.surfaceVariant
    val rootPlate = MaterialTheme.colorScheme.primaryContainer
    val edge = MaterialTheme.colorScheme.outline
    val gridColour = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val labelColour = MaterialTheme.colorScheme.onSurface
    val dimColour = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.root?.fullName ?: stringResource(R.string.tab_family)) },
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
                listOf(1, 2, 3).forEach { generations ->
                    FilterChip(
                        selected = viewModel.generations == generations,
                        onClick = { viewModel.onGenerationsChange(generations) },
                        label = { Text(stringResource(R.string.generations, generations)) },
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
                                scale = (scale * zoom).coerceIn(0.4f, 3f)
                                pan += panChange
                            }
                        }
                        .pointerInput(state.nodes) {
                            val metrics = treeMetrics()
                            detectTapGestures { tap ->
                                val centre = Offset(size.width / 2f, size.height / 2f)
                                selected = state.nodes.firstOrNull { node ->
                                    node.boxAt(centre, scale, pan, metrics).contains(tap)
                                }?.person?.id
                            }
                        },
                ) {
                    val metrics = treeMetrics()
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    val boxes = state.nodes.associate {
                        it.person.id to it.boxAt(centre, scale, pan, metrics)
                    }

                    drawGrid(gridColour, metrics.grid * scale, pan)

                    // Partners are joined side to side.
                    for ((one, other) in state.couples) {
                        val a = boxes[one] ?: continue
                        val b = boxes[other] ?: continue
                        val (left, right) = if (a.center.x <= b.center.x) a to b else b to a
                        drawLine(
                            edge,
                            Offset(left.right, left.center.y),
                            Offset(right.left, right.center.y),
                            strokeWidth = 2f * scale,
                        )
                    }

                    drawDescents(state.descents, boxes, edge, scale)

                    for (node in state.nodes) {
                        val box = boxes[node.person.id] ?: continue
                        val isRoot = node.person.id == state.root?.id

                        drawRect(
                            color = if (isRoot) rootPlate else plate,
                            topLeft = box.topLeft,
                            size = box.size,
                        )
                        drawRect(
                            color = if (isRoot) accent else edge,
                            topLeft = box.topLeft,
                            size = box.size,
                            style = Stroke(width = (if (isRoot) 2f else 1f) * scale),
                        )
                        if (node.person.id == selected) {
                            drawBrackets(box, accent, 2f * scale, gap = 5f * scale)
                        }
                        drawPlate(measurer, node.person, box, labelColour, dimColour, scale)
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
                                        viewModel.rootOn(person.id)
                                        selected = null
                                        scale = 1f
                                        pan = Offset.Zero
                                    },
                                ) {
                                    Text(stringResource(R.string.tree_from_here))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One drop per set of parents: down from between them, along a bus, then down
 * into each child. Children sharing both parents therefore share one line, and
 * a second marriage gets its own.
 */
private fun DrawScope.drawDescents(
    descents: List<Pair<String, String>>,
    boxes: Map<String, Rect>,
    colour: Color,
    scale: Float,
) {
    val parentsOfChild = descents
        .groupBy({ it.second }, { it.first })
        .mapValues { (_, parents) -> parents.distinct().sorted() }

    // Siblings with the same parents hang off a single bus.
    val families = parentsOfChild.entries.groupBy({ it.value }, { it.key })

    for ((parentIds, childIds) in families) {
        val parentBoxes = parentIds.mapNotNull { boxes[it] }
        val childBoxes = childIds.mapNotNull { boxes[it] }
        if (parentBoxes.isEmpty() || childBoxes.isEmpty()) continue

        val fromX = parentBoxes.map { it.center.x }.average().toFloat()
        val fromY = parentBoxes.maxOf { it.bottom }
        val topOfChildren = childBoxes.minOf { it.top }
        // The bus sits midway down the gap, which is what keeps every generation
        // reading as a row rather than a scatter.
        val busY = fromY + (topOfChildren - fromY) / 2f
        val stroke = 2f * scale

        drawLine(colour, Offset(fromX, fromY), Offset(fromX, busY), strokeWidth = stroke)

        val left = minOf(fromX, childBoxes.minOf { it.center.x })
        val right = maxOf(fromX, childBoxes.maxOf { it.center.x })
        if (right - left > 0.5f) {
            drawLine(colour, Offset(left, busY), Offset(right, busY), strokeWidth = stroke)
        }

        for (child in childBoxes) {
            drawLine(
                colour,
                Offset(child.center.x, busY),
                Offset(child.center.x, child.top),
                strokeWidth = stroke,
            )
        }
    }
}

/** Where this person's box lands on screen, after the user's pan and zoom. */
private fun TreeNode.boxAt(centre: Offset, scale: Float, pan: Offset, metrics: TreeMetrics): Rect {
    val x = centre.x + place.column * metrics.columnWidth * scale + pan.x
    val y = centre.y + place.generation * metrics.rowHeight * scale + pan.y
    val size = Size(metrics.boxWidth * scale, metrics.boxHeight * scale)
    return Rect(Offset(x - size.width / 2f, y - size.height / 2f), size)
}

/** The name, and the years underneath it when they are known at all. */
private fun DrawScope.drawPlate(
    measurer: TextMeasurer,
    person: PersonEntity,
    box: Rect,
    nameColour: Color,
    yearColour: Color,
    scale: Float,
) {
    val inner = (box.width - 8f * scale).toInt().coerceAtLeast(1)
    val zoom = scale.coerceIn(0.7f, 1.5f)

    val name = measurer.measure(
        text = person.displayName,
        style = TextStyle(fontFamily = Mono, fontSize = 11.sp * zoom, color = nameColour),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = inner),
    )
    val years = lifespan(person).takeIf { it.isNotEmpty() }?.let {
        measurer.measure(
            text = it,
            style = TextStyle(fontFamily = Mono, fontSize = 9.sp * zoom, color = yearColour),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = inner),
        )
    }

    val gap = 2f * scale
    val total = name.size.height + (years?.let { it.size.height + gap } ?: 0f)
    var y = box.center.y - total / 2f

    drawText(name, topLeft = Offset(box.center.x - name.size.width / 2f, y))
    if (years != null) {
        y += name.size.height + gap
        drawText(years, topLeft = Offset(box.center.x - years.size.width / 2f, y))
    }
}

/** "c.1974", "1974-2010", or nothing at all when neither year is recorded. */
private fun lifespan(person: PersonEntity): String {
    fun year(date: io.github.hamzatadlaoui.socialgraph.model.FuzzyDate): String? =
        date.year?.let { (if (date.approximate) "c." else "") + it }

    val born = year(person.birth)
    val died = year(person.death)
    return when {
        born != null && died != null -> "$born-$died"
        born != null -> born
        died != null -> "-$died"
        else -> ""
    }
}

@Composable
private fun Empty() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.family_needs_people),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
