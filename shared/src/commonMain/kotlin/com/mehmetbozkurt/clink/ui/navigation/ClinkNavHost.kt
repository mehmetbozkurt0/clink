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
                onPlay = { entry.ifResumed { goByGate() } },
                onAccount = { entry.ifResumed { navController.navigate(AccountRoute) } },
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
