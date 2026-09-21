package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.graph.Edge
import io.github.hamzatadlaoui.socialgraph.graph.Kinship
import io.github.hamzatadlaoui.socialgraph.graph.PeopleGraph
import io.github.hamzatadlaoui.socialgraph.graph.impliedKin
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.FRIEND_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARENT_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARTNER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.EX_PARTNER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.FIANCE_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SIBLING_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SPOUSE_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.TWIN_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.WIDOWED_OF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KinshipTest {

    private var pairs = 0

    private fun tie(a: String, b: String, type: RelationshipType): List<Edge> {
        val pairId = "pair-${pairs++}"
        return listOf(Edge(pairId, a, b, type), Edge(pairId, b, a, type.inverse))
    }

    private fun kinOf(graph: PeopleGraph, personId: String, otherId: String): Kinship? =
        impliedKin(graph, personId).firstOrNull { it.personId == otherId }?.kinship

    @Test
    fun `two children of the same parent are siblings without anyone saying so`() {
        val graph = PeopleGraph(tie("david", "alex", PARENT_OF) + tie("david", "sam", PARENT_OF))

        assertEquals(Kinship.SIBLING, kinOf(graph, "alex", "sam"))
        assertEquals(Kinship.SIBLING, kinOf(graph, "sam", "alex"))
    }

    @Test
    fun `an inference says who it runs through`() {
        val graph = PeopleGraph(tie("david", "alex", PARENT_OF) + tie("david", "sam", PARENT_OF))

        val implied = impliedKin(graph, "alex").first { it.personId == "sam" }
        assertEquals("david", implied.throughId)
    }

    @Test
    fun `a parent's parent is a grandparent, and one further back a great-grandparent`() {
        val graph = PeopleGraph(
            tie("ada", "david", PARENT_OF) +
                tie("david", "alex", PARENT_OF) +
                tie("alex", "robin", PARENT_OF),
        )

        assertEquals(Kinship.GRANDPARENT, kinOf(graph, "alex", "ada"))
        assertEquals(Kinship.GRANDCHILD, kinOf(graph, "ada", "alex"))
        assertEquals(Kinship.GREAT_GRANDPARENT, kinOf(graph, "robin", "ada"))
        assertEquals(Kinship.GREAT_GRANDCHILD, kinOf(graph, "ada", "robin"))
    }

    @Test
    fun `a parent's sibling is an aunt or uncle, and their children are cousins`() {
        val graph = PeopleGraph(
            tie("david", "marie", SIBLING_OF) +
                tie("david", "alex", PARENT_OF) +
                tie("marie", "jo", PARENT_OF),
        )

        assertEquals(Kinship.AUNT_OR_UNCLE, kinOf(graph, "alex", "marie"))
        assertEquals(Kinship.NIECE_OR_NEPHEW, kinOf(graph, "marie", "alex"))
        assertEquals(Kinship.COUSIN, kinOf(graph, "alex", "jo"))
        assertEquals(Kinship.COUSIN, kinOf(graph, "jo", "alex"))
    }

    @Test
    fun `a partner's parents and siblings are in-laws`() {
        val graph = PeopleGraph(
            tie("alex", "sam", PARTNER_OF) +
                tie("ada", "sam", PARENT_OF) +
                tie("sam", "kit", SIBLING_OF),
        )

        assertEquals(Kinship.PARENT_IN_LAW, kinOf(graph, "alex", "ada"))
        assertEquals(Kinship.SIBLING_IN_LAW, kinOf(graph, "alex", "kit"))
        assertEquals(Kinship.CHILD_IN_LAW, kinOf(graph, "ada", "alex"))
    }

    @Test
    fun `a spouse's family is in-laws too, and stays in-laws once widowed`() {
        val married = PeopleGraph(tie("alex", "sam", SPOUSE_OF) + tie("ada", "sam", PARENT_OF))
        val widowed = PeopleGraph(tie("alex", "sam", WIDOWED_OF) + tie("ada", "sam", PARENT_OF))

        assertEquals(Kinship.PARENT_IN_LAW, kinOf(married, "alex", "ada"))
        assertEquals(Kinship.PARENT_IN_LAW, kinOf(widowed, "alex", "ada"))
    }

    @Test
    fun `an ex-partner's family was never in-laws, and neither is a fiance's yet`() {
        val ex = PeopleGraph(tie("alex", "sam", EX_PARTNER_OF) + tie("ada", "sam", PARENT_OF))
        val engaged = PeopleGraph(tie("alex", "sam", FIANCE_OF) + tie("ada", "sam", PARENT_OF))

        assertNull(kinOf(ex, "alex", "ada"))
        assertNull(kinOf(engaged, "alex", "ada"))
    }

    @Test
    fun `a twin counts as a sibling for everything that follows from one`() {
        val graph = PeopleGraph(
            tie("david", "marie", TWIN_OF) +
                tie("david", "alex", PARENT_OF) +
                tie("marie", "jo", PARENT_OF),
        )

        assertEquals(Kinship.AUNT_OR_UNCLE, kinOf(graph, "alex", "marie"))
        assertEquals(Kinship.COUSIN, kinOf(graph, "alex", "jo"))
    }

    @Test
    fun `half siblings are only called that when both parents of both are known`() {
        // Alex and Sam share David. Alex's other parent is Marie, Sam's is Jo.
        val both = PeopleGraph(
            tie("david", "alex", PARENT_OF) +
                tie("marie", "alex", PARENT_OF) +
                tie("david", "sam", PARENT_OF) +
                tie("jo", "sam", PARENT_OF),
        )
        assertEquals(Kinship.HALF_SIBLING, kinOf(both, "alex", "sam"))

        // The same family with the second parents not recorded: there is no way
        // to tell, so the gentler answer is the right one.
        val partial = PeopleGraph(
            tie("david", "alex", PARENT_OF) + tie("david", "sam", PARENT_OF),
        )
        assertEquals(Kinship.SIBLING, kinOf(partial, "alex", "sam"))
    }

    @Test
    fun `a tie already recorded is never offered back as an inference`() {
        // The siblinghood is written down as well as being derivable.
        val graph = PeopleGraph(
            tie("david", "alex", PARENT_OF) +
                tie("david", "sam", PARENT_OF) +
                tie("alex", "sam", SIBLING_OF),
        )

        assertNull(kinOf(graph, "alex", "sam"))
    }

    @Test
    fun `friendship implies nothing about family`() {
        val graph = PeopleGraph(
            tie("alex", "sam", FRIEND_OF) + tie("sam", "kit", FRIEND_OF),
        )

        assertTrue(impliedKin(graph, "alex").isEmpty())
    }

    @Test
    fun `a grandparent's sibling is a great-aunt or great-uncle`() {
        val graph = PeopleGraph(
            tie("ada", "david", PARENT_OF) +
                tie("david", "alex", PARENT_OF) +
                tie("ada", "marie", SIBLING_OF),
        )

        assertEquals(Kinship.GREAT_AUNT_OR_UNCLE, kinOf(graph, "alex", "marie"))
        assertEquals(Kinship.GREAT_NIECE_OR_NEPHEW, kinOf(graph, "marie", "alex"))
    }

    @Test
    fun `a step-parent's other child is a step-sibling, not a blood sibling`() {
        val graph = PeopleGraph(
            tie("david", "alex", PARENT_OF) +
                tie("david", "jo", PARTNER_OF) +
                tie("jo", "sam", PARENT_OF),
        )

        assertEquals(Kinship.STEP_SIBLING, kinOf(graph, "alex", "sam"))
    }

    @Test
    fun `a step-parent's child who is also a blood child is a sibling, not a step-sibling`() {
        val graph = PeopleGraph(
            tie("david", "alex", PARENT_OF) +
                tie("david", "jo", PARTNER_OF) +
                tie("jo", "sam", PARENT_OF) +
                tie("david", "sam", PARENT_OF),
        )

        assertEquals(Kinship.SIBLING, kinOf(graph, "alex", "sam"))
    }

    @Test
    fun `a sibling's parent, recorded through the sibling tie alone, is offered as one's own`() {
        val graph = PeopleGraph(
            tie("alex", "sam", SIBLING_OF) +
                tie("david", "sam", PARENT_OF),
        )

        assertEquals(Kinship.SIBLINGS_PARENT, kinOf(graph, "alex", "david"))
    }

    @Test
    fun `a sibling's parent already recorded as one's own is not repeated back`() {
        val graph = PeopleGraph(
            tie("alex", "sam", SIBLING_OF) +
                tie("david", "sam", PARENT_OF) +
                tie("david", "alex", PARENT_OF),
        )

        assertTrue(impliedKin(graph, "alex").none { it.personId == "david" })
    }

    @Test
    fun `a parent's partner who is not a parent is named as exactly that`() {
        val graph = PeopleGraph(
            tie("david", "alex", PARENT_OF) + tie("david", "jo", PARTNER_OF),
        )

        assertEquals(Kinship.PARENTS_PARTNER, kinOf(graph, "alex", "jo"))
        assertEquals(Kinship.PARTNERS_CHILD, kinOf(graph, "jo", "alex"))
    }

    @Test
    fun `nobody is their own relation`() {
        val graph = PeopleGraph(tie("david", "alex", PARENT_OF) + tie("david", "sam", PARENT_OF))

        assertTrue(impliedKin(graph, "alex").none { it.personId == "alex" })
    }

    @Test
    fun `the closest reading wins when two paths reach the same person`() {
        // Marie is Alex's aunt by blood and would also be reachable as a
        // sibling-in-law of nobody in particular; the aunt reading is nearer.
        val graph = PeopleGraph(
            tie("david", "marie", SIBLING_OF) + tie("david", "alex", PARENT_OF),
        )

        assertEquals(Kinship.AUNT_OR_UNCLE, kinOf(graph, "alex", "marie"))
    }
}
