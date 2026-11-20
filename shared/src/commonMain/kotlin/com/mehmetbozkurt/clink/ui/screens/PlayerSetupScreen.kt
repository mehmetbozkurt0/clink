package com.mehmetbozkurt.clink.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.mehmetbozkurt.clink.game.PlayerNames
import com.mehmetbozkurt.clink.game.PlayerNamesResult
import com.mehmetbozkurt.clink.game.moved
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.add_player
import com.mehmetbozkurt.clink.resources.cd_drag
import com.mehmetbozkurt.clink.resources.cd_remove
import com.mehmetbozkurt.clink.resources.continue_button
import com.mehmetbozkurt.clink.resources.err_duplicate
import com.mehmetbozkurt.clink.resources.err_too_few
import com.mehmetbozkurt.clink.resources.err_too_many
import com.mehmetbozkurt.clink.resources.ic_add
import com.mehmetbozkurt.clink.resources.ic_drag
import com.mehmetbozkurt.clink.resources.ic_remove
import com.mehmetbozkurt.clink.resources.move_down
import com.mehmetbozkurt.clink.resources.move_up
import com.mehmetbozkurt.clink.resources.player_hint
import com.mehmetbozkurt.clink.resources.players_count
import com.mehmetbozkurt.clink.resources.players_title
import com.mehmetbozkurt.clink.ui.components.InitialBadge
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.components.TopBar
import com.mehmetbozkurt.clink.ui.components.TopBarNav
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A name row; [id] stays with the row when it moves, so its text field and animation move with it. */
private data class PlayerRow(val id: Int, val name: String)

private val RowsSaver = listSaver<SnapshotStateList<PlayerRow>, Any>(
    save = { rows -> rows.flatMap { listOf(it.id, it.name) } },
    restore = { flat -> flat.chunked(2) { PlayerRow(it[0] as Int, it[1] as String) }.toMutableStateList() },
)

private val RowShape = RoundedCornerShape(12.dp)
private val RowSpacing = 8.dp

/** Players (design §4, §6): long-press the handle and drag to reorder; × removes; counter "n / 12". */
@Composable
fun PlayerSetupScreen(onContinue: (List<String>) -> Unit, onBack: () -> Unit) {
    val rows = rememberSaveable(saver = RowsSaver) { mutableStateListOf(PlayerRow(0, ""), PlayerRow(1, "")) }
    var error by remember { mutableStateOf<PlayerNamesResult?>(null) }

    // Drag state: the row under the finger and how far it is from its slot.
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val spacingPx = with(LocalDensity.current) { RowSpacing.toPx() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var settle: Job? by remember { mutableStateOf(null) }

    fun move(from: Int, to: Int) {
        val reordered = rows.moved(from, to)
        rows.clear()
        rows.addAll(reordered)
    }

    /** Swaps the dragged row past a neighbor once it is more than half a row over it. */
    fun dragBy(id: Int, dy: Float) {
        val pitch = rowHeight + spacingPx
        dragOffset += dy
        var index = rows.indexOfFirst { it.id == id }
        while (dragOffset > pitch / 2 && index < rows.lastIndex) {
            move(index, index + 1)
            index++
            dragOffset -= pitch
        }
        while (dragOffset < -pitch / 2 && index > 0) {
            move(index, index - 1)
            index--
            dragOffset += pitch
        }
    }

    fun endDrag() {
        settle = scope.launch {
            animate(dragOffset, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ -> dragOffset = value }
            draggingId = null
        }
    }

    ScreenColumn {
        TopBar(navigation = TopBarNav.Back, onNavigate = onBack) {
            Text(
                text = stringResource(Res.string.players_count, rows.size, PlayerNames.MAX),
                color = ClinkColors.Acid,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        ScreenTitle(stringResource(Res.string.players_title))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState(), enabled = draggingId == null),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            rows.forEachIndexed { index, row ->
                key(row.id) {
                    val dragged = row.id == draggingId
                    NameRow(
                        name = row.name,
                        index = index,
                        lastIndex = rows.lastIndex,
                        canRemove = rows.size > PlayerNames.MIN,
                        dragged = dragged,
                        modifier = Modifier
                            .onSizeChanged { rowHeight = it.height }
                            .animatePlacement(animate = !dragged)
                            .zIndex(if (dragged) 1f else 0f)
                            .graphicsLayer { translationY = if (dragged) dragOffset else 0f },
                        onNameChange = {
                            rows[index] = row.copy(name = it.take(PlayerNames.MAX_LENGTH))
                            error = null
                        },
                        onRemove = {
                            rows.removeAt(index)
                            error = null
                        },
                        onMove = { to -> move(index, to) },
                        handle = Modifier.pointerInputDrag(
                            key = row.id,
                            onStart = {
                                settle?.cancel()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingId = row.id
                                dragOffset = 0f
                            },
                            onDrag = { dy -> dragBy(row.id, dy) },
                            onEnd = ::endDrag,
                        ),
                    )
                }
            }
            if (rows.size < PlayerNames.MAX) {
                AddPlayerButton {
                    rows.add(PlayerRow(rows.maxOf { it.id } + 1, ""))
                    error = null
                }
            }
        }
        error?.let { result ->
            errorText(result)?.let { Text(text = it, color = ClinkColors.Amber, style = MaterialTheme.typography.bodyMedium) }
        }
        PrimaryButton(
            text = stringResource(Res.string.continue_button),
            onClick = {
                when (val result = PlayerNames.validate(rows.map { it.name })) {
                    is PlayerNamesResult.Valid -> onContinue(result.names)
                    else -> error = result
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NameRow(
    name: String,
    index: Int,
    lastIndex: Int,
    canRemove: Boolean,
    dragged: Boolean,
    modifier: Modifier,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    onMove: (to: Int) -> Unit,
    handle: Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ClinkColors.WallRaised, RowShape)
            .border(if (dragged) 2.dp else 1.dp, if (dragged) ClinkColors.Acid else ClinkColors.Paper.copy(alpha = 0.14f), RowShape)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialBadge(name = name, index = index)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ClinkColors.Paper, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(ClinkColors.Acid),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            interactionSource = interaction,
            decorationBox = { field ->
                Column {
                    Box {
                        if (name.isEmpty()) {
                            Text(stringResource(Res.string.player_hint, index + 1), color = ClinkColors.Muted, style = MaterialTheme.typography.bodyLarge)
                        }
                        field()
                    }
                    // The focused name gets an acid underline (mockup).
                    Box(
                        Modifier.padding(top = 2.dp).fillMaxWidth().height(2.dp)
                            .background(if (focused) ClinkColors.Acid else ClinkColors.WallRaised),
                    )
                }
            },
        )
        val moveUp = stringResource(Res.string.move_up)
        val moveDown = stringResource(Res.string.move_down)
        val dragLabel = stringResource(Res.string.cd_drag)
        RowIcon(
            icon = Res.drawable.ic_drag,
            enabled = true,
            modifier = handle.semantics {
                contentDescription = dragLabel
                // Screen readers can't long-press-drag, so the handle also offers move up / down.
                customActions = buildList {
                    if (index > 0) add(CustomAccessibilityAction(moveUp) { onMove(index - 1); true })
                    if (index < lastIndex) add(CustomAccessibilityAction(moveDown) { onMove(index + 1); true })
                }
            },
        )
        val removeLabel = stringResource(Res.string.cd_remove)
        RowIcon(
            icon = Res.drawable.ic_remove,
            enabled = canRemove,
            modifier = Modifier
                .clickable(enabled = canRemove, role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeLabel },
        )
    }
}

/** Not focusable, so the keyboard's Next key jumps straight to the next name field. */
@Composable
private fun RowIcon(icon: DrawableResource, enabled: Boolean, modifier: Modifier) {
    Box(
        modifier = Modifier.size(44.dp).clip(RowShape).focusProperties { canFocus = false }.then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(if (enabled) ClinkColors.Muted else ClinkColors.Muted.copy(alpha = 0.3f)),
        )
    }
}

/** Dashed "+ Add player" (mockup). */
@Composable
private fun AddPlayerButton(onClick: () -> Unit) {
    val dash = ClinkColors.Paper.copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = dash,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .focusProperties { canFocus = false }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(ClinkColors.Paper),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.add_player),
            color = ClinkColors.Paper,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

/** Long-press then drag; reports vertical movement only. */
private fun Modifier.pointerInputDrag(key: Any, onStart: () -> Unit, onDrag: (Float) -> Unit, onEnd: () -> Unit): Modifier =
    pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onStart() },
            onDragEnd = onEnd,
            onDragCancel = onEnd,
            onDrag = { change, amount ->
                change.consume()
                onDrag(amount.y)
            },
        )
    }

/**
 * Rows slide to their new slot instead of jumping when the order changes. [animate] is off for the dragged row,
 * which follows the finger instead.
 */
private fun Modifier.animatePlacement(animate: Boolean): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<IntOffset?>(null) }
    var anim by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    onPlaced { target = it.positionInParent().round() }
        .offset {
            val t = target ?: return@offset IntOffset.Zero
            if (!animate) {
                anim = null
                return@offset IntOffset.Zero
            }
            val a = anim ?: Animatable(t, IntOffset.VectorConverter).also { anim = it }
            if (a.targetValue != t) scope.launch { a.animateTo(t, spring(stiffness = Spring.StiffnessMediumLow)) }
            a.value - t
        }
}

@Composable
private fun errorText(result: PlayerNamesResult): String? = when (result) {
    PlayerNamesResult.TooFew -> stringResource(Res.string.err_too_few)
    PlayerNamesResult.TooMany -> stringResource(Res.string.err_too_many)
    is PlayerNamesResult.Duplicate -> stringResource(Res.string.err_duplicate, result.name)
    is PlayerNamesResult.Valid -> null
}
