package io.github.hamzatadlaoui.socialgraph.model

/** A corner of a box, for when one of them is being dragged. */
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * A rectangle over a picture, held as fractions of it: 0 is the left or top
 * edge, 1 the right or bottom. Fractions rather than pixels because a tag has
 * to go on meaning the same patch of someone's face after the picture has been
 * decoded at some other size.
 *
 * Plain Kotlin with no Android in it, like the rest of `model/`, because
 * dragging a box around is exactly the sort of fiddly arithmetic that is worth
 * being able to test without a phone in the loop.
 */
data class CropBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /** Big enough to have been meant: see [MINIMUM]. */
    val usable: Boolean get() = width >= MINIMUM && height >= MINIMUM

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /**
     * Corners the right way round and inside the picture. Dragging past the top
     * left of a box flips it rather than collapsing it, which is what anyone who
     * has ever dragged a selection expects.
     */
    fun tidied(): CropBox = CropBox(
        left = minOf(left, right).coerceIn(0f, 1f),
        top = minOf(top, bottom).coerceIn(0f, 1f),
        right = maxOf(left, right).coerceIn(0f, 1f),
        bottom = maxOf(top, bottom).coerceIn(0f, 1f),
    )

    /**
     * Slides the whole box, stopping at the edges of the picture rather than
     * letting the far side run off and quietly shrink it.
     */
    fun movedBy(dx: Float, dy: Float): CropBox {
        val box = tidied()
        val x = dx.coerceIn(-box.left, 1f - box.right)
        val y = dy.coerceIn(-box.top, 1f - box.bottom)
        return CropBox(box.left + x, box.top + y, box.right + x, box.bottom + y)
    }

    /** Drags one corner to a new place, leaving the opposite one where it is. */
    fun withCorner(corner: Corner, x: Float, y: Float): CropBox = when (corner) {
        Corner.TOP_LEFT -> CropBox(x, y, right, bottom)
        Corner.TOP_RIGHT -> CropBox(left, y, x, bottom)
        Corner.BOTTOM_LEFT -> CropBox(x, top, right, y)
        Corner.BOTTOM_RIGHT -> CropBox(left, top, x, y)
    }.tidied()

    /**
     * Which corner a finger landed on, if any. [reach] is in the same fractional
     * units, so the caller converts from touch slop once and this stays pure.
     */
    fun cornerAt(x: Float, y: Float, reach: Float): Corner? = Corner.entries.firstOrNull {
        val at = cornerPoint(it)
        kotlin.math.abs(x - at.first) <= reach && kotlin.math.abs(y - at.second) <= reach
    }

    fun cornerPoint(corner: Corner): Pair<Float, Float> = when (corner) {
        Corner.TOP_LEFT -> left to top
        Corner.TOP_RIGHT -> right to top
        Corner.BOTTOM_LEFT -> left to bottom
        Corner.BOTTOM_RIGHT -> right to bottom
    }

    companion object {
        /** Two per cent of the picture each way: smaller than that was a slip, not a tag. */
        const val MINIMUM = 0.02f

        val EMPTY = CropBox(0f, 0f, 0f, 0f)

        /**
         * Where a picture of [imageWidth] by [imageHeight] actually lands when it
         * is fitted inside a view, as left, top, right, bottom in view pixels.
         *
         * The drawn area is usually smaller than the box it sits in - letterboxed
         * one way or the other - and every conversion between a finger and a
         * fraction has to go through this same rectangle or the tags drift.
         */
        fun fitted(
            imageWidth: Int,
            imageHeight: Int,
            viewWidth: Int,
            viewHeight: Int,
        ): CropBox {
            if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
                return EMPTY
            }
            val scale = minOf(
                viewWidth.toFloat() / imageWidth,
                viewHeight.toFloat() / imageHeight,
            )
            val width = imageWidth * scale
            val height = imageHeight * scale
            val left = (viewWidth - width) / 2f
            val top = (viewHeight - height) / 2f
            return CropBox(left, top, left + width, top + height)
        }
    }
}
