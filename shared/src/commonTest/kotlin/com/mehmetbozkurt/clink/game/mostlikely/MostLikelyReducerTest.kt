package com.mehmetbozkurt.clink.game.mostlikely

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MostLikelyReducerTest {
    private val players = listOf("Ayşe", "Mert", "Can")

    private fun pack(id: String, type: CardType, level: Level, vararg cardIds: String) =
        Pack(id, type, level, Access.FREE, cardIds.map { Card(it, mapOf("en" to it)) })

    private val casual = ContentLibrary(
        listOf(
            pack("m", CardType.MOST_LIKELY, Level.CASUAL, "m1", "m2", "m3"),
            pack("p", CardType.PENALTY, Level.CASUAL, "p1", "p2"),
        ),
    )

    /** Two prompts and no penalties at all. */
    private val promptsOnly = ContentLibrary(listOf(pack("m", CardType.MOST_LIKELY, Level.CASUAL, "m1", "m2")))

    private val noPrompts = ContentLibrary(listOf(pack("p", CardType.PENALTY, Level.CASUAL, "p1")))

    private fun MostLikelyState.after(vararg actions: MlAction, random: Random = Random(0)) =
        actions.fold(this) { state, action -> MostLikelyReducer.reduce(state, action, random) }

    private fun start(library: ContentLibrary = casual) =
        initialMostLikelyState(players, Level.CASUAL, library, Random(0))

    private val MostLikelyState.prompt: MlPhase.ShowingPrompt get() = phase as MlPhase.ShowingPrompt

    @Test
    fun pickingShowsAPenaltyAndCountsThePlayer() {
        val state = start().after(MlAction.Pick(1))
        val phase = state.phase as MlPhase.ShowingPenalty
        assertEquals(1, phase.picked)
        assertTrue(phase.cardId in listOf("p1", "p2"))
        assertEquals(listOf(0, 1, 0), state.picks)
    }

    @Test
    fun passingDrawsANewPromptAndCountsNobody() {
        val first = start()
        val state = first.after(MlAction.Pass)
        assertTrue(state.prompt.cardId != first.prompt.cardId)
        assertEquals(listOf(0, 0, 0), state.picks)
        assertFalse(state.hasPicks)
    }

    @Test
    fun passingNeverRepeatsThePromptJustShown() {
        var state = start(promptsOnly)
        val seen = mutableListOf(state.prompt.cardId)
        repeat(6) {
            state = state.after(MlAction.Pass, random = Random(it))
            seen += state.prompt.cardId
        }
        seen.zipWithNext().forEach { (a, b) -> assertTrue(a != b, "repeated $a") }
    }

    @Test
    fun wrongPersonGoesBackToTheSamePromptAndUncounts() {
        val first = start()
        val state = first.after(MlAction.Pick(0), MlAction.WrongPerson)
        assertEquals(first.prompt.cardId, state.prompt.cardId)
        assertEquals(listOf(0, 0, 0), state.picks)
        assertFalse(state.hasPicks)
    }

    @Test
    fun theTallySurvivesAnUndoBeforeTheEnd() {
        val state = start().after(
            MlAction.Pick(2),
            MlAction.PenaltyDone,
            MlAction.Pick(0),
            MlAction.WrongPerson,
            MlAction.End,
        )
        assertEquals(MlPhase.Summary, state.phase)
        assertEquals(listOf(0, 0, 1), state.picks)
        assertTrue(state.hasPicks)
    }

    @Test
    fun penaltyDoneDrawsTheNextPrompt() {
        val state = start().after(MlAction.Pick(1), MlAction.PenaltyDone)
        assertTrue(state.phase is MlPhase.ShowingPrompt)
    }

    @Test
    fun aLevelWithNoPenaltyCardsStillFinishesTheRound() {
        val state = start(promptsOnly).after(MlAction.Pick(1))
        val phase = state.phase as MlPhase.ShowingPenalty
        assertNull(phase.cardId)
        assertEquals(listOf(0, 1, 0), state.picks)
        assertTrue(state.after(MlAction.PenaltyDone).phase is MlPhase.ShowingPrompt)
    }

    @Test
    fun aLevelWithNoPromptsCannotStart() {
        assertEquals(MlPhase.NoPrompts, start(noPrompts).phase)
    }

    @Test
    fun longPlayerListsGetSmallerChips() {
        assertFalse(compactChips(3))
        assertFalse(compactChips(8))
        assertTrue(compactChips(9))
        assertTrue(compactChips(12))
    }

    @Test
    fun actionsThatDoNotFitThePhaseChangeNothing() {
        val state = start()
        assertEquals(state, state.after(MlAction.PenaltyDone))
        assertEquals(state, state.after(MlAction.WrongPerson))
    }
}
