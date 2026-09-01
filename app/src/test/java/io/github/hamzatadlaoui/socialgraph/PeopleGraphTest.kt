package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.radialLayout
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.CHILD_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARENT_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARTNER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SIBLING_OF
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
}
