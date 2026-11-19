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
import com.mehmetbozkurt.clink.ui.screens.GameListScreen
import com.mehmetbozkurt.clink.ui.screens.GameOverScreen
import com.mehmetbozkurt.clink.ui.screens.LevelSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ModeSelectScreen
import com.mehmetbozkurt.clink.ui.screens.PlayerSetupScreen
import com.mehmetbozkurt.clink.ui.screens.WelcomeScreen

@Composable
fun ClinkNavHost(navController: NavHostController = rememberNavController()) {
    // Game screens are registered in Tasks 9 and 10; until then a game goes straight to GameOver.
    fun startGame(game: GameId, players: List<String>, level: Level, options: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(GameOverRoute(game, players, level), options)
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
