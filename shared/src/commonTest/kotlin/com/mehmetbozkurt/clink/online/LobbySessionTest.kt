package com.mehmetbozkurt.clink.online

import com.mehmetbozkurt.clink.content.Access
import com.mehmetbozkurt.clink.content.Card
import com.mehmetbozkurt.clink.content.CardType
import com.mehmetbozkurt.clink.content.ContentLibrary
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.content.Pack
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.truthordare.TodAction
import com.mehmetbozkurt.clink.game.truthordare.TodPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LobbySessionTest {
    private val library = ContentLibrary(
        listOf(
            Pack("t", CardType.TRUTH, Level.CASUAL, Access.FREE, listOf(Card("truth-1", mapOf("tr" to "S", "en" to "Q")))),
            Pack("d", CardType.DARE, Level.CASUAL, Access.FREE, listOf(Card("dare-1", mapOf("tr" to "C", "en" to "D")))),
            Pack("p", CardType.PENALTY, Level.CASUAL, Access.FREE, listOf(Card("pen-1", mapOf("tr" to "Ce", "en" to "P")))),
        ),
    )
    private val host = Member("a", "Ayşe", "cat", 1)
    private val guest = Member("b", "Mert", "dog", 2)

    private var time = 10_000L

    private fun TestScope.launcher(repo: LobbyRepository, codes: List<String> = listOf("ABC234")): LobbyLauncher {
        val queue = ArrayDeque(codes)
        // Not backgroundScope: advanceUntilIdle() stops once only background work is left, so the lobby would never update.
        val lobbyScope = CoroutineScope(coroutineContext + Job())
        return LobbyLauncher(repo, library, Random(3), lobbyScope, clock = { time }, newCode = { queue.removeFirst() })
    }

    @Test
    fun createdLobbyHasTheHostAsMember() = runTest {
        val repo = FakeLobbyRepository()
        val session = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        assertEquals("ABC234", session.code)
        assertEquals(listOf("a"), session.snapshot.value?.members?.map { it.uid })
    }

    @Test
    fun takenCodeIsReplaced() = runTest {
        val repo = FakeLobbyRepository()
        launcher(repo, listOf("ABC234")).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL)
        val second = assertNotNull(launcher(repo, listOf("ABC234", "XYZ789")).create(Member("x", "X", "cat", 1), GameId.TRUTH_OR_DARE, Level.CASUAL))
        assertEquals("XYZ789", second.code)
    }

    @Test
    fun joiningWithAnUnknownCodeFails() = runTest {
        assertEquals(JoinResult.NotFound, launcher(FakeLobbyRepository()).join("QQQ234", guest))
    }

    @Test
    fun hostAppliesActionsAndPublishes() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        advanceUntilIdle()
        assertTrue(hostSession.startGame())
        advanceUntilIdle()
        // Seat 0 is the host (joined first); the guest can't act yet, the host can.
        guestSession.send(OnlineAction.Tod(TodAction.Pick(CardType.TRUTH)))
        advanceUntilIdle()
        assertEquals(TodPhase.Choosing, repo.session(hostSession.lobbyId)!!.tod!!.phase)
        hostSession.send(OnlineAction.Tod(TodAction.Pick(CardType.TRUTH)))
        advanceUntilIdle()
        assertIs<TodPhase.ShowingCard>(repo.session(hostSession.lobbyId)!!.tod!!.phase)
        assertIs<TodPhase.ShowingCard>(guestSession.snapshot.value?.session?.tod?.phase)
    }

    @Test
    fun nonHostNeverPublishes() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        advanceUntilIdle()
        assertFalse(guestSession.startGame())
        advanceUntilIdle()
        assertEquals(0, repo.publishCount)
        assertNull(repo.session(hostSession.lobbyId))
    }

    @Test
    fun startNeedsTwoPlayers() = runTest {
        val hostSession = assertNotNull(launcher(FakeLobbyRepository()).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        assertFalse(hostSession.startGame())
    }

    @Test
    fun offlinePlayerIsSkippedByTheHost() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        launcher(repo).join("ABC234", guest)
        launcher(repo).join("ABC234", Member("c", "Can", "fish", 3))
        advanceUntilIdle()
        hostSession.startGame()
        advanceUntilIdle()
        hostSession.send(OnlineAction.Tod(TodAction.Pick(CardType.DARE)))
        advanceUntilIdle()
        hostSession.send(OnlineAction.Tod(TodAction.Done))
        advanceUntilIdle()
        assertEquals(1, repo.session(hostSession.lobbyId)!!.tod!!.currentPlayer)
        repo.setOnline(hostSession.lobbyId, "b", false)
        advanceUntilIdle()
        assertEquals(2, repo.session(hostSession.lobbyId)!!.tod!!.currentPlayer)
    }

    @Test
    fun endingAGameReturnsToTheLobby() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        launcher(repo).join("ABC234", guest)
        advanceUntilIdle()
        hostSession.startGame()
        advanceUntilIdle()
        assertEquals(LobbyStatus.PLAYING, repo.meta(hostSession.lobbyId)!!.status)
        hostSession.endGame()
        advanceUntilIdle()
        assertEquals(LobbyStatus.LOBBY, repo.meta(hostSession.lobbyId)!!.status)
        assertNull(repo.session(hostSession.lobbyId))
    }

    @Test
    fun leavingAsHostClosesTheLobby() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        hostSession.leave()
        advanceUntilIdle()
        assertEquals(LobbyStatus.CLOSED, repo.meta(hostSession.lobbyId)!!.status)
        assertFalse(repo.codeExists("ABC234"))
    }

    @Test
    fun removedMemberCannotRejoin() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        launcher(repo).join("ABC234", guest)
        advanceUntilIdle()
        hostSession.removePlayer("b")
        advanceUntilIdle()
        assertEquals(JoinResult.Refused, launcher(repo).join("ABC234", guest))
    }

    @Test
    fun aThirteenthPlayerIsTurnedAway() = runTest {
        val repo = FakeLobbyRepository()
        assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        (2..12).forEach { launcher(repo).join("ABC234", Member("p$it", "P$it", "cat", it.toLong())) }
        advanceUntilIdle()
        assertEquals(JoinResult.Full, launcher(repo).join("ABC234", Member("late", "Late", "cat", 99)))
    }

    @Test
    fun chatIsSharedAndTrimmed() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        advanceUntilIdle()
        guestSession.sendChat("  merhaba  ")
        guestSession.sendChat("   ")
        guestSession.sendChat("x".repeat(400))
        advanceUntilIdle()
        val texts = hostSession.chat.value.map { it.text }
        assertEquals(listOf("merhaba", "x".repeat(CHAT_MAX_LENGTH)), texts)
    }

    @Test
    fun removedMemberSeesTheyWereRemoved() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        advanceUntilIdle()
        assertNull(guestSession.exit.value)
        hostSession.removePlayer("b")
        advanceUntilIdle()
        assertEquals(LobbyExit.REMOVED, guestSession.exit.value)
    }

    @Test
    fun membersSeeAClosedLobby() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        advanceUntilIdle()
        hostSession.leave()
        advanceUntilIdle()
        assertEquals(LobbyExit.CLOSED, guestSession.exit.value)
    }

    @Test
    fun hostAwayLongerThanAMinuteClosesTheLobby() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        repo.isConnected.value = false
        advanceUntilIdle()
        time += HOST_TIMEOUT_MILLIS
        repo.isConnected.value = true
        advanceUntilIdle()
        assertEquals(LobbyStatus.CLOSED, repo.meta(hostSession.lobbyId)!!.status)
    }

    @Test
    fun shortHostOutageKeepsTheLobby() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        repo.isConnected.value = false
        advanceUntilIdle()
        time += 5_000
        repo.isConnected.value = true
        advanceUntilIdle()
        assertEquals(LobbyStatus.LOBBY, repo.meta(hostSession.lobbyId)!!.status)
    }

    @Test
    fun guestSeesTheHostReconnectingCountdown() = runTest {
        val repo = FakeLobbyRepository()
        val hostSession = assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestSession = assertIs<JoinResult.Joined>(launcher(repo).join("ABC234", guest)).session
        repo.setHostOnline(hostSession.lobbyId, online = false, seenAt = time - 15_000)
        advanceUntilIdle()
        assertEquals(HostStatus.Reconnecting(45), guestSession.hostStatus(guestSession.snapshot.value!!))
    }

    /** A database that never answers (no connection, or no database at all). */
    private class SilentRepository(private val real: FakeLobbyRepository = FakeLobbyRepository()) : LobbyRepository by real {
        override suspend fun resolve(code: String): CodeInfo? = awaitCancellation()
    }

    @Test
    fun peekingAnUnknownCodeFindsNothing() = runTest {
        assertEquals(Peek.NotFound, launcher(FakeLobbyRepository()).peek("QQQ234"))
    }

    @Test
    fun peekingWithoutAConnectionFails() = runTest {
        assertEquals(Peek.Failed, launcher(SilentRepository()).peek("QQQ234"))
    }

    @Test
    fun joiningWithoutAConnectionFails() = runTest {
        assertEquals(JoinResult.Failed, launcher(SilentRepository()).join("QQQ234", guest))
    }

    @Test
    fun peekingAnOpenLobbyShowsItsHost() = runTest {
        val repo = FakeLobbyRepository()
        launcher(repo).create(host, GameId.SPIN_THE_BOTTLE, Level.HARD)
        val found = assertIs<Peek.Found>(launcher(repo).peek("abc-234"))
        assertEquals("Ayşe", found.info.hostName)
        assertEquals(GameId.SPIN_THE_BOTTLE, found.info.game)
    }

    // Review I2: tapping the invite link again must not throw you out of the lobby you're in.
    @Test
    fun joiningTheLobbyYouAreInKeepsYourSession() = runTest {
        val repo = FakeLobbyRepository()
        assertNotNull(launcher(repo).create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        val guestLauncher = launcher(repo)
        val first = assertIs<JoinResult.Joined>(guestLauncher.join("ABC234", guest)).session
        advanceUntilIdle()
        val again = assertIs<JoinResult.Joined>(guestLauncher.join("ABC234", guest)).session
        advanceUntilIdle()
        assertTrue(first === again)
        assertNull(again.exit.value)
    }

    @Test
    fun hostOpeningItsOwnInviteKeepsTheLobbyOpen() = runTest {
        val repo = FakeLobbyRepository()
        val hostLauncher = launcher(repo)
        val hostSession = assertNotNull(hostLauncher.create(host, GameId.TRUTH_OR_DARE, Level.CASUAL))
        advanceUntilIdle()
        assertIs<JoinResult.Joined>(hostLauncher.join("ABC234", host))
        advanceUntilIdle()
        assertEquals(LobbyStatus.LOBBY, repo.meta(hostSession.lobbyId)!!.status)
    }
}