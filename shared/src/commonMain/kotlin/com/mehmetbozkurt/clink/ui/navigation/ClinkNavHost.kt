package com.mehmetbozkurt.clink.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mehmetbozkurt.clink.content.Level
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.game.spinthebottle.SpinTheBottleScreen
import com.mehmetbozkurt.clink.game.truthordare.TruthOrDareScreen
import com.mehmetbozkurt.clink.ui.screens.GameListScreen
import com.mehmetbozkurt.clink.ui.screens.GameOverScreen
import com.mehmetbozkurt.clink.ui.screens.LevelSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ModeSelectScreen
import com.mehmetbozkurt.clink.ui.screens.PlayerSetupScreen
import com.mehmetbozkurt.clink.ui.screens.WelcomeScreen

@Composable
fun ClinkNavHost(navController: NavHostController = rememberNavController()) {
    fun startGame(game: GameId, players: List<String>, level: Level, options: NavOptionsBuilder.() -> Unit = {}) {
        val route: Any = when (game) {
            GameId.TRUTH_OR_DARE -> TruthOrDareRoute(players, level)
            GameId.SPIN_THE_BOTTLE -> SpinTheBottleRoute(players, level)
            // Beer Pong is disabled in the game list until Plan 2.
            GameId.BEER_PONG -> GameOverRoute(game, players, level)
        }
        navController.navigate(route, options)
    }

    NavHost(navController = navController, startDestination = WelcomeRoute) {
        composable<WelcomeRoute> {
            WelcomeScreen(onPlay = { navController.navigate(GameListRoute) })
        }
        composable<GameListRoute> {
            GameListScreen(onGame = { navController.navigate(ModeRoute(it)) })
        }
        composable<ModeRoute> { entry ->
            val route = entry.toRoute<ModeRoute>()
            ModeSelectScreen(game = route.game, onLocal = { navController.navigate(PlayersRoute(route.game)) })
        }
        composable<PlayersRoute> { entry ->
            val route = entry.toRoute<PlayersRoute>()
            PlayerSetupScreen(onContinue = { players -> navController.navigate(LevelRoute(route.game, players)) })
        }
        composable<LevelRoute> { entry ->
            val route = entry.toRoute<LevelRoute>()
            LevelSelectScreen(onLevel = { level -> startGame(route.game, route.players, level) })
        }
        composable<TruthOrDareRoute> { entry ->
            val route = entry.toRoute<TruthOrDareRoute>()
            TruthOrDareScreen(
                players = route.players,
                level = route.level,
                onEnd = {
                    navController.navigate(GameOverRoute(GameId.TRUTH_OR_DARE, route.players, route.level)) {
                        popUpTo<TruthOrDareRoute> { inclusive = true }
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
                    navController.navigate(GameOverRoute(GameId.SPIN_THE_BOTTLE, route.players, route.level)) {
                        popUpTo<SpinTheBottleRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<GameOverRoute> { entry ->
            val route = entry.toRoute<GameOverRoute>()
            GameOverScreen(
                game = route.game,
                onPlayAgain = {
                    startGame(route.game, route.players, route.level) { popUpTo<GameOverRoute> { inclusive = true } }
                },
                onBackToGames = { navController.popBackStack(GameListRoute, inclusive = false) },
            )
        }
    }
}
