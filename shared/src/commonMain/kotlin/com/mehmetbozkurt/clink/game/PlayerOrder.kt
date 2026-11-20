package com.mehmetbozkurt.clink.game

/** The item at [from] ends up at [to]; the others keep their order. Out of range → the same list. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
