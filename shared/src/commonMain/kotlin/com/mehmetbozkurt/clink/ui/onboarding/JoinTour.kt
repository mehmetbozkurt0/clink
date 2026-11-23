package com.mehmetbozkurt.clink.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.game.truthordare.Choosing
import com.mehmetbozkurt.clink.online.Member
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.online_waiting_for
import com.mehmetbozkurt.clink.resources.tour_join_1_body
import com.mehmetbozkurt.clink.resources.tour_join_1_title
import com.mehmetbozkurt.clink.resources.tour_join_2_body
import com.mehmetbozkurt.clink.resources.tour_join_2_title
import com.mehmetbozkurt.clink.resources.tour_join_3_body
import com.mehmetbozkurt.clink.resources.tour_join_3_title
import com.mehmetbozkurt.clink.resources.tour_sample_guest
import com.mehmetbozkurt.clink.resources.tour_sample_host
import com.mehmetbozkurt.clink.ui.components.WaitingLabel
import com.mehmetbozkurt.clink.ui.online.CodeCard
import com.mehmetbozkurt.clink.ui.online.MemberRow
import org.jetbrains.compose.resources.stringResource

/** The three cards of the Join tour (spec §5.3); the previews are the real composables with sample data. */
fun joinTourCards(): List<TourCard> = listOf(
    TourCard(Res.string.tour_join_1_title, Res.string.tour_join_1_body) {
        PhoneFrame { LobbyPreview() }
    },
    TourCard(Res.string.tour_join_2_title, Res.string.tour_join_2_body) {
        PhoneFrame {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                CodeCard("X2KW2T", onShare = {})
            }
        }
    },
    TourCard(Res.string.tour_join_3_title, Res.string.tour_join_3_body) {
        TurnPreview()
    },
)

@Composable
private fun LobbyPreview() {
    val host = stringResource(Res.string.tour_sample_host)
    val guest = stringResource(Res.string.tour_sample_guest)
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberRow(Member("host", host, "cat", joinedAt = 0), isHost = true, isMe = false, picked = false, onPick = null, onRemove = null)
            MemberRow(Member("guest", guest, "dog", joinedAt = 1), isHost = false, isMe = true, picked = false, onPick = null, onRemove = null)
        }
    }
}

/** Two mini phones: the player whose turn it is sees the buttons, the other one sees who is being waited for. */
@Composable
private fun TurnPreview() {
    val host = stringResource(Res.string.tour_sample_host)
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PhoneFrame(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { Choosing(onPick = {}) }
        }
        PhoneFrame(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                WaitingLabel(stringResource(Res.string.online_waiting_for, host))
            }
        }
    }
}
