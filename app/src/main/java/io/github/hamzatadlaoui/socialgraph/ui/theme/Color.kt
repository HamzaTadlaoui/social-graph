package io.github.hamzatadlaoui.socialgraph.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A cold blue-slate readout: one bright cyan carries everything selectable, amber is
 * held back for things wanting attention, and the greys do the rest. Dark is the
 * intended look; the light scheme is the same dossier printed rather than displayed.
 *
 * Nothing outside this file names a colour — every screen reads the scheme through
 * MaterialTheme — so these two tables are the whole palette.
 */

// Dark: the terminal ground.
private val Ink = Color(0xFF131C24)
private val Panel = Color(0xFF1B2733)
private val PanelRaised = Color(0xFF24333F)
private val Cyan = Color(0xFF4FC3D9)
private val CyanDeep = Color(0xFF17414F)
private val CyanPale = Color(0xFFA8E4F0)
private val Readout = Color(0xFFD7E3EB)
private val ReadoutDim = Color(0xFF93A9B8)
private val Rule = Color(0xFF35495A)
private val RuleFaint = Color(0xFF263644)
private val Amber = Color(0xFFE3A344)
private val Alert = Color(0xFFD9534F)

internal val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF04222B),
    primaryContainer = CyanDeep,
    onPrimaryContainer = CyanPale,
    secondary = Color(0xFF8AA6B8),
    onSecondary = Color(0xFF10222D),
    secondaryContainer = Color(0xFF2C3E4C),
    onSecondaryContainer = Color(0xFFC5D6E2),
    tertiary = Amber,
    onTertiary = Color(0xFF2B1B00),
    tertiaryContainer = Color(0xFF4A3411),
    onTertiaryContainer = Color(0xFFF5D9A8),
    error = Alert,
    onError = Color(0xFF2B0705),
    errorContainer = Color(0xFF5C221F),
    onErrorContainer = Color(0xFFF6C7C4),
    background = Ink,
    onBackground = Readout,
    surface = Panel,
    onSurface = Readout,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = ReadoutDim,
    surfaceContainerLowest = Color(0xFF0D151C),
    surfaceContainerLow = Ink,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelRaised,
    surfaceContainerHighest = Color(0xFF2C3C49),
    inverseSurface = Readout,
    inverseOnSurface = Ink,
    inversePrimary = Color(0xFF10627A),
    outline = Rule,
    outlineVariant = RuleFaint,
    scrim = Color(0xFF000000),
)

// Light: the same record on paper.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF10627A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBEE6F0),
    onPrimaryContainer = Color(0xFF00323F),
    secondary = Color(0xFF4A6172),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEDFEB),
    onSecondaryContainer = Color(0xFF0B1E2B),
    tertiary = Color(0xFF8A5A10),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB0),
    onTertiaryContainer = Color(0xFF2C1A00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFEDF1F4),
    onBackground = Color(0xFF17222B),
    surface = Color(0xFFF6F8FA),
    onSurface = Color(0xFF17222B),
    surfaceVariant = Color(0xFFDFE6EB),
    onSurfaceVariant = Color(0xFF44545F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5F7),
    surfaceContainer = Color(0xFFEAEFF3),
    surfaceContainerHigh = Color(0xFFE3EAEF),
    surfaceContainerHighest = Color(0xFFDCE4EA),
    inverseSurface = Color(0xFF2C3C49),
    inverseOnSurface = Color(0xFFEDF1F4),
    inversePrimary = Cyan,
    outline = Color(0xFF7B8D9B),
    outlineVariant = Color(0xFFC3CFD8),
    scrim = Color(0xFF000000),
)
