package io.github.hamzatadlaoui.socialgraph.graph

import io.github.hamzatadlaoui.socialgraph.model.RelationshipType

/**
 * One tie, as the graph sees it. [pairId] is shared by the two rows that are
 * the same tie from either end, which is how an edge drawn once stays drawn
 * once even though the database holds both directions.
 */
data class Edge(
    val pairId: String,
    val fromId: String,
    val toId: String,
    val type: RelationshipType,
)

/**
 * A slice of the database around one person: who is in it, how far out they
 * are, and the ties between them.
 */
data class EgoNetwork(
    val rootId: String,
    val depth: Map<String, Int>,
    val edges: List<Edge>,
    /** The tree the ring layout hangs on: who each person was first reached through. */
    val reachedThrough: Map<String, String> = emptyMap(),
) {
    val ids: Set<String> get() = depth.keys
}

/**
 * The people database seen as a graph, with no idea that Room or Android exist.
 *
 * Every question the views ask - who is next to this person, who sits within
 * two hops, how are these two connected - is answered here, so all of it can be
 * tested on a laptop.
 */
class PeopleGraph(edges: List<Edge>) {

    /** Both directions are stored, so this is already symmetric. */
    private val adjacency: Map<String, List<Edge>> = edges.groupBy { it.fromId }

    fun neighbours(personId: String): List<Edge> = adjacency[personId].orEmpty()

    /**
     * Everyone within [depth] hops of [rootId], keeping only the ties [include]
     * accepts. Edges come back once each, not twice.
     */
    fun egoNetwork(
        rootId: String,
        depth: Int,
        include: (RelationshipType) -> Boolean = { true },
    ): EgoNetwork {
        val distance = linkedMapOf(rootId to 0)
        val parents = mutableMapOf<String, String>()
        val kept = LinkedHashMap<String, Edge>()
        var frontier = listOf(rootId)

        repeat(depth.coerceAtLeast(0)) {
            val next = mutableListOf<String>()
            for (personId in frontier) {
                for (edge in neighbours(personId)) {
                    if (!include(edge.type)) continue
                    kept.putIfAbsent(edge.pairId, edge)
                    if (edge.toId !in distance) {
                        distance[edge.toId] = (distance[personId] ?: 0) + 1
                        parents[edge.toId] = personId
                        next += edge.toId
                    }
                }
            }
            frontier = next
        }

        // An edge is only worth drawing when both of its ends made the cut.
        val edges = kept.values.filter { it.fromId in distance && it.toId in distance }
        return EgoNetwork(rootId, distance, edges, parents)
    }

    /**
     * The shortest chain of ties from one person to another, or an empty list
     * when they are not connected at all. Section 5.5 - "how is this person
     * related to me?" is a breadth-first search and nothing more.
     */
    fun shortestPath(fromId: String, toId: String): List<Edge> {
        if (fromId == toId) return emptyList()

        val cameBy = mutableMapOf<String, Edge>()
        val seen = mutableSetOf(fromId)
        val queue = ArrayDeque(listOf(fromId))

        while (queue.isNotEmpty()) {
            val personId = queue.removeFirst()
            for (edge in neighbours(personId)) {
                if (!seen.add(edge.toId)) continue
                cameBy[edge.toId] = edge
                if (edge.toId == toId) return retrace(cameBy, fromId, toId)
                queue += edge.toId
            }
        }
        return emptyList()
    }

    private fun retrace(cameBy: Map<String, Edge>, fromId: String, toId: String): List<Edge> {
        val path = ArrayDeque<Edge>()
        var at = toId
        while (at != fromId) {
            val edge = cameBy[at] ?: return emptyList()
            path.addFirst(edge)
            at = edge.fromId
        }
        return path.toList()
    }
}
