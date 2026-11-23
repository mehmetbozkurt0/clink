package com.mehmetbozkurt.clink.ui.onboarding

/** How a tour ended; every one of them counts as "seen" for the caller. */
enum class TourExit { COMPLETED, SKIPPED, BACKED_OUT }

/**
 * The pager's logic without any Compose, so it is unit-testable. [exit] stays null while the tour is running;
 * once set it is final (every move is ignored), so the caller sees exactly one exit.
 */
data class TourState(val page: Int, val pageCount: Int, val exit: TourExit? = null) {
    /** Next card; "next" on the last card finishes the tour. */
    fun next(): TourState =
        if (exit != null) this else if (page >= pageCount - 1) copy(exit = TourExit.COMPLETED) else copy(page = page + 1)

    /** Previous card; back on the first card leaves without continuing into the flow. */
    fun back(): TourState =
        if (exit != null) this else if (page <= 0) copy(exit = TourExit.BACKED_OUT) else copy(page = page - 1)

    fun skip(): TourState = if (exit != null) this else copy(exit = TourExit.SKIPPED)

    /** A swipe landed on [page]; clamped so a stray index can't leave the cards. */
    fun goTo(page: Int): TourState = if (exit != null) this else copy(page = page.coerceIn(0, pageCount - 1))
}
