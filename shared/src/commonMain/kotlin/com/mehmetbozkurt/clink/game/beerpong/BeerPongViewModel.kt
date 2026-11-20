package com.mehmetbozkurt.clink.game.beerpong

import androidx.lifecycle.ViewModel
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.core.mvi.Store
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** One match between the two [players] (in throwing order); survives configuration changes. */
class BeerPongViewModel(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
) : ViewModel() {
    private val store = Store(initialBeerPongState(players, level, library), BeerPongReducer, random)

    val state: StateFlow<BeerPongState> = store.state

    fun onAction(action: BpAction) = store.dispatch(action)
}
