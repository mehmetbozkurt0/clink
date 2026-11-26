package com.mehmetbozkurt.clink.game.mostlikely

import androidx.lifecycle.ViewModel
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.core.mvi.Store
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Keeps the session alive across configuration changes (e.g. a language switch). */
class MostLikelyViewModel(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
) : ViewModel() {
    private val store = Store(initialMostLikelyState(players, level, library, random), MostLikelyReducer, random)

    val state: StateFlow<MostLikelyState> = store.state

    fun onAction(action: MlAction) = store.dispatch(action)
}
