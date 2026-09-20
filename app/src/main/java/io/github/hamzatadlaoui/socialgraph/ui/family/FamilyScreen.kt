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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hamzatadlaoui.socialgraph.data.PersonEntity
import io.github.hamzatadlaoui.socialgraph.data.PhotoStore
import io.github.hamzatadlaoui.socialgraph.ui.drawBrackets
import io.github.hamzatadlaoui.socialgraph.ui.drawGrid
import io.github.hamzatadlaoui.socialgraph.ui.drawPortrait
import io.github.hamzatadlaoui.socialgraph.ui.initials
import io.github.hamzatadlaoui.socialgraph.ui.rememberPortraits
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

private fun Density.treeMetrics(showPhotos: Boolean): TreeMetrics {
    // With photos on, the cell holds the square plus its own name plate
    // hanging below it, not a name squeezed inside a taller box; the gap
    // between one generation's boxes and the next stays fixed either way, so
    // the bus lines in drawDescents keep the same clearance to route through.
    val boxHeight = (if (showPhotos) TREE_PORTRAIT + 58.dp else 46.dp).toPx()
    return TreeMetrics(
        columnWidth = 116.dp.toPx(),
        rowHeight = boxHeight + 50.dp.toPx(),
        boxWidth = 92.dp.toPx(),
        boxHeight = boxHeight,
        grid = 24.dp.toPx(),
    )
}

/** Big enough to actually read as a face, not a thumbnail standing in for one. */
private val TREE_PORTRAIT = 64.dp

private const val MIN_GENERATIONS = 1
private const val MAX_GENERATIONS = 6

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
    photos: PhotoStore,
    onOpenPerson: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val measurer = rememberTextMeasurer()

    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<String?>(null) }
    // Until the user moves it themselves, the tree frames itself.
    var moved by remember { mutableStateOf(false) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var showPhotos by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    // Faces on the tree, as the network graph draws them. Decoded once at a
    // fixed size regardless of zoom, and not decoded at all while photos are
    // switched off.
    val portraitPx = with(density) { TREE_PORTRAIT.roundToPx() }
    val portraits = rememberPortraits(
        photos = photos,
        fileNames = if (showPhotos) state.nodes.map { it.person.photo } else emptyList(),
        sizePx = portraitPx,
    )

    val surface = MaterialTheme.colorScheme.surface
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
            // translation should push the row sideways rather than crush a control.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                // A stepper rather than a row of fixed choices, so the depth is one
                // dial rather than a fixed set of tabs that would need a new one
                // adding every time a wider range turned out to be worth showing.
                IconButton(
                    onClick = {
                        viewModel.onGenerationsChange(
                            (viewModel.generations - 1).coerceAtLeast(MIN_GENERATIONS),
                        )
                    },
                    enabled = viewModel.generations > MIN_GENERATIONS,
                ) {
                    Icon(Icons.Default.Remove, stringResource(R.string.fewer_generations))
                }
                Text(
                    text = stringResource(R.string.generations, viewModel.generations),
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(
                    onClick = {
                        viewModel.onGenerationsChange(
                            (viewModel.generations + 1).coerceAtMost(MAX_GENERATIONS),
                        )
                    },
                    enabled = viewModel.generations < MAX_GENERATIONS,
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.more_generations))
                }
                FilterChip(
                    selected = showPhotos,
                    onClick = { showPhotos = !showPhotos },
                    label = { Text(stringResource(R.string.show_photos)) },
                )
            }

            if (state.isEmpty) {
                Empty()
                return@Column
            }

            // A family is drawn to be seen whole: work out what it takes to fit
            // and start there, rather than dropping the reader at one hundred
            // per cent with the grandparents somewhere off the right-hand edge.
            LaunchedEffect(state.nodes, viewSize, viewModel.generations, showPhotos) {
                if (moved || viewSize.width == 0 || state.nodes.isEmpty()) return@LaunchedEffect
                val metrics = with(density) { treeMetrics(showPhotos) }
                val columns = state.nodes.map { it.place.column }
                val generations = state.nodes.map { it.place.generation }

                val spread = (columns.max() - columns.min()) * metrics.columnWidth + metrics.boxWidth
                val depth =
                    (generations.max() - generations.min()) * metrics.rowHeight + metrics.boxHeight
                val margin = with(density) { 48.dp.toPx() }

                scale = minOf(
                    (viewSize.width - margin) / spread,
                    (viewSize.height - margin) / depth,
                    1f,
                ).coerceIn(0.35f, 1f)

                // Slide the middle of the tree to the middle of the view.
                val midColumn = (columns.max() + columns.min()) / 2f
                val midGeneration = (generations.max() + generations.min()) / 2f
                pan = Offset(
                    x = -midColumn * metrics.columnWidth * scale,
                    y = -midGeneration * metrics.rowHeight * scale,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewSize = it }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panChange, zoom, _ ->
                                moved = true
                                scale = (scale * zoom).coerceIn(0.4f, 3f)
                                pan += panChange
                            }
                        }
                        .pointerInput(state.nodes, showPhotos) {
                            val metrics = treeMetrics(showPhotos)
                            detectTapGestures { tap ->
                                val centre = Offset(size.width / 2f, size.height / 2f)
                                selected = state.nodes.firstOrNull { node ->
                                    node.boxAt(centre, scale, pan, metrics).contains(tap)
                                }?.person?.id
                            }
                        },
                ) {
                    val metrics = treeMetrics(showPhotos)
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    val boxes = state.nodes.associate {
                        it.person.id to it.boxAt(centre, scale, pan, metrics)
                    }
                    // Lines connect to the square that is actually drawn, not to the
                    // full cell it is laid out in - with photos on, that cell has
                    // empty space below the square for the name plate, and a line
                    // anchored to the cell's own centre or bottom would float clear
                    // of anything visible.
                    val photoSide = TREE_PORTRAIT.toPx() * scale
                    val anchors = if (showPhotos) {
                        boxes.mapValues { (_, box) -> photoFrame(box, photoSide) }
                    } else {
                        boxes
                    }

                    drawGrid(gridColour, metrics.grid * scale, pan)

                    // Partners are joined side to side.
                    for ((one, other) in state.couples) {
                        val a = anchors[one] ?: continue
                        val b = anchors[other] ?: continue
                        val (left, right) = if (a.center.x <= b.center.x) a to b else b to a
                        drawLine(
                            edge,
                            Offset(left.right, left.center.y),
                            Offset(right.left, right.center.y),
                            strokeWidth = 2f * scale,
                        )
                    }

                    drawDescents(state.descents, anchors, edge, scale)

                    for (node in state.nodes) {
                        val box = boxes[node.person.id] ?: continue
                        val isRoot = node.person.id == state.root?.id
                        val borderColour = if (isRoot) accent else edge

                        if (showPhotos) {
                            drawPhotoNode(
                                measurer = measurer,
                                person = node.person,
                                box = box,
                                portrait = portraits[node.person.photo],
                                isRoot = isRoot,
                                surfaceColour = surface,
                                plateColour = plate,
                                borderColour = borderColour,
                                nameColour = labelColour,
                                yearColour = dimColour,
                                scale = scale,
                            )
                        } else {
                            drawRect(
                                color = if (isRoot) rootPlate else plate,
                                topLeft = box.topLeft,
                                size = box.size,
                            )
                            drawRect(
                                color = borderColour,
                                topLeft = box.topLeft,
                                size = box.size,
                                style = Stroke(width = (if (isRoot) 2f else 1f) * scale),
                            )
                            drawNameOnlyPlate(
                                measurer = measurer,
                                person = node.person,
                                box = box,
                                nameColour = labelColour,
                                yearColour = dimColour,
                                scale = scale,
                            )
                        }

                        if (node.person.id == selected) {
                            drawBrackets(box, accent, 2f * scale, gap = 5f * scale)
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
                                        viewModel.rootOn(person.id)
                                        selected = null
                                        moved = false
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

/** The photo square within a cell: centred horizontally, flush with its top. */
private fun photoFrame(box: Rect, side: Float): Rect =
    Rect(Offset(box.center.x - side / 2f, box.top), Size(side, side))

/**
 * A person as a face on the tree: a square photograph - or their initials,
 * where there is none yet - sitting on its own above the row, with a small
 * name plate hanging below it rather than a name squeezed inside the same
 * box as the photo. The same idea the network graph draws nodes with,
 * carried over so a person looks like the same person in both views.
 */
private fun DrawScope.drawPhotoNode(
    measurer: TextMeasurer,
    person: PersonEntity,
    box: Rect,
    portrait: ImageBitmap?,
    isRoot: Boolean,
    surfaceColour: Color,
    plateColour: Color,
    borderColour: Color,
    nameColour: Color,
    yearColour: Color,
    scale: Float,
) {
    val side = TREE_PORTRAIT.toPx() * scale
    val frame = photoFrame(box, side)

    // A backing square first, so an edge running underneath does not show
    // through a photograph with transparency - the same trick the network
    // graph uses.
    drawRect(surfaceColour, frame.topLeft, frame.size)
    if (portrait != null) {
        drawPortrait(portrait, frame.left, frame.top, side)
    } else {
        drawRect(plateColour, frame.topLeft, frame.size)
        val mark = measurer.measure(
            text = initials(person.displayName),
            style = TextStyle(
                fontFamily = Mono,
                fontSize = 15.sp * scale.coerceIn(0.8f, 1.6f),
                color = borderColour,
            ),
            maxLines = 1,
        )
        drawText(
            mark,
            topLeft = Offset(
                frame.center.x - mark.size.width / 2f,
                frame.center.y - mark.size.height / 2f,
            ),
        )
    }
    drawRect(
        color = borderColour,
        topLeft = frame.topLeft,
        size = frame.size,
        style = Stroke(width = (if (isRoot) 2.5f else 1.5f) * scale),
    )

    drawNamePlate(
        measurer = measurer,
        person = person,
        centerX = frame.center.x,
        top = frame.bottom + 6f * scale,
        nameColour = nameColour,
        yearColour = yearColour,
        background = plateColour,
        border = borderColour,
        scale = scale,
    )
}

/**
 * A small plate fit tightly to the text it holds, the way the network graph
 * labels a node - not stretched to the width of a box nothing else is
 * drawing there anymore.
 */
private fun DrawScope.drawNamePlate(
    measurer: TextMeasurer,
    person: PersonEntity,
    centerX: Float,
    top: Float,
    nameColour: Color,
    yearColour: Color,
    background: Color,
    border: Color,
    scale: Float,
) {
    val zoom = scale.coerceIn(0.8f, 1.6f)
    val maxWidth = (120.dp.toPx() * scale).toInt().coerceAtLeast(1)

    val name = measurer.measure(
        text = person.displayName,
        style = TextStyle(fontFamily = Mono, fontSize = 11.sp * zoom, color = nameColour),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxWidth),
    )
    val years = lifespan(person).takeIf { it.isNotEmpty() }?.let {
        measurer.measure(
            text = it,
            style = TextStyle(fontFamily = Mono, fontSize = 9.sp * zoom, color = yearColour),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxWidth),
        )
    }

    val gap = 2f * scale
    val padX = 5f * scale
    val padY = 3f * scale
    val textWidth = maxOf(name.size.width, years?.size?.width ?: 0)
    val textHeight = name.size.height + (years?.let { it.size.height + gap } ?: 0f)

    val plate = Rect(
        offset = Offset(centerX - textWidth / 2f - padX, top),
        size = Size(textWidth + padX * 2f, textHeight + padY * 2f),
    )

    drawRect(background, plate.topLeft, plate.size)
    drawRect(border, plate.topLeft, plate.size, style = Stroke(width = 1f * scale))

    var y = plate.top + padY
    drawText(name, topLeft = Offset(centerX - name.size.width / 2f, y))
    if (years != null) {
        y += name.size.height + gap
        drawText(years, topLeft = Offset(centerX - years.size.width / 2f, y))
    }
}

/** The original plain plate: a name, and the years underneath it, centred in the box. */
private fun DrawScope.drawNameOnlyPlate(
    measurer: TextMeasurer,
    person: PersonEntity,
    box: Rect,
    nameColour: Color,
    yearColour: Color,
    scale: Float,
) {
    val inner = (box.width - 8f * scale).toInt().coerceAtLeast(1)
    val zoom = scale.coerceIn(0.7f, 1.5f)
    val gap = 2f * scale

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
