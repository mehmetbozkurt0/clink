package com.mehmetbozkurt.clink.ui.theme

import androidx.compose.ui.graphics.Color

/** "Street / Graffiti" palette (design 2026-11-20, §3.1). */
object ClinkColors {
    /** App background. */
    val Wall = Color(0xFF1C1C1F)

    /** Rows, chips and panels on the wall. */
    val WallRaised = Color(0xFF26262A)

    /** Posters (game cards, question and penalty cards); text on dark. */
    val Paper = Color(0xFFF4EFE3)

    /** Text on paper and on neon buttons. */
    val Ink = Color(0xFF141414)

    /** Secondary text on paper (posters). */
    val InkMuted = Color(0xFF5A564D)

    /** Secondary text, hints, inactive icons on the wall. */
    val Muted = Color(0xFFA8A49A)

    /** Primary action, Casual level, highlights. */
    val Acid = Color(0xFFC6FF00)

    /** Secondary accent, "Take penalty", Spicy level. */
    val Pink = Color(0xFFFF2E88)

    /** Hard level, warnings. */
    val Amber = Color(0xFFFFB020)

    /** Masking-tape labels. */
    val Tape = Color(0xFFE8DCB5)
}
