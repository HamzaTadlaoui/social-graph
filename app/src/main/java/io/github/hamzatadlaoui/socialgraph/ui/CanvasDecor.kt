package io.github.hamzatadlaoui.socialgraph.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor
import kotlin.math.min

/**
 * The two hand-drawn canvases share a visual language: graph paper underneath,
 * and corner brackets around whatever is being looked at. Both live here so the
 * network and the family tree cannot drift apart.
 */

/**
 * Faint graph paper, moving with the drawing rather than the screen so it reads
 * as a surface being panned over rather than a texture laid on the glass.
 */
fun DrawScope.drawGrid(colour: Color, step: Float, pan: Offset, alpha: Float = 0.35f) {
    if (step <= 1f) return

    // Start at the first line left of / above the viewport, so panning slides the
    // grid instead of rebuilding it from the corner.
    val firstX = pan.x - floor(pan.x / step) * step - step
    val firstY = pan.y - floor(pan.y / step) * step - step

    var x = firstX
    while (x <= size.width) {
        drawLine(colour, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f, alpha = alpha)
        x += step
    }
    var y = firstY
    while (y <= size.height) {
        drawLine(colour, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f, alpha = alpha)
        y += step
    }
}

/**
 * Four corner marks around [rect] — the selection cue, in place of a highlight
 * or a glow, because a bracket says "this one is being looked at".
 */
fun DrawScope.drawBrackets(rect: Rect, colour: Color, strokeWidth: Float, gap: Float = 0f) {
    val box = Rect(
        left = rect.left - gap,
        top = rect.top - gap,
        right = rect.right + gap,
        bottom = rect.bottom + gap,
    )
    // Arms are a third of the shorter side, so they stay corner marks on a wide
    // plate and do not close up into a full outline on a small one.
    val arm = min(box.width, box.height) / 3f

    fun corner(at: Offset, dx: Float, dy: Float) {
        drawLine(colour, at, Offset(at.x + dx * arm, at.y), strokeWidth = strokeWidth)
        drawLine(colour, at, Offset(at.x, at.y + dy * arm), strokeWidth = strokeWidth)
    }

    corner(box.topLeft, 1f, 1f)
    corner(Offset(box.right, box.top), -1f, 1f)
    corner(Offset(box.left, box.bottom), 1f, -1f)
    corner(box.bottomRight, -1f, -1f)
}
