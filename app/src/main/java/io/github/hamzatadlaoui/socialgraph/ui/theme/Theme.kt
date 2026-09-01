package io.github.hamzatadlaoui.socialgraph.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Cold and clinical: this is a surveillance desk, and the palette, the condensed
 * headers and the square corners are all saying so.
 *
 * Dynamic colour is off by default, and that default is the point. It used to win
 * unconditionally on API 31+, which meant the app's own scheme was only ever visible
 * on Android 11 and below — anything set here was invisible on a modern phone. The
 * capability is kept as a parameter for anyone who would rather have Material You.
 */
@Composable
fun SocialGraphTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SocialGraphTypography,
        shapes = SocialGraphShapes,
        content = content,
    )
}
