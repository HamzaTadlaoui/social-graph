package io.github.hamzatadlaoui.socialgraph.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.hamzatadlaoui.socialgraph.R

/**
 * Two families, doing two jobs. Barlow Condensed heads the panels — narrow and
 * letter-spaced, so a heading reads as a label on a file rather than a sentence.
 * IBM Plex Mono carries the body, which is what makes a half-known date (`c.1974`)
 * and a tie label look like a record instead of prose.
 *
 * Both are SIL Open Font Licence; see res/font/README.md.
 */

internal val Condensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
)

internal val Mono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

private fun head(size: Int, line: Int, tracking: Double, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(
        fontFamily = Condensed,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

private fun body(size: Int, line: Int, tracking: Double, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = Mono,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

internal val SocialGraphTypography = Typography(
    displayLarge = head(52, 60, 0.0),
    displayMedium = head(42, 50, 0.0),
    displaySmall = head(34, 42, 0.0),

    headlineLarge = head(30, 38, 0.5),
    headlineMedium = head(26, 33, 0.5),
    headlineSmall = head(22, 28, 0.8),

    titleLarge = head(21, 27, 1.2),
    titleMedium = head(17, 23, 1.0),
    titleSmall = head(15, 20, 1.0, FontWeight.Medium),

    bodyLarge = body(15, 23, 0.3),
    bodyMedium = body(13, 20, 0.25),
    bodySmall = body(12, 17, 0.3),

    // Labels sit on buttons, chips and the navigation bar: tracked wide, because
    // that spacing is most of what makes an interface read as instrumentation.
    labelLarge = body(13, 18, 1.0, FontWeight.Medium),
    labelMedium = body(11, 15, 1.1, FontWeight.Medium),
    labelSmall = body(10, 14, 1.2, FontWeight.Medium),
)
