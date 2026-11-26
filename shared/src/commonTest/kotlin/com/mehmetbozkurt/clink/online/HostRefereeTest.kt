package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.cuppong.CupPongPhysics
import com.mehmetbozkurt.clink.game.cuppong.CpPhase
import com.mehmetbozkurt.clink.game.cuppong.ThrowInput
import com.mehmetbozkurt.clink.game.mostlikely.MlAction
import com.mehmetbozkurt.clink.game.mostlikely.MlPhase
import com.mehmetbozkurt.clink.game.cuppong.aimAt
import com.mehmetbozkurt.clink.game.cuppong.simulateThrow
import com.mehmetbozkurt.clink.game.spinthebottle.SpinAction
import com.mehmetbozkurt.clink.game.spinthebottle.SpinPhase
import com.mehmetbozkurt.clink.game.truthordare.TodAction
import com.mehmetbozkurt.clink.game.truthordare.TodPhase
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostRefereeTest {
    private val library = ContentLibrary(
        listOf(
            Pack("t", CardType.TRUTH, Level.CASUAL, Access.FREE, listOf(Card("truth-1", mapOf("tr" to "Gizli soru", "en" to "Secret question")))),
            Pack("d", CardType.DARE, Level.CASUAL, Access.FREE, listOf(Card("dare-1", mapOf("tr" to "Cesaret", "en" to "Dare")))),
            Pack("p", CardType.PENALTY, Level.CASUAL, Access.FREE, listOf(Card("pen-1", mapOf("tr" to "Ceza", "en" to "Penalty")))),
            Pack(
                "m", CardType.MOST_LIKELY, Level.CASUAL, Access.FREE,
                listOf(Card("ml-1", mapOf("tr" to "Kim?", "en" to "Who?")), Card("ml-2", mapOf("tr" to "Kim ya?", "en" to "Who then?"))),
            ),
        ),
    )
    private val random = Random(7)
    private val a = Member("a", "Ayşe", "cat", 1)
    private val b = Member("b", "Mert", "dog", 2)
    private val c = Member("c", "Can", "fish", 3)
    private val d = Member("d", "Elif", "bird", 4)

    private fun start(game: GameId, vararg players: Member) =
        HostReferee.start(game, Level.CASUAL, players.toList(), library, random, hostUid = players.first().uid)
    private fun SessionState.act(uid: String, action: OnlineAction) = HostReferee.apply(this, uid, action, random)

    // Truth or Dare

    @Test
    fun todOnlyCurrentPlayerMayAct() {
        val s = start(GameId.TRUTH_OR_DARE, a, b, c)
        assertNull(s.act("b", OnlineAction.Tod(TodAction.Pick(CardType.TRUTH))))
        assertIs<TodPhase.ShowingCard>(assertNotNull(s.act("a", OnlineAction.Tod(TodAction.Pick(CardType.TRUTH)))).tod!!.phase)
    }

    @Test
    fun todSpectatorActionIgnored() =
        assertNull(start(GameId.TRUTH_OR_DARE, a, b).act("z", OnlineAction.Tod(TodAction.Pick(CardType.TRUTH))))

    @Test
    fun wrongGameActionIgnored() = assertNull(start(GameId.TRUTH_OR_DARE, a, b).act("a", OnlineAction.Spin(SpinAction.Spin)))

    @Test
    fun todOfflineCurrentPlayerIsSkipped() {
        val s = start(GameId.TRUTH_OR_DARE, a, b, c)
        assertEquals(1, assertNotNull(HostReferee.skipOffline(s, setOf("b", "c"), random)).tod!!.currentPlayer)
    }

    @Test
    fun todOfflinePlayerWithACardIsMovedOn() {
        val shown = assertNotNull(start(GameId.TRUTH_OR_DARE, a, b).act("a", OnlineAction.Tod(TodAction.Pick(CardType.DARE))))
        val skipped = assertNotNull(HostReferee.skipOffline(shown, setOf("b"), random)).tod!!
        assertEquals(1, skipped.currentPlayer)
        assertEquals(TodPhase.Choosing, skipped.phase)
    }

    @Test
    fun nothingToSkipWhenTheCurrentPlayerIsOnline() = assertNull(HostReferee.skipOffline(start(GameId.TRUTH_OR_DARE, a, b), setOf("a"), random))

    // Spin the Bottle

    private fun asking(): SessionState {
        val s = start(GameId.SPIN_THE_BOTTLE, a, b, c)
        return s.copy(spin = s.spin!!.copy(phase = SpinPhase.Asking(asker = 0, answerer = 1)))
    }

    @Test
    fun spinOnlySpinnerSpins() {
        val s = start(GameId.SPIN_THE_BOTTLE, a, b, c)
        assertNull(s.act("b", OnlineAction.Spin(SpinAction.Spin)))
        assertEquals(1, assertNotNull(s.act("a", OnlineAction.Spin(SpinAction.Spin))).spin!!.spinCount)
    }

    @Test
    fun spinOnlyAskerAsks() {
        assertNull(asking().act("b", OnlineAction.AskQuestion("Who?")))
        assertEquals("Who?", assertNotNull(asking().act("a", OnlineAction.AskQuestion("  Who?  "))).spinQuestion)
    }

    @Test
    fun spinBlankQuestionIgnored() = assertNull(asking().act("a", OnlineAction.AskQuestion("   ")))

    @Test
    fun spinAnswerNeedsAQuestionFirst() = assertNull(asking().act("b", OnlineAction.AnswerQuestion("Me")))

    @Test
    fun spinOnlyAnswererAnswersAndThenSpins() {
        val asked = assertNotNull(asking().act("a", OnlineAction.AskQuestion("Who?")))
        assertNull(asked.act("a", OnlineAction.AnswerQuestion("Me")))
        val answered = assertNotNull(asked.act("b", OnlineAction.AnswerQuestion("Me")))
        assertEquals("Me", answered.spinAnswer)
        assertEquals(1, answered.spin!!.spinner)
        assertIs<SpinPhase.ReadyToSpin>(answered.spin!!.phase)
    }

    @Test
    fun spinAnswererMayRefuse() {
        assertIs<SpinPhase.ShowingPenalty>(assertNotNull(asking().act("b", OnlineAction.Spin(SpinAction.Refuse))).spin!!.phase)
    }

    @Test
    fun spinNextSpinClearsTheTexts() {
        val asked = assertNotNull(asking().act("a", OnlineAction.AskQuestion("Who?")))
        val answered = assertNotNull(asked.act("b", OnlineAction.AnswerQuestion("Me")))
        val spun = assertNotNull(answered.act("b", OnlineAction.Spin(SpinAction.Spin)))
        assertNull(spun.spinQuestion)
        assertNull(spun.spinAnswer)
    }

    @Test
    fun spinOfflineSpinnerIsSkipped() {
        val s = start(GameId.SPIN_THE_BOTTLE, a, b, c)
        assertEquals(1, assertNotNull(HostReferee.skipOffline(s, setOf("b", "c"), random)).spin!!.spinner)
    }

    @Test
    fun spinOfflineAnswererIsMovedOn() {
        val skipped = assertNotNull(HostReferee.skipOffline(asking(), setOf("a", "c"), random)).spin!!
        assertIs<SpinPhase.ReadyToSpin>(skipped.phase)
        assertEquals(2, skipped.spinner)
    }

    // Cup Pong

    @Test
    fun cupPongOnlyThrowerThrows() {
        val s = start(GameId.CUP_PONG, a, b)
        val shot = OnlineAction.Throw(ThrowInput(0f, 350f))
        assertNull(s.act("b", shot))
        assertEquals(1, assertNotNull(s.act("a", shot)).cupPong!!.throwCount)
    }

    @Test
    fun cupPongSpectatorCannotThrow() = assertNull(start(GameId.CUP_PONG, a, b).act("c", OnlineAction.Throw(ThrowInput(0f, 350f))))

    @Test
    fun cupPongPausesWhileThrowerOffline() {
        val s = start(GameId.CUP_PONG, a, b)
        assertNull(HostReferee.skipOffline(s, setOf("b"), random))
        assertTrue(isPaused(s, online = setOf("b")))
        assertFalse(isPaused(s, online = setOf("a", "b")))
    }

    @Test
    fun cupPongMatchOverIgnoresThrows() {
        val s = start(GameId.CUP_PONG, a, b)
        val over = s.copy(cupPong = s.cupPong!!.copy(phase = CpPhase.MatchOver(0, 1, null)))
        assertNull(over.act("a", OnlineAction.Throw(ThrowInput(0f, 350f))))
    }

    // Publishing

    @Test
    fun publishesCardIdsNotText() {
        val shown = assertNotNull(start(GameId.TRUTH_OR_DARE, a, b).act("a", OnlineAction.Tod(TodAction.Pick(CardType.TRUTH))))
        val json = encodeSession(shown)
        assertTrue("truth-1" in json)
        assertFalse("Gizli soru" in json || "Secret question" in json)
        assertEquals(shown, decodeSession(json))
    }

    @Test
    fun seqGoesUpOnEveryChange() {
        val s = start(GameId.TRUTH_OR_DARE, a, b)
        assertEquals(s.seq + 1, assertNotNull(s.act("a", OnlineAction.Tod(TodAction.Pick(CardType.DARE)))).seq)
    }

    @Test
    fun actionsRoundTripAsJson() {
        val action = OnlineAction.Throw(ThrowInput(12f, 380f))
        assertEquals(action, decodeAction(encodeAction(action)))
    }
    // A session published by the version before stop angles: no "lastAngle" field.
    @Test
    fun oldSessionWithoutAngleDecodes() {
        val spun = assertNotNull(start(GameId.SPIN_THE_BOTTLE, a, b, c).act("a", OnlineAction.Spin(SpinAction.Spin)))
        assertTrue("lastAngle" in encodeSession(spun))
        val oldJson = encodeSession(spun).replace(Regex(""","lastAngle":[^,}]+"""), "")
        assertFalse("lastAngle" in oldJson)
        val decoded = decodeSession(oldJson).spin!!
        assertNull(decoded.lastAngle)
        assertEquals(spun.spin!!.lastTarget, decoded.lastTarget)
    }
    // Online every phone replays the host's flight, so the path must survive publishing intact.
    @Test
    fun publishedThrowRoundTrips() {
        val s = assertNotNull(start(GameId.CUP_PONG, a, b).act("a", OnlineAction.Throw(aimAt(CupPongPhysics.cups[5]))))
        assertEquals(s, decodeSession(encodeSession(s)))
        assertTrue(s.cupPong!!.lastThrow!!.path.isNotEmpty())
    }

    @Test
    fun longestFlightFitsInPublishedState() {
        // The worst case is a miss that bounces and rolls, not a throw that sinks: a sunk ball stops at the cup.
        val longest = (0..40).maxBy { i ->
            val speed = CupPongPhysics.MIN_SPEED + i * (CupPongPhysics.MAX_SPEED - CupPongPhysics.MIN_SPEED) / 40f
            simulateThrow(ThrowInput(0f, speed), CupPongPhysics.ALL_CUPS).path.size
        }.let { CupPongPhysics.MIN_SPEED + it * (CupPongPhysics.MAX_SPEED - CupPongPhysics.MIN_SPEED) / 40f }
        val s = assertNotNull(start(GameId.CUP_PONG, a, b).act("a", OnlineAction.Throw(ThrowInput(0f, longest))))
        assertTrue(encodeSession(s).length < 12_000, "the longest flight encodes to ${encodeSession(s).length} chars")
    }

    // Most Likely To

    private fun ml(vararg players: Member) = start(GameId.MOST_LIKELY_TO, *players)

    private fun SessionState.voting() = mostLikely!!.phase as MlPhase.Voting

    /** Votes as a phone does: for the prompt showing now, claiming seat 0 so the stamping is visible. */
    private fun SessionState.vote(uid: String, target: Int) =
        act(uid, OnlineAction.MostLikely(MlAction.Vote(0, target, voting().cardId)))

    @Test
    fun mostLikelyStartsInVotingWithASeatPerPlayer() {
        val s = ml(a, b, c)
        assertEquals(listOf(null, null, null), s.voting().votes)
        assertEquals("a", s.hostUid)
    }

    @Test
    fun aVoteIsStampedWithTheSendersSeat() {
        // "b" claims voter 0; the referee must record seat 1 instead.
        val s = assertNotNull(ml(a, b, c).vote("b", 2))
        assertEquals(listOf(null, 2, null), s.voting().votes)
    }

    @Test
    fun onlyTheHostCanReveal() {
        val voted = assertNotNull(ml(a, b, c).vote("b", 2))
        assertNull(voted.act("b", OnlineAction.MostLikely(MlAction.Reveal)))
        assertIs<MlPhase.ShowingPenalty>(assertNotNull(voted.act("a", OnlineAction.MostLikely(MlAction.Reveal))).mostLikely!!.phase)
    }

    @Test
    fun onlyTheHostCanPass() {
        val s = ml(a, b, c)
        assertNull(s.act("b", OnlineAction.MostLikely(MlAction.Pass)))
        assertNotNull(s.act("a", OnlineAction.MostLikely(MlAction.Pass)))
    }

    @Test
    fun aVoteAfterAPassDoesNotCountForTheOldPrompt() {
        val before = ml(a, b, c)
        val stale = before.voting().cardId
        val passed = assertNotNull(before.act("a", OnlineAction.MostLikely(MlAction.Pass)))
        // The vote was sent for the prompt that is gone; it must not land on the new one.
        assertNull(passed.act("b", OnlineAction.MostLikely(MlAction.Vote(1, 2, stale))))
        assertEquals(listOf(null, null, null), passed.voting().votes)
    }

    @Test
    fun theRoundRevealsWhenEveryOnlinePlayerHasVoted() {
        var s = assertNotNull(ml(a, b, c).vote("a", 1))
        s = assertNotNull(s.vote("b", 1))
        // "c" is offline, so the two votes that exist are the whole room.
        val revealed = assertNotNull(HostReferee.skipOffline(s, setOf("a", "b"), random)).mostLikely!!
        assertEquals(listOf(1), (revealed.phase as MlPhase.ShowingPenalty).picked)
    }

    @Test
    fun aVoteFromSomeoneWhoThenDroppedStillCounts() {
        // "c" votes and then drops; "d" never votes and is offline too; "a" and "b" are the whole live room.
        var s = assertNotNull(ml(a, b, c, d).vote("c", 0))
        s = assertNotNull(s.vote("a", 0))
        s = assertNotNull(s.vote("b", 1))
        val revealed = assertNotNull(HostReferee.skipOffline(s, setOf("a", "b"), random)).mostLikely!!
        assertEquals(2, (revealed.phase as MlPhase.ShowingPenalty).counts[0])
    }

    @Test
    fun aPenaltyNobodyPresentTakesAdvancesByItself() {
        var s = assertNotNull(ml(a, b, c).vote("c", 2))
        s = assertNotNull(s.act("a", OnlineAction.MostLikely(MlAction.Reveal)))
        assertEquals(listOf(2), (s.mostLikely!!.phase as MlPhase.ShowingPenalty).picked)
        val moved = assertNotNull(HostReferee.skipOffline(s, setOf("a", "b"), random)).mostLikely!!
        assertIs<MlPhase.Voting>(moved.phase)
    }
}
