package io.github.hamzatadlaoui.socialgraph

import io.github.hamzatadlaoui.socialgraph.model.Corner
import io.github.hamzatadlaoui.socialgraph.model.CropBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CropBoxTest {

    private fun box(l: Float, t: Float, r: Float, b: Float) = CropBox(l, t, r, b)

    @Test
    fun `a box dragged up and to the left comes out the right way round`() {
        val flipped = box(0.8f, 0.9f, 0.2f, 0.3f).tidied()

        assertEquals(0.2f, flipped.left, 0.0001f)
        assertEquals(0.3f, flipped.top, 0.0001f)
        assertEquals(0.8f, flipped.right, 0.0001f)
        assertEquals(0.9f, flipped.bottom, 0.0001f)
    }

    @Test
    fun `a box dragged off the picture is clamped to it`() {
        val outside = box(-0.5f, -2f, 1.4f, 3f).tidied()

        assertEquals(0f, outside.left, 0.0001f)
        assertEquals(0f, outside.top, 0.0001f)
        assertEquals(1f, outside.right, 0.0001f)
        assertEquals(1f, outside.bottom, 0.0001f)
    }

    @Test
    fun `moving a box against an edge stops it rather than squashing it`() {
        val near = box(0.1f, 0.1f, 0.3f, 0.3f)

        val shoved = near.movedBy(-1f, -1f)

        // Hard against the corner, and still exactly the size it was.
        assertEquals(0f, shoved.left, 0.0001f)
        assertEquals(0f, shoved.top, 0.0001f)
        assertEquals(0.2f, shoved.width, 0.0001f)
        assertEquals(0.2f, shoved.height, 0.0001f)
    }

    @Test
    fun `moving a box the other way also keeps its size`() {
        val moved = box(0.7f, 0.7f, 0.9f, 0.9f).movedBy(1f, 1f)

        assertEquals(1f, moved.right, 0.0001f)
        assertEquals(1f, moved.bottom, 0.0001f)
        assertEquals(0.2f, moved.width, 0.0001f)
        assertEquals(0.2f, moved.height, 0.0001f)
    }

    @Test
    fun `dragging one corner leaves the opposite one alone`() {
        val original = box(0.2f, 0.2f, 0.8f, 0.8f)

        val resized = original.withCorner(Corner.TOP_LEFT, 0.4f, 0.5f)

        assertEquals(0.4f, resized.left, 0.0001f)
        assertEquals(0.5f, resized.top, 0.0001f)
        assertEquals(0.8f, resized.right, 0.0001f)
        assertEquals(0.8f, resized.bottom, 0.0001f)
    }

    @Test
    fun `dragging a corner past its opposite flips rather than inverts the box`() {
        val original = box(0.2f, 0.2f, 0.8f, 0.8f)

        // Hauled right across the box and out the other side.
        val turned = original.withCorner(Corner.TOP_LEFT, 0.95f, 0.9f)

        assertEquals(0.8f, turned.left, 0.0001f)
        assertEquals(0.8f, turned.top, 0.0001f)
        assertEquals(0.95f, turned.right, 0.0001f)
        assertEquals(0.9f, turned.bottom, 0.0001f)
        assertTrue(turned.width > 0f && turned.height > 0f)
    }

    @Test
    fun `a corner is found only when the finger lands near it`() {
        val target = box(0.2f, 0.2f, 0.8f, 0.8f)

        assertEquals(Corner.TOP_LEFT, target.cornerAt(0.21f, 0.22f, reach = 0.05f))
        assertEquals(Corner.BOTTOM_RIGHT, target.cornerAt(0.79f, 0.78f, reach = 0.05f))
        // The middle of the box belongs to moving it, not resizing it.
        assertNull(target.cornerAt(0.5f, 0.5f, reach = 0.05f))
    }

    @Test
    fun `a box has to be big enough to have been meant`() {
        assertFalse(box(0.5f, 0.5f, 0.501f, 0.502f).usable)
        assertFalse(box(0.5f, 0.1f, 0.5f, 0.9f).usable)
        assertTrue(box(0.2f, 0.2f, 0.4f, 0.4f).usable)
    }

    @Test
    fun `a wide picture in a tall view is letterboxed top and bottom`() {
        // 1000x500 inside 500x500: fills the width, half the height, centred.
        val frame = CropBox.fitted(1000, 500, 500, 500)

        assertEquals(0f, frame.left, 0.001f)
        assertEquals(125f, frame.top, 0.001f)
        assertEquals(500f, frame.width, 0.001f)
        assertEquals(250f, frame.height, 0.001f)
    }

    @Test
    fun `a tall picture in a wide view is letterboxed left and right`() {
        val frame = CropBox.fitted(500, 1000, 500, 500)

        assertEquals(125f, frame.left, 0.001f)
        assertEquals(0f, frame.top, 0.001f)
        assertEquals(250f, frame.width, 0.001f)
        assertEquals(500f, frame.height, 0.001f)
    }

    @Test
    fun `a view with no size yet gives nothing rather than dividing by zero`() {
        assertEquals(CropBox.EMPTY, CropBox.fitted(800, 600, 0, 0))
        assertEquals(CropBox.EMPTY, CropBox.fitted(0, 0, 500, 500))
    }

    @Test
    fun `a point maps to the same fraction it came from`() {
        // The round trip a finger makes: view pixel to fraction and back.
        val frame = CropBox.fitted(1000, 500, 500, 500)
        val viewX = frame.left + 0.25f * frame.width
        val viewY = frame.top + 0.75f * frame.height

        assertEquals(0.25f, (viewX - frame.left) / frame.width, 0.0001f)
        assertEquals(0.75f, (viewY - frame.top) / frame.height, 0.0001f)
    }
}
