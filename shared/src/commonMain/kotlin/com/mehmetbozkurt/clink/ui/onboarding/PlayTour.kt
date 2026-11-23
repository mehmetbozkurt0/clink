package com.mehmetbozkurt.clink.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.GameId
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.tour_play_1_body
import com.mehmetbozkurt.clink.resources.tour_play_1_title
import com.mehmetbozkurt.clink.resources.tour_play_2_body
import com.mehmetbozkurt.clink.resources.tour_play_2_title
import com.mehmetbozkurt.clink.resources.tour_play_3_body
import com.mehmetbozkurt.clink.resources.tour_play_3_title
import com.mehmetbozkurt.clink.resources.tour_play_4_body
import com.mehmetbozkurt.clink.resources.tour_play_4_title
import com.mehmetbozkurt.clink.resources.tour_sample_host
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.Poster
import com.mehmetbozkurt.clink.ui.screens.GameListScreen
import com.mehmetbozkurt.clink.ui.screens.LevelSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ModeSelectScreen
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.stringResource

/** The four cards of the Play tour (spec §5.4); the previews are the real screens with sample data. */
fun playTourCards(): List<TourCard> = listOf(
    TourCard(Res.string.tour_play_1_title, Res.string.tour_play_1_body) {
        PhoneFrame { GameListScreen(onGame = {}, onBack = {}) }
    },
    TourCard(Res.string.tour_play_2_title, Res.string.tour_play_2_body) {
        PhoneFrame { ModeSelectScreen(GameId.TRUTH_OR_DARE, {}, {}, {}) }
    },
    TourCard(Res.string.tour_play_3_title, Res.string.tour_play_3_body) {
        PhoneFrame { LevelSelectScreen({}, {}) }
    },
    TourCard(Res.string.tour_play_4_title, Res.string.tour_play_4_body) {
        PhoneFrame { HostPreview() }
    },
)

@Composable
private fun HostPreview() {
    val name = stringResource(Res.string.tour_sample_host)
    ScreenColumn {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Poster(rotation = -2f) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarBadge("cat", name, size = 64.dp)
                    Text(name, color = ClinkColors.Ink, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}