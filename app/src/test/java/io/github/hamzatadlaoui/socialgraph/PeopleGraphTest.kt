package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.radialLayout
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.CHILD_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARENT_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARTNER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SIBLING_OF
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The family drawn in section 5.4 of the brief:
 *
 *        Robert ─── Anne
 *            │
 *      ┌─────┴─────┐
 *    David       Claire ─── Marc
 *      │                      │
 *     me                    Sophie
 */
class PeopleGraphTest {

    private var pairs = 0

    /** Records a tie the way the repository does: both ways round, one pair id. */
    private fun tie(a: String, b: String, type: RelationshipType): List<Edge> {
        val pairId = "pair-${pairs++}"
        return listOf(Edge(pairId, a, b, type), Edge(pairId, b, a, type.inverse))
    }

    private val graph = PeopleGraph(
        tie("robert", "anne", PARTNER_OF) +
            tie("robert", "david", PARENT_OF) +
            tie("robert", "claire", PARENT_OF) +
            tie("anne", "david", PARENT_OF) +
            tie("anne", "claire", PARENT_OF) +
            tie("david", "claire", SIBLING_OF) +
            tie("claire", "marc", PARTNER_OF) +
            tie("david", "me", PARENT_OF) +
            tie("marc", "sophie", PARENT_OF) +
            tie("claire", "sophie", PARENT_OF),
    )

    @Test
    fun `both directions of a tie are there, so neighbours works from either end`() {
        assertTrue(graph.neighbours("me").any { it.toId == "david" && it.type == CHILD_OF })
        assertTrue(graph.neighbours("david").any { it.toId == "me" && it.type == PARENT_OF })
    }

    @Test
    fun `one hop out is exactly the people you are tied to`() {
        val network = graph.egoNetwork("me", depth = 1)

        assertEquals(setOf("me", "david"), network.ids)
        assertEquals(0, network.depth["me"])
        assertEquals(1, network.depth["david"])
    }

    @Test
    fun `two hops reaches the grandparents and the aunt, but not the cousin`() {
        val network = graph.egoNetwork("me", depth = 2)

        assertTrue(network.ids.containsAll(setOf("robert", "anne", "claire")))
        assertEquals(2, network.depth["claire"])
        // Sophie is four hops away: me, David, Claire, Sophie.
        assertTrue("sophie" !in network.ids)
    }

    @Test
    fun `an edge is kept once, not once per direction`() {
        val network = graph.egoNetwork("me", depth = 3)

        assertEquals(network.edges.map { it.pairId }.distinct().size, network.edges.size)
    }

    @Test
    fun `filtering to family alone drops everything else`() {
        val withFriend = PeopleGraph(
            tie("me", "david", CHILD_OF) + tie("me", "bob", RelationshipType.FRIEND_OF),
        )

        val family = withFriend.egoNetwork("me", depth = 1) { it.isFamily }

        assertEquals(setOf("me", "david"), family.ids)
    }

    @Test
    fun `a budget that reaches a step of family does not stretch to a weaker tie`() {
        val mixed = PeopleGraph(
            tie("me", "sibling", SIBLING_OF) + tie("me", "acquaintance", RelationshipType.KNOWS),
        )

        val network = mixed.egoNetwork("me", depth = 1)

        assertTrue("sibling" in network.ids)
        assertTrue("acquaintance" !in network.ids)
    }

    @Test
    fun `the same budget reaches a second step of family but only the first weak tie`() {
        val mixed = PeopleGraph(
            tie("me", "child", PARENT_OF) +
                tie("child", "grandchild", PARENT_OF) +
                tie("me", "coworker", RelationshipType.COWORKER_OF),
        )

        val network = mixed.egoNetwork("me", depth = 2)

        assertTrue("grandchild" in network.ids)
        assertTrue("coworker" in network.ids)
        // The coworker's own coworker would be a second weak-tie hop, which
        // this budget already spent reaching the coworker in the first place.
        assertEquals(2, network.depth["grandchild"])
        assertEquals(2, network.depth["coworker"])
    }

    @Test
    fun `finds how two people are connected, by the shortest way round`() {
        val path = graph.shortestPath("me", "sophie")

        assertEquals(listOf("david", "claire", "sophie"), path.map { it.toId })
        assertEquals("me", path.first().fromId)
    }

    @Test
    fun `two people with nothing between them have no path`() {
        val strangers = PeopleGraph(tie("me", "david", CHILD_OF) + tie("ada", "bob", SIBLING_OF))

        assertTrue(strangers.shortestPath("me", "ada").isEmpty())
    }

    @Test
    fun `the person at the centre sits at the centre, and rings grow outwards`() {
        val network = graph.egoNetwork("me", depth = 2)
        val places = radialLayout(network)

        assertEquals(0f, places.getValue("me").x, 0.001f)
        assertEquals(0f, places.getValue("me").y, 0.001f)

        val david = places.getValue("david")
        val radius = kotlin.math.hypot(david.x, david.y)
        // One hop of two, so halfway out.
        assertEquals(0.5f, radius, 0.001f)
    }

    @Test
    fun `the same network lays out the same way every time`() {
        val network = graph.egoNetwork("me", depth = 3)

        assertEquals(radialLayout(network), radialLayout(network))
    }

    @Test
    fun `a crowded ring is pushed further out than a sparse one would sit`() {
        val busy = PeopleGraph(
            (1..6).flatMap { tie("me", "friend$it", RelationshipType.FRIEND_OF) } +
                tie("friend1", "acquaintance", RelationshipType.FRIEND_OF),
        )
        val places = radialLayout(busy.egoNetwork("me", depth = 2))

        val ringOneRadius = kotlin.math.hypot(
            places.getValue("friend2").x,
            places.getValue("friend2").y,
        )
        // Sparsely populated, one hop of two would sit at half the outer
        // radius, same as in the test above; six people sharing this ring
        // push it out past that so they have room to be drawn apart.
        assertTrue(ringOneRadius > 0.5f)
    }

    @Test
    fun `a childless sibling is not squeezed out by one with a large family`() {
        // "busy" has nine further descendants; "quiet" has none, but both
        // are direct neighbours of "me" and neither is a subtree to size a
        // wedge by - each is a single box that needs its own fair slice.
        val skewed = PeopleGraph(
            tie("me", "busy", RelationshipType.FRIEND_OF) +
                tie("me", "quiet", RelationshipType.FRIEND_OF) +
                (1..9).flatMap { tie("busy", "descendant$it", RelationshipType.FRIEND_OF) },
        )
        val places = radialLayout(skewed.egoNetwork("me", depth = 2))

        fun angle(id: String): Double =
            kotlin.math.atan2(places.getValue(id).y.toDouble(), places.getValue(id).x.toDouble())
        var gap = kotlin.math.abs(angle("busy") - angle("quiet"))
        if (gap > PI) gap = 2 * PI - gap

        // Ten leaves sharing the circle evenly would average 2*PI/10 apart;
        // "busy" claims most of the wedge by weight, but "quiet" still keeps
        // a guaranteed minimum share rather than being squeezed towards zero.
        assertTrue(gap > PI / 4)
    }
}
