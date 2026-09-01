package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.model.RelationshipType
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.CHILD_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.EMPLOYEE_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.EMPLOYER_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.PARENT_OF
import io.github.hamzatadlaoui.socialgraph.model.RelationshipType.SIBLING_OF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipTypeTest {

    @Test
    fun `the pairs the brief names invert into each other`() {
        assertEquals(CHILD_OF, PARENT_OF.inverse)
        assertEquals(PARENT_OF, CHILD_OF.inverse)
        assertEquals(EMPLOYEE_OF, EMPLOYER_OF.inverse)
        assertEquals(EMPLOYER_OF, EMPLOYEE_OF.inverse)
    }

    @Test
    fun `a tie read from either end is its own inverse`() {
        assertEquals(SIBLING_OF, SIBLING_OF.inverse)
        assertTrue(SIBLING_OF.isSymmetric)
    }

    @Test
    fun `inverting twice gets you back where you started, for every type`() {
        for (type in RelationshipType.entries) {
            assertEquals(type, type.inverse.inverse)
        }
    }
}
