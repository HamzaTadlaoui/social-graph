package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.familyTree
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.FRIEND_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARENT_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARTNER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SIBLING_OF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The same family as [PeopleGraphTest], drawn the way section 5.4 draws it. */
class FamilyTreeTest {

    private var pairs = 0

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
            tie("claire", "sophie", PARENT_OF) +
            tie("me", "bob", FRIEND_OF),
    )

    private fun generationOf(rootId: String, personId: String, up: Int = 2, down: Int = 2): Int? =
        familyTree(graph, rootId, up, down).places.firstOrNull { it.id == personId }?.generation

    @Test
    fun `parents sit one row above, grandparents two`() {
        assertEquals(0, generationOf("me", "me"))
        assertEquals(-1, generationOf("me", "david"))
        assertEquals(-2, generationOf("me", "robert"))
        assertEquals(-2, generationOf("me", "anne"))
    }

    @Test
    fun `an aunt is on the parents row, and her husband beside her`() {
        assertEquals(-1, generationOf("me", "claire"))
        assertEquals(-1, generationOf("me", "marc"))
    }

    @Test
    fun `a cousin lands on the same row as you`() {
        assertEquals(0, generationOf("me", "sophie"))
    }

    @Test
    fun `friends are not family, so the tree leaves them out`() {
        assertEquals(null, generationOf("me", "bob"))
    }

    @Test
    fun `the tree can be rebuilt around anyone, and everything moves`() {
        // Section 5.4: tapping Sophie re-roots the tree on her. Her mother
        // Claire is then one row up, and David - my father - is her uncle.
        assertEquals(0, generationOf("sophie", "sophie"))
        assertEquals(-1, generationOf("sophie", "claire"))
        assertEquals(-1, generationOf("sophie", "david"))
        assertEquals(-2, generationOf("sophie", "robert"))
        assertEquals(0, generationOf("sophie", "me"))
    }

    @Test
    fun `asking for fewer generations really does stop at the boundary`() {
        val close = familyTree(graph, "me", up = 1, down = 1)

        assertTrue(close.places.any { it.id == "david" })
        assertTrue(close.places.none { it.id == "robert" })
    }

    @Test
    fun `couples end up next to each other on their row`() {
        val tree = familyTree(graph, "me")
        val row = tree.places.filter { it.generation == -2 }.sortedBy { it.column }

        assertEquals(listOf("robert", "anne"), row.map { it.id })
    }

    @Test
    fun `each couple is drawn once, and every descent is a parent to a child`() {
        val tree = familyTree(graph, "me")

        assertEquals(tree.couples.distinct().size, tree.couples.size)
        assertTrue(tree.descents.contains("david" to "me"))
        assertTrue(tree.descents.none { it.first == "me" })
    }

    @Test
    fun `a couple stands together, with their children under the middle of both`() {
        val tree = familyTree(graph, "david", up = 1, down = 1)
        val column = tree.places.associate { it.id to it.column }

        val robert = column.getValue("robert")
        val anne = column.getValue("anne")
        val david = column.getValue("david")
        val claire = column.getValue("claire")

        // Partners are side by side, one column apart.
        assertEquals(1f, kotlin.math.abs(robert - anne), 0.001f)

        // And the pair of them sit over the middle of their two children, so
        // descent reads straight down the page rather than off to one side.
        assertEquals((david + claire) / 2f, (robert + anne) / 2f, 0.001f)
    }

    @Test
    fun `the person the tree is drawn around sits at zero`() {
        val tree = familyTree(graph, "david", up = 2, down = 2)

        assertEquals(0f, tree.places.first { it.id == "david" }.column, 0.001f)
    }

    @Test
    fun `nobody in a row is placed on top of anybody else`() {
        val tree = familyTree(graph, "david", up = 2, down = 2)

        for ((_, row) in tree.places.groupBy { it.generation }) {
            val columns = row.map { it.column }.sorted()
            for (i in 1 until columns.size) {
                assertTrue(
                    "two people share a column in the same generation",
                    columns[i] - columns[i - 1] >= 0.999f,
                )
            }
        }
    }
}
