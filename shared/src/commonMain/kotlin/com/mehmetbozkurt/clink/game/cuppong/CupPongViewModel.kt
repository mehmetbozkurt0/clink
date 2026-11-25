package com.mehmetbozkurt.clink.game.cuppong

import androidx.lifecycle.ViewModel
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.core.mvi.Store
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** One match between the two [players] (in throwing order); survives configuration changes. */
class CupPongViewModel(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
) : ViewModel() {
    private val store = Store(initialCupPongState(players, level, library), CupPongReducer, random)

    val state: StateFlow<CupPongState> = store.state

    fun onAction(action: CpAction) = store.dispatch(action)
}
