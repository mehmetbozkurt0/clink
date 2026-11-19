package com.mehmetbozkurt.clink.testing

import kotlin.random.Random

/** Test random: each `nextInt(until)` returns the next queued value, so tests can pick exact outcomes. */
class SequenceRandom(vararg values: Int) : Random() {
    private val queue = ArrayDeque(values.toList())

    override fun nextInt(until: Int): Int {
        val value = queue.removeFirstOrNull() ?: error("SequenceRandom ran out of values")
        check(value in 0 until until) { "Queued value $value is not in 0 until $until" }
        return value
    }

    override fun nextBits(bitCount: Int): Int = error("SequenceRandom only supports nextInt(until)")
}
