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

    val rows = generation.entries.groupBy({ it.value }, { it.key })
        .mapValues { (_, ids) -> partnersTogether(ids, graph) }

    val places = rows.flatMap { (row, ids) ->
        val middle = (ids.size - 1) / 2f
        ids.mapIndexed { index, id -> TreePlace(id, row, index - middle) }
    }

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
