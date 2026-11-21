package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.account.AccountSession
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.account.Profile
import com.mehmetbozkurt.clink.account.ProfileInput
import com.mehmetbozkurt.clink.account.ProfileNameResult
import com.mehmetbozkurt.clink.game.PlayerNames
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.err_profile_name_blank
import com.mehmetbozkurt.clink.resources.err_profile_save_failed
import com.mehmetbozkurt.clink.resources.profile_avatar
import com.mehmetbozkurt.clink.resources.profile_name_hint
import com.mehmetbozkurt.clink.resources.profile_save
import com.mehmetbozkurt.clink.resources.profile_title
import com.mehmetbozkurt.clink.ui.components.AvatarBadge
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.components.avatarLabel
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val FieldShape = RoundedCornerShape(12.dp)

/** Profile setup and editing (spec §4): a name (prefilled) and one of the preset avatars in a 4 × 3 grid. */
@Composable
fun ProfileSetupScreen(
    session: AccountSession,
    initialName: String,
    initialAvatar: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var avatar by rememberSaveable { mutableStateOf(initialAvatar) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack)
        ScreenTitle(stringResource(Res.string.profile_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NameField(name = name, onChange = { name = it.take(PlayerNames.MAX_LENGTH); error = null })
            Text(text = stringResource(Res.string.profile_avatar), color = ClinkColors.Muted, style = MaterialTheme.typography.labelLarge)
            AvatarGrid(selected = avatar, name = name, onSelect = { avatar = it })
        }
        error?.let { Text(text = stringResource(it), color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium) }
        PrimaryButton(
            text = stringResource(Res.string.profile_save),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (busy) return@PrimaryButton
                when (val result = ProfileInput.validate(name)) {
                    ProfileNameResult.Blank -> error = Res.string.err_profile_name_blank
                    is ProfileNameResult.Valid -> {
                        busy = true
                        scope.launch {
                            val saved = try {
                                session.save(Profile(result.name, avatar))
                                true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                false
                            }
                            busy = false
                            if (saved) onSaved() else error = Res.string.err_profile_save_failed
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun NameField(name: String, onChange: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = name,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ClinkColors.Paper, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(ClinkColors.Acid),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        interactionSource = interaction,
        decorationBox = { field ->
            Column(
                Modifier
                    .background(ClinkColors.WallRaised, FieldShape)
                    .border(1.dp, ClinkColors.Paper.copy(alpha = 0.14f), FieldShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box {
                    if (name.isEmpty()) {
                        Text(stringResource(Res.string.profile_name_hint), color = ClinkColors.Muted, style = MaterialTheme.typography.bodyLarge)
                    }
                    field()
                }
                Box(
                    Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp)
                        .background(if (focused) ClinkColors.Acid else ClinkColors.WallRaised),
                )
            }
        },
    )
}

@Composable
private fun AvatarGrid(selected: String, name: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatars.ids.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { id ->
                    val isSelected = id == selected
                    val label = avatarLabel(id) ?: id
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .border(3.dp, if (isSelected) ClinkColors.Acid else Color.Transparent, CircleShape)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(id) })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarBadge(avatarId = id, name = name, size = 54.dp)
                    }
                }
            }
        }
    }
}
