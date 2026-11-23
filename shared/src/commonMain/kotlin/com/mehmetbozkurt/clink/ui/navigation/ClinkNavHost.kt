package com.mehmetbozkurt.clink.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mehmetbozkurt.clink.LocalAppContainer
import com.mehmetbozkurt.clink.account.AccountSession
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.PlayGate
import com.mehmetbozkurt.clink.account.suggestedName
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.beerpong.BeerPongScreen
import com.mehmetbozkurt.clink.game.beerpong.BeerPongSetupScreen
import com.mehmetbozkurt.clink.game.beerpong.suggestedMatchup
import com.mehmetbozkurt.clink.game.spinthebottle.SpinTheBottleScreen
import com.mehmetbozkurt.clink.game.truthordare.TruthOrDareScreen
import com.mehmetbozkurt.clink.online.Member
import com.mehmetbozkurt.clink.online.codeFromLink
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cancel
import com.mehmetbozkurt.clink.resources.done_button
import com.mehmetbozkurt.clink.resources.err_lobby_create
import com.mehmetbozkurt.clink.resources.start_button
import com.mehmetbozkurt.clink.resources.try_again
import com.mehmetbozkurt.clink.ui.components.PosterDialog
import com.mehmetbozkurt.clink.ui.online.JoinScreen
import com.mehmetbozkurt.clink.ui.online.OnlineRoomScreen
import com.mehmetbozkurt.clink.ui.onboarding.OnboardingPager
import com.mehmetbozkurt.clink.ui.onboarding.TourExit
import com.mehmetbozkurt.clink.ui.onboarding.playTourCards
import com.mehmetbozkurt.clink.ui.screens.AccountScreen
import com.mehmetbozkurt.clink.ui.screens.GameListScreen
import com.mehmetbozkurt.clink.ui.screens.GameOverScreen
import com.mehmetbozkurt.clink.ui.screens.LevelSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ModeSelectScreen
import com.mehmetbozkurt.clink.ui.screens.PlayerSetupScreen
import com.mehmetbozkurt.clink.ui.screens.ProfileSetupScreen
import com.mehmetbozkurt.clink.ui.screens.SignInScreen
import com.mehmetbozkurt.clink.ui.screens.WelcomeScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val TransitionMillis = 250

@Composable
fun ClinkNavHost(navController: NavHostController = rememberNavController()) {
    /** [matchWinner]/[matchLoser]: Beer Pong's previous match, so the winner stays on. */
    fun startGame(
        game: GameId,
        players: List<String>,
        level: Level,
        matchWinner: Int? = null,
        matchLoser: Int? = null,
        options: NavOptionsBuilder.() -> Unit = {},
    ) {
        val route: Any = when (game) {
            GameId.TRUTH_OR_DARE -> TruthOrDareRoute(players, level)
            GameId.SPIN_THE_BOTTLE -> SpinTheBottleRoute(players, level)
            GameId.BEER_PONG -> if (players.size == 2) {
                val matchup = suggestedMatchup(2, matchWinner, matchLoser)
                BeerPongRoute(players, level, matchup.first, matchup.second)
            } else {
                BeerPongSetupRoute(players, level, matchWinner, matchLoser)
            }
        }
        navController.navigate(route, options)
    }

    // The on-screen ‹ does exactly what system back does: pop this screen.
    fun NavBackStackEntry.back() = ifResumed { navController.popBackStack() }

    // The host and their profile (spec §4: the host needs an account). Loaded when the user changes, so the
    // Welcome badge shows up; Play asks the session where to go.
    val container = LocalAppContainer.current
    val session = remember(container) { AccountSession(container.auth, container.profiles) }
    val scope = rememberCoroutineScope()
    val user by container.auth.user.collectAsState()
    LaunchedEffect(user?.uid) { session.gate() }
    var gating by remember { mutableStateOf<Job?>(null) }

    // An invite link (spec §5.1) opens the join screen with its code, whatever screen is showing.
    val link by container.incomingLinks.link.collectAsState()
    LaunchedEffect(link) {
        val url = link ?: return@LaunchedEffect
        container.incomingLinks.consume()
        val code = codeFromLink(url) ?: return@LaunchedEffect
        // The link of the lobby this phone is already in just brings that lobby back.
        if (code == container.lobbies.active.value?.code) {
            navController.popBackStack(OnlineRoomRoute, inclusive = false)
        } else {
            navController.navigate(JoinRoute(code))
        }
    }

    /** Opens what [AccountSession.gate] decides; [popUpTo] removes the screen that asked (sign-in, setup). */
    fun goByGate(popUpTo: (NavOptionsBuilder.() -> Unit) = {}) {
        if (gating?.isActive == true) return
        gating = scope.launch {
            when (session.gate()) {
                PlayGate.SignIn -> navController.navigate(SignInRoute, popUpTo)
                PlayGate.SetUpProfile -> navController.navigate(ProfileSetupRoute(editing = false), popUpTo)
                PlayGate.Play -> navController.navigate(GameListRoute, popUpTo)
            }
        }
    }

    // Every navigation goes through entry.ifResumed, so a double tap can't open the same screen twice.
    // Short slide + fade between screens (design §3.4); going back reverses it.
    NavHost(
        navController = navController,
        startDestination = WelcomeRoute,
        enterTransition = { slideInHorizontally(tween(TransitionMillis)) { it / 8 } + fadeIn(tween(TransitionMillis)) },
        exitTransition = { slideOutHorizontally(tween(TransitionMillis)) { -it / 8 } + fadeOut(tween(TransitionMillis)) },
        popEnterTransition = { slideInHorizontally(tween(TransitionMillis)) { -it / 8 } + fadeIn(tween(TransitionMillis)) },
        popExitTransition = { slideOutHorizontally(tween(TransitionMillis)) { it / 8 } + fadeOut(tween(TransitionMillis)) },
    ) {
        composable<WelcomeRoute> { entry ->
            WelcomeScreen(
                profile = session.profile,
                onPlay = {
                    entry.ifResumed {
                        if (container.prefs.prefs.value.playTourSeen) goByGate() else navController.navigate(PlayTourRoute(replay = false))
                    }
                },
                onJoin = { entry.ifResumed { navController.navigate(JoinRoute()) } },
                onHowToPlay = { entry.ifResumed { navController.navigate(PlayTourRoute(replay = true)) } },
                onAccount = { entry.ifResumed { navController.navigate(AccountRoute) } },
            )
        }
        composable<PlayTourRoute> { entry ->
            val replay = entry.toRoute<PlayTourRoute>().replay
            OnboardingPager(
                cards = playTourCards(),
                lastLabel = if (replay) Res.string.done_button else Res.string.start_button,
                onExit = { exit ->
                    // Skip, finish and back-out all mark the tour seen. After an Activity recreation onExit can fire
                    // again, so only the entry that is still on top may navigate.
                    scope.launch { container.prefs.update { it.copy(playTourSeen = true) } }
                    if (navController.currentBackStackEntry == entry) {
                        if (replay || exit == TourExit.BACKED_OUT) {
                            navController.popBackStack()
                        } else {
                            goByGate { popUpTo<PlayTourRoute> { inclusive = true } }
                        }
                    }
                },
            )
        }
        composable<SignInRoute> { entry ->
            SignInScreen(
                onSignedIn = { goByGate { popUpTo<SignInRoute> { inclusive = true } } },
                onBack = { entry.back() },
            )
        }
        composable<ProfileSetupRoute> { entry ->
            val editing = entry.toRoute<ProfileSetupRoute>().editing
            val current = session.profile
            ProfileSetupScreen(
                session = session,
                initialName = if (editing && current != null) current.displayName else suggestedName(user?.googleName),
                initialAvatar = current?.avatarId ?: Avatars.ids.first(),
                onSaved = {
                    entry.ifResumed {
                        if (editing) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(GameListRoute) { popUpTo<ProfileSetupRoute> { inclusive = true } }
                        }
                    }
                },
                onBack = { entry.back() },
            )
        }
        composable<AccountRoute> { entry ->
            AccountScreen(
                session = session,
                onEdit = { entry.ifResumed { navController.navigate(ProfileSetupRoute(editing = true)) } },
                onSignedOut = { navController.popBackStack(WelcomeRoute, inclusive = false) },
                onDeleted = { navController.popBackStack(WelcomeRoute, inclusive = false) },
                onBack = { entry.back() },
            )
        }
        composable<GameListRoute> { entry ->
            GameListScreen(onGame = { game -> entry.ifResumed { navController.navigate(ModeRoute(game)) } }, onBack = { entry.back() })
        }
        composable<ModeRoute> { entry ->
            val route = entry.toRoute<ModeRoute>()
            ModeSelectScreen(
                game = route.game,
                onLocal = { entry.ifResumed { navController.navigate(PlayersRoute(route.game)) } },
                onOnline = { entry.ifResumed { navController.navigate(OnlineLevelRoute(route.game)) } },
                onBack = { entry.back() },
            )
        }
        composable<OnlineLevelRoute> { entry ->
            val route = entry.toRoute<OnlineLevelRoute>()
            var creating by remember { mutableStateOf(false) }
            var failedLevel by remember { mutableStateOf<Level?>(null) }
            // The host is signed in with a profile here: the game list is behind the account gate.
            fun create(level: Level) {
                val uid = user?.uid
                val profile = session.profile
                if (creating || uid == null || profile == null) return
                creating = true
                scope.launch {
                    val host = Member(uid, profile.displayName, profile.avatarId, joinedAt = 0)
                    val lobby = container.lobbies.create(host, route.game, level)
                    creating = false
                    if (lobby == null) {
                        failedLevel = level
                    } else {
                        entry.ifResumed { navController.navigate(OnlineRoomRoute) { popUpTo<OnlineLevelRoute> { inclusive = true } } }
                    }
                }
            }
            LevelSelectScreen(onLevel = { level -> entry.ifResumed { create(level) } }, onBack = { entry.back() })
            failedLevel?.let { level ->
                PosterDialog(
                    title = stringResource(Res.string.err_lobby_create),
                    confirmText = stringResource(Res.string.try_again),
                    cancelText = stringResource(Res.string.cancel),
                    onConfirm = { failedLevel = null; create(level) },
                    onDismiss = { failedLevel = null },
                )
            }
        }
        composable<OnlineRoomRoute> { entry ->
            // Not entry.ifResumed: the room can end while its enter transition still runs.
            OnlineRoomScreen(onExit = { if (navController.currentBackStackEntry == entry) navController.popBackStack() })
        }
        composable<JoinRoute> { entry ->
            val route = entry.toRoute<JoinRoute>()
            JoinScreen(
                initialCode = route.code,
                profile = session.profile.takeIf { user?.isAnonymous == false },
                onJoined = { entry.ifResumed { navController.navigate(OnlineRoomRoute) { popUpTo<JoinRoute> { inclusive = true } } } },
                onBack = { entry.back() },
            )
        }
        composable<PlayersRoute> { entry ->
            val route = entry.toRoute<PlayersRoute>()
            PlayerSetupScreen(
                onContinue = { players -> entry.ifResumed { navController.navigate(LevelRoute(route.game, players)) } },
                onBack = { entry.back() },
            )
        }
        composable<LevelRoute> { entry ->
            val route = entry.toRoute<LevelRoute>()
            LevelSelectScreen(
                onLevel = { level -> entry.ifResumed { startGame(route.game, route.players, level) } },
                onBack = { entry.back() },
            )
        }
        composable<TruthOrDareRoute> { entry ->
            val route = entry.toRoute<TruthOrDareRoute>()
            TruthOrDareScreen(
                players = route.players,
                level = route.level,
                onEnd = {
                    entry.ifResumed {
                        navController.navigate(GameOverRoute(GameId.TRUTH_OR_DARE, route.players, route.level)) {
                            popUpTo<TruthOrDareRoute> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<SpinTheBottleRoute> { entry ->
            val route = entry.toRoute<SpinTheBottleRoute>()
            SpinTheBottleScreen(
                players = route.players,
                level = route.level,
                onEnd = {
                    entry.ifResumed {
                        navController.navigate(GameOverRoute(GameId.SPIN_THE_BOTTLE, route.players, route.level)) {
                            popUpTo<SpinTheBottleRoute> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<BeerPongSetupRoute> { entry ->
            val route = entry.toRoute<BeerPongSetupRoute>()
            BeerPongSetupScreen(
                players = route.players,
                suggested = suggestedMatchup(route.players.size, route.previousWinner, route.previousLoser),
                previousWinner = route.previousWinner,
                onStart = { matchup ->
                    entry.ifResumed {
                        navController.navigate(BeerPongRoute(route.players, route.level, matchup.first, matchup.second)) {
                            popUpTo<BeerPongSetupRoute> { inclusive = true }
                        }
                    }
                },
                onBack = { entry.back() },
            )
        }
        composable<BeerPongRoute> { entry ->
            val route = entry.toRoute<BeerPongRoute>()
            val matchIndexes = listOf(route.first, route.second)
            BeerPongScreen(
                players = matchIndexes.map { route.players[it] },
                playerIndexes = matchIndexes,
                level = route.level,
                onMatchOver = { winner, loser ->
                    entry.ifResumed {
                        navController.navigate(
                            GameOverRoute(GameId.BEER_PONG, route.players, route.level, matchIndexes[winner], matchIndexes[loser]),
                        ) { popUpTo<BeerPongRoute> { inclusive = true } }
                    }
                },
                onEnd = {
                    entry.ifResumed {
                        navController.navigate(GameOverRoute(GameId.BEER_PONG, route.players, route.level)) {
                            popUpTo<BeerPongRoute> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<GameOverRoute> { entry ->
            val route = entry.toRoute<GameOverRoute>()
            GameOverScreen(
                game = route.game,
                level = route.level,
                onPlayAgain = {
                    entry.ifResumed {
                        startGame(route.game, route.players, route.level, route.matchWinner, route.matchLoser) {
                            popUpTo<GameOverRoute> { inclusive = true }
                        }
                    }
                },
                onBackToGames = { entry.ifResumed { navController.popBackStack(GameListRoute, inclusive = false) } },
            )
        }
    }
}
