package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cd_account
import com.mehmetbozkurt.clink.resources.how_to_play
import com.mehmetbozkurt.clink.resources.play
import com.mehmetbozkurt.clink.resources.tagline
import com.mehmetbozkurt.clink.resources.welcome_join
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.SecondaryButton
import com.mehmetbozkurt.clink.ui.components.TapeLabel
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import com.mehmetbozkurt.clink.ui.theme.ClinkFonts
import org.jetbrains.compose.resources.stringResource

/**
 * Welcome (design §4, spec §5.1): spray logo with drips and the tape tagline centred in the free space, PLAY and JOIN as
 * equal buttons, a quiet "How to play" link below. No top bar; once the host has a profile, their avatar sits top-right
 * and opens the account screen.
 */
@Composable
fun WelcomeScreen(
    profile: Profile?,
    onPlay: () -> Unit,
    onJoin: () -> Unit,
    onHowToPlay: () -> Unit,
    onAccount: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (profile != null) {
                val label = stringResource(Res.string.cd_account)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onAccount)
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarBadge(avatarId = profile.avatarId, name = profile.displayName, size = 40.dp)
                }
            }
        }
        // Centred when it fits; scrolls when a large font doesn't.
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    text = buildAnnotatedString {
                        append("clink")
                        withStyle(SpanStyle(color = ClinkColors.Pink)) { append(".") }
                    },
                    color = ClinkColors.Acid,
                    style = TextStyle(
                        fontFamily = ClinkFonts.spray,
                        fontSize = 76.sp,
                        lineHeight = 80.sp,
                        shadow = Shadow(color = ClinkColors.Acid.copy(alpha = 0.35f), blurRadius = 24f),
                    ),
                )
                Drips(modifier = Modifier.padding(start = 22.dp))
                Spacer(Modifier.height(14.dp))
                TapeLabel(stringResource(Res.string.tagline))
            }
        }
        PrimaryButton(text = stringResource(Res.string.play), onClick = onPlay, modifier = Modifier.fillMaxWidth())
        // Guests with an invite code start here (spec §5.1); no account needed.
        SecondaryButton(
            text = stringResource(Res.string.welcome_join),
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onHowToPlay),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.how_to_play),
                color = ClinkColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
/** A few paint drips under the logo. */
@Composable
private fun Drips(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(14, 26, 9, 20).forEach { length ->
            Box(
                Modifier
                    .size(width = 5.dp, height = length.dp)
                    .background(ClinkColors.Acid.copy(alpha = 0.85f), RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
    }
}
