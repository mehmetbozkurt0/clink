package com.mehmetbozkurt.clink.game.truthordare

import androidx.lifecycle.ViewModel
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.core.mvi.Store
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Keeps the session alive across configuration changes (e.g. a language switch). */
class TruthOrDareViewModel(
    players: List<String>,
    level: Level,
    library: ContentLibrary,
    random: Random,
) : ViewModel() {
    private val store = Store(initialTruthOrDareState(players, level, library), TruthOrDareReducer, random)

    val state: StateFlow<TruthOrDareState> = store.state

    fun onAction(action: TodAction) = store.dispatch(action)
}
