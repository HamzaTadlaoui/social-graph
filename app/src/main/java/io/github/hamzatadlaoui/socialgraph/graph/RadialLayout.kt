package io.github.hamzatadlaoui.socialgraph.graph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A place on the drawing, in units where the outermost ring sits at radius 1. */
data class Point(val x: Float, val y: Float)

/**
 * Rings around whoever the view is centred on: the person themselves in the
 * middle, everyone one hop out on the first ring, two hops on the next.
 *
 * Chosen over a force-directed tangle because the distance is the point - "two
 * hops away" is the thing the user came to see, and a spring simulation hides
 * it. Each person is given a slice of the circle, and their own neighbours are
 * placed inside that slice, which keeps families together and lines apart.
 *
 * Deterministic: the same network always lays out the same way, so the graph
 * does not rearrange itself under the user's finger between visits.
 */
fun radialLayout(network: EgoNetwork): Map<String, Point> {
    val maxDepth = network.depth.values.maxOrNull() ?: 0
    if (maxDepth == 0) return mapOf(network.rootId to Point(0f, 0f))

    val children = network.reachedThrough.entries
        .groupBy({ it.value }, { it.key })
        .mapValues { (_, ids) -> ids.sorted() }

    val places = mutableMapOf(network.rootId to Point(0f, 0f))

    // Each subtree gets a wedge in proportion to how many people it ends up
    // holding, so a large family does not squeeze onto one line.
    fun place(personId: String, from: Double, to: Double) {
        val kids = children[personId].orEmpty()
        if (kids.isEmpty()) return

        val weights = kids.map { leaves(it, children) }
        val total = weights.sum().toDouble()
        var cursor = from

        kids.forEachIndexed { index, childId ->
            val slice = (to - from) * (weights[index] / total)
            val middle = cursor + slice / 2
            val radius = (network.depth[childId] ?: 1).toFloat() / maxDepth

            places[childId] = Point(
                x = (radius * cos(middle)).toFloat(),
                y = (radius * sin(middle)).toFloat(),
            )
            place(childId, cursor, cursor + slice)
            cursor += slice
        }
    }

    place(network.rootId, -PI, PI)
    return places
}

/** How many people hang off this one, counting themselves when nobody does. */
private fun leaves(personId: String, children: Map<String, List<String>>): Int {
    val kids = children[personId].orEmpty()
    if (kids.isEmpty()) return 1
    return kids.sumOf { leaves(it, children) }
}
