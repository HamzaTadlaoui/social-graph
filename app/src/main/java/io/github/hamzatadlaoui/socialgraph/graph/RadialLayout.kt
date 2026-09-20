package io.github.hamzatadlaoui.socialgraph.graph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A place on the drawing, in units where an uncrowded outermost ring sits at
 * radius 1. A ring busy enough to need more room than that is allowed to
 * land past 1 rather than being squeezed back under it - see [radialLayout].
 */
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

    // A ring's radius grows with how many people actually sit on it, not
    // just with how many hops out it is. A ring fixed at depth/maxDepth put
    // a crowded first ring - everyone directly tied to the centre - on a
    // radius meant for a couple of people, however generously their wedges
    // were split, and no wedge split fixes a ring that is simply too small
    // to hold them without overlapping. Up to MIN_RING_STEP, a ring is
    // spaced the way it always was, so "two hops away" still reads as
    // roughly twice as far as "one hop away" in the ordinary case; past
    // that, the ring is crowded, and CROWD_WEIGHT pushes it out faster than
    // one unit of radius per extra person.
    val countByDepth = network.depth.values.groupingBy { it }.eachCount()
    val ringRadius = DoubleArray(maxDepth + 1)
    for (d in 1..maxDepth) {
        val crowd = (countByDepth[d] ?: 1).toDouble()
        val excess = (crowd - MIN_RING_STEP).coerceAtLeast(0.0)
        ringRadius[d] = ringRadius[d - 1] + MIN_RING_STEP + excess * CROWD_WEIGHT
    }
    // Divided by a fixed baseline - what the outermost ring would need if
    // nothing were crowded - rather than by this network's own outermost
    // ring. Dividing by the actual total let a busy ring further out dilute
    // every ring closer in back towards the centre, which is exactly
    // backwards: how far out the first ring sits should depend on how
    // crowded the first ring is, not on how crowded the third ring happens
    // to be. That was the bug behind a busy family drawing like an atom,
    // everyone packed at the middle, instead of like the same family with
    // more people further out. A ring crowded enough to need more than its
    // fixed share now lands past radius 1 rather than being squeezed back
    // under it - out past the default view, same as it would be squeezed
    // into it either way, but reachable by pinching out instead of
    // unreadable on arrival.
    val baseline = maxDepth * MIN_RING_STEP

    val places = mutableMapOf(network.rootId to Point(0f, 0f))

    // Each subtree gets a wedge in proportion to how many people it ends up
    // holding, so a large family does not squeeze onto one line - but a
    // childless person still keeps a guaranteed slice of their own, an even
    // share of half the wedge, rather than one proportional only to a
    // subtree they don't have. Splitting purely by weight let one sibling
    // with a large family of their own crowd a childless sibling down to
    // almost nothing, which is exactly backwards: the childless one is a
    // single box needing a fair slice, not a subtree needing a large one.
    fun place(personId: String, from: Double, to: Double) {
        val kids = children[personId].orEmpty()
        if (kids.isEmpty()) return

        val weights = kids.map { leaves(it, children) }
        val total = weights.sum().toDouble()
        val width = to - from
        val evenShare = width * MIN_CHILD_SHARE / kids.size
        val bonusWidth = width * (1 - MIN_CHILD_SHARE)
        var cursor = from

        kids.forEachIndexed { index, childId ->
            val slice = evenShare + bonusWidth * (weights[index] / total)
            val middle = cursor + slice / 2
            val radius = (ringRadius[network.depth[childId] ?: 1] / baseline).toFloat()

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

/** How many nodes a ring can hold before it earns extra radius of its own. */
private const val MIN_RING_STEP = 4.0

/** How much harder a crowded ring pushes outward, per person past that. */
private const val CROWD_WEIGHT = 1.5

/** The share of a wedge split evenly among direct children before weight has a say. */
private const val MIN_CHILD_SHARE = 0.7

/** How many people hang off this one, counting themselves when nobody does. */
private fun leaves(personId: String, children: Map<String, List<String>>): Int {
    val kids = children[personId].orEmpty()
    if (kids.isEmpty()) return 1
    return kids.sumOf { leaves(it, children) }
}
