package com.mehmetbozkurt.clink.game.spinthebottle

import androidx.lifecycle.ViewModel
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.core.mvi.Store
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Keeps the session alive across configuration changes (e.g. a language switch). */
class SpinTheBottleViewModel(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
) : ViewModel() {
    private val store = Store(initialSpinState(players, level, library), SpinTheBottleReducer, random)

    val state: StateFlow<SpinState> = store.state

    fun onAction(action: SpinAction) = store.dispatch(action)
}
