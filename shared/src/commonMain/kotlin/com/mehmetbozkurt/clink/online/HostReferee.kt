package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.cuppong.CupPongReducer
import com.mehmetbozkurt.clink.game.cuppong.CpAction
import com.mehmetbozkurt.clink.game.cuppong.CpPhase
import com.mehmetbozkurt.clink.game.cuppong.initialCupPongState
import com.mehmetbozkurt.clink.game.spinthebottle.SpinAction
import com.mehmetbozkurt.clink.game.spinthebottle.SpinPhase
import com.mehmetbozkurt.clink.game.spinthebottle.SpinTheBottleReducer
import com.mehmetbozkurt.clink.game.spinthebottle.initialSpinState
import com.mehmetbozkurt.clink.game.truthordare.TodAction
import com.mehmetbozkurt.clink.game.truthordare.TodPhase
import com.mehmetbozkurt.clink.game.truthordare.TruthOrDareReducer
import com.mehmetbozkurt.clink.game.truthordare.initialTruthOrDareState
import kotlin.random.Random

/** Spin the Bottle's typed question and answer are short lines on a card. */
const val SPIN_TEXT_MAX_LENGTH = 200

/**
 * The rules engine on the host's phone (spec §10.1, §10.4): only it runs the game reducers. It accepts an action only
 * from the member whose turn it is (or whose role it is), moves past players who are offline, and returns the next
 * state to publish — or null when nothing changes. Pure, so it can move to a server later (§8).
 */
object HostReferee {

    /** A new game for [players] in seat order; for Cup Pong exactly the two match players. */
    fun start(game: GameId, level: Level, players: List<Member>, library: ContentLibrary, random: Random, seq: Int = 1): SessionState {
        val names = players.map { it.name }
        val uids = players.map { it.uid }
        return when (game) {
            GameId.TRUTH_OR_DARE -> SessionState(seq, game, level, uids, tod = initialTruthOrDareState(names, level, library))
            GameId.SPIN_THE_BOTTLE -> SessionState(seq, game, level, uids, spin = initialSpinState(names, level, library))
            GameId.CUP_PONG -> {
                require(players.size == 2) { "Cup Pong is 1v1" }
                SessionState(seq, game, level, uids, cupPong = initialCupPongState(names, level, library))
            }
        }
    }

    fun apply(session: SessionState, uid: String, action: OnlineAction, random: Random): SessionState? {
        val seat = session.playerUids.indexOf(uid).takeIf { it >= 0 } ?: return null
        val next = when (action) {
            is OnlineAction.Tod -> todAction(session, seat, action.action, random)
            is OnlineAction.Spin -> spinAction(session, seat, action.action, random)
            is OnlineAction.AskQuestion -> askQuestion(session, seat, action.text)
            is OnlineAction.AnswerQuestion -> answerQuestion(session, seat, action.text, random)
            is OnlineAction.Throw -> throwBall(session, seat, action, random)
        }
        return next?.takeIf { it != session }?.copy(seq = session.seq + 1)
    }

    /**
     * Moves the game past a player who has to act but is offline (spec §5.2: they are skipped). Truth or Dare and Spin
     * go on with the next online player; a Cup Pong match waits for its thrower ([isPaused]).
     */
    fun skipOffline(session: SessionState, online: Set<String>, random: Random): SessionState? {
        if (session.playerUids.none { it in online }) return null
        var next = session
        repeat(session.playerUids.size * 3) {
            val moved = skipOnce(next, online, random) ?: return@repeat
            next = moved
        }
        return next.takeIf { it != session }?.copy(seq = session.seq + 1)
    }

    private fun skipOnce(session: SessionState, online: Set<String>, random: Random): SessionState? {
        fun offline(seat: Int) = session.playerUids[seat] !in online
        session.tod?.let { tod ->
            if (!offline(tod.currentPlayer)) return null
            return when (tod.phase) {
                TodPhase.Choosing -> session.copy(tod = tod.copy(currentPlayer = nextOnline(session, tod.currentPlayer, online)))
                is TodPhase.ShowingCard -> session.copy(tod = TruthOrDareReducer.reduce(tod, TodAction.Done, random))
                is TodPhase.ShowingPenalty -> session.copy(tod = TruthOrDareReducer.reduce(tod, TodAction.PenaltyDone, random))
            }
        }
        session.spin?.let { spin ->
            return when (val phase = spin.phase) {
                is SpinPhase.ReadyToSpin ->
                    if (offline(spin.spinner)) session.copy(spin = spin.copy(spinner = nextOnline(session, spin.spinner, online))) else null
                // Whoever of the two is gone, the round can't finish: it ends as answered, like a skipped turn.
                is SpinPhase.Asking ->
                    if (offline(phase.asker) || offline(phase.answerer)) {
                        session.copy(spin = SpinTheBottleReducer.reduce(spin, SpinAction.Answered, random), spinQuestion = null, spinAnswer = null)
                    } else {
                        null
                    }
                is SpinPhase.ShowingPenalty ->
                    if (offline(phase.answerer)) session.copy(spin = SpinTheBottleReducer.reduce(spin, SpinAction.PenaltyDone, random)) else null
            }
        }
        return null
    }

    private fun nextOnline(session: SessionState, from: Int, online: Set<String>): Int {
        val n = session.playerUids.size
        return (1..n).map { (from + it) % n }.firstOrNull { session.playerUids[it] in online } ?: from
    }

    private fun todAction(session: SessionState, seat: Int, action: TodAction, random: Random): SessionState? {
        val tod = session.tod ?: return null
        if (seat != tod.currentPlayer) return null
        return session.copy(tod = TruthOrDareReducer.reduce(tod, action, random))
    }

    private fun spinAction(session: SessionState, seat: Int, action: SpinAction, random: Random): SessionState? {
        val spin = session.spin ?: return null
        val allowed = when (val phase = spin.phase) {
            is SpinPhase.ReadyToSpin -> action == SpinAction.Spin && seat == spin.spinner
            // Online the answer is typed (AnswerQuestion); refusing works with or without a question.
            is SpinPhase.Asking -> action == SpinAction.Refuse && seat == phase.answerer
            is SpinPhase.ShowingPenalty -> action == SpinAction.PenaltyDone && seat == phase.answerer
        }
        if (!allowed) return null
        val next = session.copy(spin = SpinTheBottleReducer.reduce(spin, action, random))
        return if (action == SpinAction.Spin) next.copy(spinQuestion = null, spinAnswer = null) else next
    }

    private fun askQuestion(session: SessionState, seat: Int, text: String): SessionState? {
        val phase = session.spin?.phase as? SpinPhase.Asking ?: return null
        val question = text.trim().take(SPIN_TEXT_MAX_LENGTH)
        if (seat != phase.asker || session.spinQuestion != null || question.isEmpty()) return null
        return session.copy(spinQuestion = question)
    }

    private fun answerQuestion(session: SessionState, seat: Int, text: String, random: Random): SessionState? {
        val spin = session.spin ?: return null
        val phase = spin.phase as? SpinPhase.Asking ?: return null
        val answer = text.trim().take(SPIN_TEXT_MAX_LENGTH)
        if (seat != phase.answerer || session.spinQuestion == null || answer.isEmpty()) return null
        return session.copy(spin = SpinTheBottleReducer.reduce(spin, SpinAction.Answered, random), spinAnswer = answer)
    }

    private fun throwBall(session: SessionState, seat: Int, action: OnlineAction.Throw, random: Random): SessionState? {
        val match = session.cupPong ?: return null
        if (match.phase != CpPhase.Aiming || seat != match.thrower) return null
        return session.copy(cupPong = CupPongReducer.reduce(match, CpAction.Throw(action.input), random))
    }
}

/** A Cup Pong match waits while its thrower is offline (1v1: there is no one to skip to). */
fun isPaused(session: SessionState, online: Set<String>): Boolean {
    val match = session.cupPong ?: return false
    return match.phase == CpPhase.Aiming && session.playerUids[match.thrower] !in online
}
