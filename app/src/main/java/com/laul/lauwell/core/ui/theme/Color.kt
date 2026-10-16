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
 * App color tokens from the locked design (direction 13b, token sheet in the local
 * `docs/design/LauWell Final Design (for Figma import).html`; values recorded in PLAN.md).
 * Not modeled on Material3's ColorScheme: the primary is near-black [ink]/[accent], not a hue.
 *
 * Category colors are derived in OKLCH from one hue per category (fg L 0.50 / C 0.11, tint
 * L 0.92 / C 0.05, stroke L 0.80 / C 0.077; dark: fg L 0.80 / C 0.094, tint L 0.27 / C 0.06,
 * stroke L 0.45 / C 0.077). Look them up with [forCategory] rather than the fields.
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
    val vitals: CategoryColor,
    val labs: CategoryColor,
    val medicalRecords: CategoryColor,
    val lifestyle: CategoryColor,
    val symptoms: CategoryColor,
    val treatmentsAndAppliances: CategoryColor,
) {
    /** The single category → color lookup. Labs gets its entry when it joins [Category]. */
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
    vitals = CategoryColor(fg = Color(0xFF984742), tint = Color(0xFFFFD8D4), stroke = Color(0xFFEBABA5)),
    labs = CategoryColor(fg = Color(0xFF6D5398), tint = Color(0xFFE9DEFF), stroke = Color(0xFFC5B3E9)),
    medicalRecords = CategoryColor(fg = Color(0xFF1F689D), tint = Color(0xFFCAE9FF), stroke = Color(0xFF94C4ED)),
    lifestyle = CategoryColor(fg = Color(0xFF267543), tint = Color(0xFFCDEFD5), stroke = Color(0xFF99CDA6)),
    symptoms = CategoryColor(fg = Color(0xFF895700), tint = Color(0xFFFAE0C1), stroke = Color(0xFFDDB687)),
    treatmentsAndAppliances = CategoryColor(fg = Color(0xFF007677), tint = Color(0xFFBFF0EF), stroke = Color(0xFF80CECD)),
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
    vitals = CategoryColor(fg = Color(0xFFF4A69F), tint = Color(0xFF3F1917), stroke = Color(0xFF7A433F)),
    labs = CategoryColor(fg = Color(0xFFC7B0F2), tint = Color(0xFF2B1F3F), stroke = Color(0xFF5C4B7A)),
    medicalRecords = CategoryColor(fg = Color(0xFF8AC4F6), tint = Color(0xFF062942), stroke = Color(0xFF2D597D)),
    lifestyle = CategoryColor(fg = Color(0xFF90CFA0), tint = Color(0xFF0A2F17), stroke = Color(0xFF316140)),
    symptoms = CategoryColor(fg = Color(0xFFE3B479), tint = Color(0xFF382100), stroke = Color(0xFF704E1F)),
    treatmentsAndAppliances = CategoryColor(fg = Color(0xFF6DD1D0), tint = Color(0xFF002F2F), stroke = Color(0xFF006262)),
)

val LocalLauWellColors = staticCompositionLocalOf { LightLauWellColors }
