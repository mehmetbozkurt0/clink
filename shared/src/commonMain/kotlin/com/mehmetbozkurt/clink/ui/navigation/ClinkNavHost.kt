package com.mehmetbozkurt.clink.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.beerpong.BeerPongScreen
import com.mehmetbozkurt.clink.game.beerpong.BeerPongSetupScreen
import com.mehmetbozkurt.clink.game.beerpong.suggestedMatchup
import com.mehmetbozkurt.clink.game.spinthebottle.SpinTheBottleScreen
import com.mehmetbozkurt.clink.game.truthordare.TruthOrDareScreen
import com.mehmetbozkurt.clink.ui.screens.GameListScreen
import com.mehmetbozkurt.clink.ui.screens.GameOverScreen
import com.mehmetbozkurt.clink.ui.screens.LevelSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ModeSelectScreen
import com.mehmetbozkurt.clink.ui.screens.PlayerSetupScreen
import com.mehmetbozkurt.clink.ui.screens.WelcomeScreen

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
            WelcomeScreen(onPlay = { entry.ifResumed { navController.navigate(GameListRoute) } })
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
