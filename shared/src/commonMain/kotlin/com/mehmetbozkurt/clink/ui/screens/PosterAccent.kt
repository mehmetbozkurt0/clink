package com.mehmetbozkurt.clink.ui.screens

/**
 * Where the highlighted word sits in a home poster's title (spec 2026-11-24-home-poster-text §Design 1), or null when
 * it isn't there. Exact match: callers upper-case both strings with the same locale first.
 */
internal fun accentRange(title: String, accent: String): IntRange? {
    if (accent.isBlank()) return null
    val start = title.indexOf(accent)
    return if (start < 0) null else start until start + accent.length
}
