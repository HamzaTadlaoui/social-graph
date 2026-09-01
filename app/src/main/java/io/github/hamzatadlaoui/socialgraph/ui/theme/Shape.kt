package io.github.hamzatadlaoui.socialgraph.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Hard edges. A rounded card reads as something soft and personal; this app is
 * meant to read as a filing system, so corners stay square and the couple of
 * places that would look broken at zero — the FAB, dialogs — get 2.dp instead.
 */
internal val SocialGraphShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(0.dp),
)
