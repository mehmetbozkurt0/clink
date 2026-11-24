package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.beerpong.BpPhase
import com.mehmetbozkurt.clink.game.beerpong.ThrowInput
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
        ),
    )
    private val random = Random(7)
    private val a = Member("a", "Ayşe", "cat", 1)
    private val b = Member("b", "Mert", "dog", 2)
    private val c = Member("c", "Can", "fish", 3)

    private fun start(game: GameId, vararg players: Member) = HostReferee.start(game, Level.CASUAL, players.toList(), library, random)
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

    // Beer Pong

    @Test
    fun beerPongOnlyThrowerThrows() {
        val s = start(GameId.BEER_PONG, a, b)
        val shot = OnlineAction.Throw(ThrowInput(0f, 90f))
        assertNull(s.act("b", shot))
        assertEquals(1, assertNotNull(s.act("a", shot)).beerPong!!.throwCount)
    }

    @Test
    fun beerPongSpectatorCannotThrow() = assertNull(start(GameId.BEER_PONG, a, b).act("c", OnlineAction.Throw(ThrowInput(0f, 90f))))

    @Test
    fun beerPongPausesWhileThrowerOffline() {
        val s = start(GameId.BEER_PONG, a, b)
        assertNull(HostReferee.skipOffline(s, setOf("b"), random))
        assertTrue(isPaused(s, online = setOf("b")))
        assertFalse(isPaused(s, online = setOf("a", "b")))
    }

    @Test
    fun beerPongMatchOverIgnoresThrows() {
        val s = start(GameId.BEER_PONG, a, b)
        val over = s.copy(beerPong = s.beerPong!!.copy(phase = BpPhase.MatchOver(0, 1, null)))
        assertNull(over.act("a", OnlineAction.Throw(ThrowInput(0f, 90f))))
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
        val action = OnlineAction.Throw(ThrowInput(12f, 80f))
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
}