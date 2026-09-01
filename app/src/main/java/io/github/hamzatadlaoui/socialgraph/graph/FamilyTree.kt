package io.github.hamzatadlaoui.socialgraph.graph

import io.github.hamzatadlaoui.socialgraph.model.RelationshipType

/**
 * Where one person sits in the tree: [generation] counts down from the person
 * it was built around (their parents are -1, their children +1), and [column]
 * places them left to right within it, centred on zero.
 */
data class TreePlace(val id: String, val generation: Int, val column: Float)

/**
 * A family tree is not a second database - it is this, computed from the same
 * ties the graph uses (section 3.4). [couples] and [descents] are the lines to
 * draw: beside, and downwards.
 */
data class FamilyTree(
    val rootId: String,
    val places: List<TreePlace>,
    val couples: List<Pair<String, String>>,
    val descents: List<Pair<String, String>>,
) {
    val generations: IntRange
        get() = (places.minOfOrNull { it.generation } ?: 0)..(places.maxOfOrNull { it.generation } ?: 0)
}

/**
 * Builds the tree around [rootId], reaching [up] generations towards the
 * grandparents and [down] towards the grandchildren.
 *
 * The rules are section 5.4's, and no more than that: parents above, children
 * below, partners beside, siblings on the same row.
 */
fun familyTree(graph: PeopleGraph, rootId: String, up: Int = 2, down: Int = 2): FamilyTree {
    val generation = linkedMapOf(rootId to 0)
    val queue = ArrayDeque(listOf(rootId))

    // Walk outwards along family ties only, carrying the generation with us.
    while (queue.isNotEmpty()) {
        val personId = queue.removeFirst()
        val here = generation.getValue(personId)

        for (edge in graph.neighbours(personId)) {
            val step = when (edge.type) {
                RelationshipType.PARENT_OF -> 1
                RelationshipType.CHILD_OF -> -1
                RelationshipType.SIBLING_OF,
                RelationshipType.PARTNER_OF,
                RelationshipType.EX_PARTNER_OF,
                -> 0
                else -> continue
            }
            val there = here + step
            // Stop at the edge of the requested view rather than walking the
            // whole database and throwing most of it away.
            if (there < -up || there > down) continue
            if (edge.toId in generation) continue

            generation[edge.toId] = there
            queue += edge.toId
        }
    }

    val places = place(generation, graph, rootId)

    val inTree = generation.keys
    val couples = mutableSetOf<Pair<String, String>>()
    val descents = mutableSetOf<Pair<String, String>>()

    for (personId in inTree) {
        for (edge in graph.neighbours(personId)) {
            if (edge.toId !in inTree) continue
            when (edge.type) {
                RelationshipType.PARTNER_OF, RelationshipType.EX_PARTNER_OF ->
                    // One line per couple, whichever way round it was entered.
                    couples += listOf(edge.fromId, edge.toId).sorted().let { it[0] to it[1] }
                RelationshipType.PARENT_OF -> descents += edge.fromId to edge.toId
                else -> Unit
            }
        }
    }

    return FamilyTree(rootId, places, couples.toList(), descents.toList())
}

/**
 * Turns "who is in which generation" into where each of them goes.
 *
 * Two rules, in this order. A couple is one block, so partners are never split
 * up by someone else's family. And a set of children hangs under the middle of
 * its parents - both of them, when both are known - so that reading down the
 * page follows descent rather than the order people happened to be typed in.
 *
 * Positions are in columns of one person each. The tree is then slid sideways
 * so the person it was built around sits at zero.
 */
private fun place(
    generation: Map<String, Int>,
    graph: PeopleGraph,
    rootId: String,
): List<TreePlace> {
    val inTree = generation.keys

    val parentsOf = inTree.associateWith { id ->
        graph.neighbours(id)
            .filter { it.type == RelationshipType.CHILD_OF && it.toId in inTree }
            .map { it.toId }
            .distinct()
            .sorted()
    }
    val childrenOf = inTree.associateWith { id ->
        graph.neighbours(id)
            .filter { it.type == RelationshipType.PARENT_OF && it.toId in inTree }
            .map { it.toId }
            .distinct()
    }

    val rows = generation.entries
        .groupBy({ it.value }, { it.key })
        .mapValues { (_, ids) -> partnersTogether(ids, graph) }
    val orderedRows = rows.keys.sorted()

    val column = mutableMapOf<String, Float>()

    // Downwards first: each row is laid out under the one above it.
    for (row in orderedRows) {
        val units = unitsIn(rows.getValue(row), graph)

        // Where each block would like to sit: the middle of its parents.
        val wanted = units.map { unit ->
            val above = unit.flatMap { parentsOf[it].orEmpty() }.distinct().mapNotNull { column[it] }
            if (above.isEmpty()) null else above.average().toFloat()
        }

        // Blocks whose parents are known go where their parents are, in that
        // order; anyone left over is added on the end rather than cutting in.
        val order = units.indices.sortedWith(
            compareBy({ wanted[it] == null }, { wanted[it] ?: 0f }),
        )

        var cursor: Float? = null
        for (index in order) {
            val unit = units[index]
            val width = unit.size.toFloat()
            val preferred = wanted[index]?.minus((width - 1f) / 2f)
            val start = when {
                cursor == null -> preferred ?: 0f
                preferred == null -> cursor
                else -> maxOf(preferred, cursor)
            }
            unit.forEachIndexed { offset, id -> column[id] = start + offset }
            cursor = start + width - 1f + SPACING
        }
    }

    // Then upwards, so a couple ends up over the middle of their children
    // rather than wherever their own generation happened to start.
    for (row in orderedRows.reversed()) {
        val units = unitsIn(rows.getValue(row), graph)
        for (unit in units) {
            val below = unit.flatMap { childrenOf[it].orEmpty() }.distinct().mapNotNull { column[it] }
            if (below.isEmpty()) continue

            val middle = below.average().toFloat()
            val start = middle - (unit.size - 1f) / 2f
            unit.forEachIndexed { offset, id -> column[id] = start + offset }
        }
        spread(rows.getValue(row), column)
    }

    // The person the tree was built around sits at zero, whatever happened above.
    val shift = column[rootId] ?: 0f
    return generation.map { (id, row) -> TreePlace(id, row, (column[id] ?: 0f) - shift) }
}

/** Couples are one block; everyone else is a block of one. */
private fun unitsIn(ids: List<String>, graph: PeopleGraph): List<List<String>> {
    val remaining = ids.toMutableList()
    val units = mutableListOf<List<String>>()

    while (remaining.isNotEmpty()) {
        val personId = remaining.removeAt(0)
        val partner = graph.neighbours(personId)
            .firstOrNull {
                (it.type == RelationshipType.PARTNER_OF || it.type == RelationshipType.EX_PARTNER_OF) &&
                    it.toId in remaining
            }
            ?.toId

        if (partner == null) {
            units += listOf(personId)
        } else {
            remaining.remove(partner)
            units += listOf(personId, partner)
        }
    }
    return units
}

/**
 * Pushes a row apart until nobody is sitting on anybody. Centring a couple over
 * their children can walk them into the family next door; this walks them back
 * out, leftmost first, without changing anyone's order.
 */
private fun spread(ids: List<String>, column: MutableMap<String, Float>) {
    val ordered = ids.sortedBy { column[it] ?: 0f }
    var last: Float? = null
    for (id in ordered) {
        val here = column[id] ?: continue
        val floor = last?.plus(1f)
        if (floor != null && here < floor) column[id] = floor
        last = column[id]
    }
}

/** One clear column between one family and the next. */
private const val SPACING = 1.6f

/** Keeps couples next to each other, so the line between them is a short one. */
private fun partnersTogether(ids: List<String>, graph: PeopleGraph): List<String> {
    val remaining = ids.toMutableList()
    val ordered = mutableListOf<String>()

    while (remaining.isNotEmpty()) {
        val personId = remaining.removeAt(0)
        ordered += personId

        val partner = graph.neighbours(personId)
            .firstOrNull {
                (it.type == RelationshipType.PARTNER_OF || it.type == RelationshipType.EX_PARTNER_OF) &&
                    it.toId in remaining
            }
            ?.toId

        if (partner != null) {
            remaining.remove(partner)
            ordered += partner
        }
    }
    return ordered
}
