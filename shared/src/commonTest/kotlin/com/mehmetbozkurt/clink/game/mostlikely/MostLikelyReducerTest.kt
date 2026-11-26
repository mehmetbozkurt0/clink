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
        assertEquals(listOf(1), phase.picked)
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

    // --- Online: the voting phase (spec 2026-11-26-most-likely-to-online §6) ---

    private fun online(library: ContentLibrary = casual) =
        initialMostLikelyState(players, Level.CASUAL, library, Random(0), voting = true)

    private val MostLikelyState.votingPhase: MlPhase.Voting get() = phase as MlPhase.Voting

    /** Votes for the prompt showing now, as the phones do. */
    private fun MostLikelyState.votes(vararg pairs: Pair<Int, Int>): MostLikelyState {
        val id = votingPhase.cardId
        return after(*pairs.map { MlAction.Vote(it.first, it.second, id) }.toTypedArray())
    }

    @Test
    fun aVoteFillsOnlyItsOwnSlot() {
        val state = online().votes(1 to 0)
        assertEquals(listOf(null, 0, null), state.votingPhase.votes)
    }

    @Test
    fun votingAgainReplacesTheFirstVote() {
        val state = online().votes(1 to 0, 1 to 2)
        assertEquals(listOf(null, 2, null), state.votingPhase.votes)
    }

    @Test
    fun theLastVoteRevealsTheResult() {
        val state = online().votes(0 to 1, 1 to 1, 2 to 0)
        val phase = state.phase as MlPhase.ShowingPenalty
        assertEquals(3, phase.counts.sum())
    }

    @Test
    fun theMostVotedTakesThePenalty() {
        val state = online().votes(0 to 0, 1 to 0, 2 to 2)
        val phase = state.phase as MlPhase.ShowingPenalty
        assertEquals(listOf(2, 0, 1), phase.counts)
        assertEquals(listOf(0), phase.picked)
    }

    @Test
    fun everyoneTiedTakesThePenalty() {
        val state = online().votes(0 to 1, 1 to 2, 2 to 0)
        val phase = state.phase as MlPhase.ShowingPenalty
        assertEquals(listOf(0, 1, 2), phase.picked)
    }

    @Test
    fun revealWithNoVotesDrawsTheNextPromptInstead() {
        val first = online()
        val state = first.after(MlAction.Reveal)
        assertTrue(state.phase is MlPhase.Voting)
        assertTrue(state.votingPhase.cardId != first.votingPhase.cardId)
        assertEquals(listOf(0, 0, 0), state.picks)
    }

    @Test
    fun revealClosesTheRoundEarly() {
        val state = online().votes(0 to 1, 1 to 1).after(MlAction.Reveal)
        val phase = state.phase as MlPhase.ShowingPenalty
        assertEquals(listOf(1), phase.picked)
        assertEquals(listOf(0, 2, 0), phase.counts)
    }

    @Test
    fun theTallyCountsPenaltiesNotVotes() {
        val state = online().votes(0 to 0, 1 to 0, 2 to 2)
        assertEquals(listOf(1, 0, 0), state.picks)
    }

    @Test
    fun passFromVotingDropsTheVotes() {
        val first = online()
        val state = first.votes(0 to 1).after(MlAction.Pass)
        assertTrue(state.votingPhase.cardId != first.votingPhase.cardId)
        assertEquals(listOf(null, null, null), state.votingPhase.votes)
    }

    @Test
    fun aVoteForAPromptThatIsGoneIsIgnored() {
        val first = online()
        val stale = MlAction.Vote(1, 2, first.votingPhase.cardId)
        val passed = first.after(MlAction.Pass)
        assertEquals(passed, passed.after(stale))
    }

    @Test
    fun penaltyDoneAfterVotingGoesBackToVoting() {
        val state = online().votes(0 to 1).after(MlAction.Reveal, MlAction.PenaltyDone)
        assertTrue(state.phase is MlPhase.Voting)
    }

    @Test
    fun aVoteDoesNotCountAsANewPhaseForTheTapGuard() {
        // Every vote changes the phase's value; keying the guard on that would re-arm it under the room's fingers.
        val first = MlPhase.Voting("m1", listOf(null, null, null))
        val afterOneVote = MlPhase.Voting("m1", listOf(0, null, null))
        assertEquals(first.tapGuardKey(), afterOneVote.tapGuardKey())
        assertTrue(first.tapGuardKey() != MlPhase.Voting("m2", listOf(null, null, null)).tapGuardKey())
    }

    @Test
    fun closingTheGameShowsTheTallyFirstOnlyWhenThereIsOne() {
        assertFalse(online().endsWithTally())
        val played = online().votes(0 to 1, 1 to 1, 2 to 1)
        assertTrue(played.endsWithTally())
        assertFalse(played.after(MlAction.End).endsWithTally())
    }

    @Test
    fun actionsThatDoNotFitThePhaseChangeNothing() {
        val state = start()
        assertEquals(state, state.after(MlAction.PenaltyDone))
        assertEquals(state, state.after(MlAction.WrongPerson))
    }
}
