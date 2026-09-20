package io.github.hamzatadlaoui.socialgraph.graph

import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import kotlin.math.ceil

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
     *
     * [depth] is spent like a budget rather than counted like a strict number
     * of steps: a hop through [RelationshipType.closeness] cheap family costs
     * less of it than a hop through a weaker tie, so the same budget reaches
     * two steps of parents and siblings but only one step through a friend or
     * coworker. A person is placed on whichever ring their cheapest way in
     * rounds up to, so a tie costing 1.5 still lands one ring further out
     * than a plain family hop, not folded into the same one.
     *
     * Ties for the cheapest way to reach someone are broken by which edge was
     * offered first, the same order [neighbours] already returns them in, so
     * the same network always lays out the same way.
     */
    fun egoNetwork(
        rootId: String,
        depth: Int,
        include: (RelationshipType) -> Boolean = { true },
    ): EgoNetwork {
        val budget = depth.coerceAtLeast(0).toDouble()
        val cost = linkedMapOf(rootId to 0.0)
        val parents = mutableMapOf<String, String>()
        val kept = LinkedHashMap<String, Edge>()

        var sequence = 0
        val order = compareBy<Triple<String, Double, Int>>({ it.second }, { it.third })
        val frontier = java.util.PriorityQueue(order)
        frontier += Triple(rootId, 0.0, sequence++)

        while (true) {
            val (personId, atCost, _) = frontier.poll() ?: break
            if (atCost > cost.getValue(personId)) continue // a cheaper way in was already found

            for (edge in neighbours(personId)) {
                if (!include(edge.type)) continue
                kept.putIfAbsent(edge.pairId, edge)

                val toCost = atCost + edge.type.closeness
                if (toCost > budget) continue
                val known = cost[edge.toId]
                if (known == null || toCost < known) {
                    cost[edge.toId] = toCost
                    parents[edge.toId] = personId
                    frontier += Triple(edge.toId, toCost, sequence++)
                }
            }
        }

        val distance = cost.mapValues { (_, spent) -> ceil(spent).toInt() }
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
