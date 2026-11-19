package com.mehmetbozkurt.clink.core.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** Holds the current state and applies [reducer] to it, synchronously, for every dispatched action. */
class Store<S, A>(initial: S, private val reducer: Reducer<S, A>, private val random: Random) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    fun dispatch(action: A) {
        _state.update { reducer.reduce(it, action, random) }
    }
}
