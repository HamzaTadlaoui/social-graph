package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.RelationshipEntity
import io.github.hamzatadlaoui.socialgraph.model.Certainty
import io.github.hamzatadlaoui.socialgraph.model.FuzzyDate
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RelationshipPairTest {

    private val davidIsFatherOfAlex = RelationshipEntity(
        id = "row-1",
        pairId = "pair-1",
        fromId = "david",
        toId = "alex",
        type = RelationshipType.PARENT_OF,
        start = FuzzyDate(2008),
        notes = "born in Lyon",
        certainty = Certainty.PROBABLE,
    )

    @Test
    fun `the other end of a tie swaps the people and inverts the type`() {
        val backwards = davidIsFatherOfAlex.inverse("row-2")

        assertEquals("alex", backwards.fromId)
        assertEquals("david", backwards.toId)
        assertEquals(RelationshipType.CHILD_OF, backwards.type)
    }

    @Test
    fun `both rows are the same tie, so they share a pair id and their details`() {
        val backwards = davidIsFatherOfAlex.inverse("row-2")

        assertEquals(davidIsFatherOfAlex.pairId, backwards.pairId)
        assertNotEquals(davidIsFatherOfAlex.id, backwards.id)
        assertEquals(FuzzyDate(2008), backwards.start)
        assertEquals("born in Lyon", backwards.notes)
        assertEquals(Certainty.PROBABLE, backwards.certainty)
    }

    @Test
    fun `going round both ends gets you the tie you first wrote`() {
        val there = davidIsFatherOfAlex
        val back = there.inverse("row-2").inverse(there.id)

        assertEquals(there, back)
    }
}
