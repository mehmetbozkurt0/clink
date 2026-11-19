package com.mehmetbozkurt.clink.core.mvi

import kotlin.random.Random

/** Pure game rule: the next state from the current state and an action. All randomness comes from [random]. */
fun interface Reducer<S, A> {
    fun reduce(state: S, action: A, random: Random): S
}
