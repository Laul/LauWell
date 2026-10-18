package com.laul.lauwell.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.laul.lauwell.core.catalog.Category

/**
 * One data category's colors in one mode. Color means category: a metric keeps its category's
 * colors wherever it is shown (PLAN.md → Decisions → Visual design).
 *
 * - [fg]: text, icons, chart bars, icon-tile outlines.
 * - [tint]: a light (dark-mode: deep) fill behind category content.
 * - [stroke]: a mid-tone outline, e.g. the quick-log chips.
 */
@Immutable
data class CategoryColor(val fg: Color, val tint: Color, val stroke: Color)

/**
 * App color tokens from the locked design (direction 13b, "Vivid" palette: token sheet in the
 * later export, local `docs/design/Design • Global direction - v13b-02.html`; values recorded in
 * PLAN.md). Not modeled on Material3's ColorScheme: the primary is near-black [ink]/[accent], not a hue.
 *
 * Category colors are derived in OKLCH from one hue per category (vitals 28, labs 300, records 255,
 * lifestyle 150, symptoms 352, T&A 200; ostomy 80 is ours, not the design's): fg L 0.57 / C 0.20, tint L 0.92 / C 0.05, stroke L 0.80 /
 * C 0.14; dark: fg L 0.78 / C 0.18, tint L 0.27 / C 0.06, stroke L 0.45 / C 0.14, each sRGB channel
 * clipped. Look them up with
 * [forCategory] rather than the fields.
 */
@Immutable
data class LauWellColors(
    /** Page background. */
    val bg: Color,
    val surface: Color,
    /** Recessed fills: segmented-control track, empty chart slot. */
    val surface2: Color,
    /** Primary text and icons. */
    val ink: Color,
    /** Secondary text: context lines, units, captions. */
    val ink2: Color,
    /** The 2px ink rule under section labels. */
    val line: Color,
    /** The filled main-action button. */
    val accent: Color,
    val onAccent: Color,
    /** Content panels and their 1px hairline (also the dividers inside a panel). */
    val panel: Color,
    val panelLine: Color,
    /**
     * Soft divider outside panels: sheet rows, the per-day list, the bottom-nav top edge, a missing
     * chart day. Used by the design but not on its token sheet (`lineSoft` there).
     */
    val lineSoft: Color,
    /** Empty part of a progress track, e.g. a pinned tile's goal bar (`trackBg` in the design). */
    val trackBg: Color,
    val vitals: CategoryColor,
    val labs: CategoryColor,
    val medicalRecords: CategoryColor,
    val lifestyle: CategoryColor,
    val symptoms: CategoryColor,
    val treatmentsAndAppliances: CategoryColor,
    val ostomy: CategoryColor,
) {
    /** The single category → color lookup. Labs and Ostomy get their entries when they join [Category]. */
    fun forCategory(category: Category): CategoryColor = when (category) {
        Category.Vitals -> vitals
        Category.MedicalRecords -> medicalRecords
        Category.Lifestyle -> lifestyle
        Category.Symptoms -> symptoms
        Category.TreatmentsAndAppliances -> treatmentsAndAppliances
    }
}

val LightLauWellColors = LauWellColors(
    bg = Color(0xFFF4F6F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE9EDF0),
    ink = Color(0xFF12171B),
    ink2 = Color(0xFF56616B),
    line = Color(0xFF16191C),
    accent = Color(0xFF16191C),
    onAccent = Color(0xFFFFFFFF),
    panel = Color(0xFFFAFBFB),
    panelLine = Color(0xFFE4E8EB),
    lineSoft = Color(0xFFD9DFE4),
    trackBg = Color(0xFFE6EAED),
    vitals = CategoryColor(fg = Color(0xFFD4312A), tint = Color(0xFFFFD9D2), stroke = Color(0xFFFF998A)),
    labs = CategoryColor(fg = Color(0xFF8C51D9), tint = Color(0xFFE9DEFF), stroke = Color(0xFFCCA8FF)),
    medicalRecords = CategoryColor(fg = Color(0xFF0073E9), tint = Color(0xFFCFE7FF), stroke = Color(0xFF7DC1FF)),
    lifestyle = CategoryColor(fg = Color(0xFF00952A), tint = Color(0xFFCEEFD3), stroke = Color(0xFF75D78D)),
    symptoms = CategoryColor(fg = Color(0xFFC83184), tint = Color(0xFFFFD8E8), stroke = Color(0xFFFF96C7)),
    treatmentsAndAppliances = CategoryColor(fg = Color(0xFF0095A5), tint = Color(0xFFBFEFF2), stroke = Color(0xFF00D8E1)),
    ostomy = CategoryColor(fg = Color(0xFFB26200), tint = Color(0xFFF6E2C0), stroke = Color(0xFFEDB345)),
)

val DarkLauWellColors = LauWellColors(
    bg = Color(0xFF090B0D),
    surface = Color(0xFF12161A),
    surface2 = Color(0xFF1B2127),
    ink = Color(0xFFE4EAEE),
    ink2 = Color(0xFF93A0AB),
    line = Color(0xFFECEFF1),
    accent = Color(0xFFECEFF1),
    onAccent = Color(0xFF0B0D0F),
    panel = Color(0xFF14191D),
    panelLine = Color(0xFF262E35),
    lineSoft = Color(0xFF252D35),
    trackBg = Color(0xFF262E35),
    vitals = CategoryColor(fg = Color(0xFFFF8474), tint = Color(0xFF3F1915), stroke = Color(0xFF932C25)),
    labs = CategoryColor(fg = Color(0xFFCA99FF), tint = Color(0xFF2B1F3F), stroke = Color(0xFF623E96)),
    medicalRecords = CategoryColor(fg = Color(0xFF60B9FF), tint = Color(0xFF102743), stroke = Color(0xFF0C54A0)),
    lifestyle = CategoryColor(fg = Color(0xFF4CD676), tint = Color(0xFF0C2E16), stroke = Color(0xFF006925)),
    symptoms = CategoryColor(fg = Color(0xFFFF81C3), tint = Color(0xFF3C1929), stroke = Color(0xFF8B2B5D)),
    treatmentsAndAppliances = CategoryColor(fg = Color(0xFF00D7E4), tint = Color(0xFF002F32), stroke = Color(0xFF006A74)),
    ostomy = CategoryColor(fg = Color(0xFFF2A700), tint = Color(0xFF362200), stroke = Color(0xFF7C4800)),
)

val LocalLauWellColors = staticCompositionLocalOf { LightLauWellColors }
