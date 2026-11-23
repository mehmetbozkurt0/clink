package com.mehmetbozkurt.clink.ui.online

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.online.CHAT_MAX_LENGTH
import com.mehmetbozkurt.clink.online.ChatMessage
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cd_chat
import com.mehmetbozkurt.clink.resources.chat_empty
import com.mehmetbozkurt.clink.resources.chat_hint
import com.mehmetbozkurt.clink.resources.chat_send
import com.mehmetbozkurt.clink.resources.chat_title
import com.mehmetbozkurt.clink.resources.ic_chat
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.ClinkTextField
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The chat button on the lobby and game top bars; a pink dot means unread messages. */
@Composable
internal fun ChatButton(unread: Boolean, onClick: () -> Unit) {
    val label = stringResource(Res.string.cd_chat)
    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(38.dp).background(ClinkColors.WallRaised, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Res.drawable.ic_chat),
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(ClinkColors.Paper),
            )
        }
        if (unread) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(10.dp).background(ClinkColors.Pink, CircleShape))
    }
}

/**
 * The lobby chat (spec §5.3) sliding over the screen: messages with avatar and name, at most [CHAT_MAX_LENGTH]
 * characters each. Tapping the dimmed part closes it.
 */
@Composable
internal fun ChatPanel(
    messages: List<ChatMessage>,
    myUid: String,
    avatarOf: (String) -> String?,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(messages.lastOrNull()?.id) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
    fun send() {
        if (draft.isBlank()) return
        onSend(draft)
        draft = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinkColors.Ink.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(ClinkColors.Wall)
                // Swallow taps so they don't reach the dimmed area behind.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(navigation = TopBarNav.Close, onNavigate = onClose, title = stringResource(Res.string.chat_title))
            LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (messages.isEmpty()) {
                    item { Text(stringResource(Res.string.chat_empty), color = ClinkColors.Muted, style = MaterialTheme.typography.bodyLarge) }
                }
                items(messages, key = { it.id }) { message -> MessageRow(message, mine = message.uid == myUid, avatarId = avatarOf(message.uid)) }
            }
            ClinkTextField(
                value = draft,
                onChange = { draft = it.take(CHAT_MAX_LENGTH) },
                hint = stringResource(Res.string.chat_hint),
                imeAction = ImeAction.Send,
                onImeAction = ::send,
            )
            PrimaryButton(text = stringResource(Res.string.chat_send), onClick = ::send, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage, mine: Boolean, avatarId: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        AvatarBadge(avatarId = avatarId.orEmpty(), name = message.name, size = 32.dp)
        Column {
            Text(
                text = message.name,
                color = if (mine) ClinkColors.Acid else ClinkColors.Pink,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(text = message.text, color = ClinkColors.Paper, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
