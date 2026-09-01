package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.data.DocumentTagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTagTest {

    @Test
    fun `a box drawn upwards and leftwards still comes out the right way round`() {
        // Dragging bottom-right to top-left is as natural as the other way.
        val tag = DocumentTagEntity.region("doc", "person", 0.8f, 0.9f, 0.2f, 0.3f)

        assertNotNull(tag)
        assertEquals(0.2f, tag!!.left, 0.0001f)
        assertEquals(0.3f, tag.top, 0.0001f)
        assertEquals(0.8f, tag.right, 0.0001f)
        assertEquals(0.9f, tag.bottom, 0.0001f)
    }

    @Test
    fun `a box dragged off the edge is clamped to the picture`() {
        val tag = DocumentTagEntity.region("doc", "person", -0.5f, -2f, 1.4f, 3f)

        assertNotNull(tag)
        assertEquals(0f, tag!!.left, 0.0001f)
        assertEquals(0f, tag.top, 0.0001f)
        assertEquals(1f, tag.right, 0.0001f)
        assertEquals(1f, tag.bottom, 0.0001f)
    }

    @Test
    fun `a stray tap is not a tag`() {
        // Well under the two per cent minimum: this was a finger, not an intent.
        assertNull(DocumentTagEntity.region("doc", "person", 0.5f, 0.5f, 0.501f, 0.502f))
    }

    @Test
    fun `a box with no width is not a tag however tall it is`() {
        assertNull(DocumentTagEntity.region("doc", "person", 0.5f, 0.1f, 0.5f, 0.9f))
    }

    @Test
    fun `a region tag knows it is about a patch, a bare tag knows it is about the whole`() {
        val region = DocumentTagEntity.region("doc", "person", 0.1f, 0.1f, 0.4f, 0.4f)
        val whole = DocumentTagEntity(documentId = "doc", personId = "person")

        assertFalse(region!!.whole)
        assertTrue(whole.whole)
    }
}
