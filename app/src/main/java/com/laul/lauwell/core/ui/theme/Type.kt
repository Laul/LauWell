// Font(resId, …, variationSettings) is still @ExperimentalTextApi in Compose 1.7; it is the only way
// to pick a weight from a bundled variable font.
@file:OptIn(ExperimentalTextApi::class)

package com.laul.lauwell.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.laul.lauwell.R

/**
 * App text styles from the locked design (PLAN.md → Decisions → Visual design) — a custom set,
 * not Material3's Typography scale. Bricolage Grotesque for display, headline and title; IBM Plex
 * Sans for body and labels; IBM Plex Mono for **every** metric value (numbers, times, "2/3").
 * Colors are not set here: pass one from [LauWellColors] at the call site.
 */
@Immutable
data class LauWellTypography(
    /** Wordmark and hero text. 56 sp / 700. */
    val display: TextStyle,
    /** Screen titles ("Tracker", "Good morning"). 28 sp / 700. */
    val headline: TextStyle,
    /** Row and card titles ("Steps"). 18 sp / 600. */
    val title: TextStyle,
    /** Running text. 16 sp / 400. */
    val body: TextStyle,
    /** Section labels: uppercase at the call site, tracked. 11.5 sp / 600. */
    val label: TextStyle,
    /** Metric values in rows, tiles and stats. 20 sp / 500, Mono. */
    val metric: TextStyle,
    /** The big value on a detail screen. 60 sp / 500, Mono. */
    val metricXl: TextStyle,
    /**
     * Component sizes the design uses inside rows, tiles and sheets (not on its type sheet).
     * Row and chip names: 15 sp / 500, Plex Sans.
     */
    val item: TextStyle,
    /** Context lines, units, badges and captions: 12 sp / 400, Plex Sans. */
    val caption: TextStyle,
    /** A value in a list row (Tracker hub, sheet): 15 sp / 500, Mono. */
    val metricSmall: TextStyle,
)

// Fonts are bundled in res/font (decided in M0: nothing downloaded at runtime). Bricolage Grotesque
// and IBM Plex Sans are variable fonts; each weight is an instance of the same file. Bricolage also
// gets its optical size set to the style's size, as browsers do (`font-optical-sizing: auto`) — the
// design was drawn that way.
private fun bricolage(weight: Int, opticalSize: Float) = FontFamily(
    Font(
        R.font.bricolage_grotesque,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.Setting("opsz", opticalSize),
        ),
    ),
)

private fun plexSans(weight: Int) = Font(
    R.font.ibm_plex_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val PlexSans = FontFamily(plexSans(400), plexSans(500), plexSans(600), plexSans(700))

private val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

val DefaultLauWellTypography = LauWellTypography(
    display = TextStyle(
        fontFamily = bricolage(weight = 700, opticalSize = 56f),
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.035).em,
    ),
    headline = TextStyle(
        fontFamily = bricolage(weight = 700, opticalSize = 28f),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.025).em,
    ),
    title = TextStyle(
        fontFamily = bricolage(weight = 600, opticalSize = 18f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    body = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    label = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.09.em,
    ),
    metric = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    ),
    metricXl = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.02).em,
    ),
    item = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    metricSmall = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
)

val LocalLauWellTypography = staticCompositionLocalOf { DefaultLauWellTypography }
