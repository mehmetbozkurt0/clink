package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.account.Avatars
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.av_alien
import com.mehmetbozkurt.clink.resources.av_bird
import com.mehmetbozkurt.clink.resources.av_butterfly
import com.mehmetbozkurt.clink.resources.av_cactus
import com.mehmetbozkurt.clink.resources.av_cat
import com.mehmetbozkurt.clink.resources.av_crown
import com.mehmetbozkurt.clink.resources.av_dog
import com.mehmetbozkurt.clink.resources.av_fish
import com.mehmetbozkurt.clink.resources.av_ghost
import com.mehmetbozkurt.clink.resources.av_rabbit
import com.mehmetbozkurt.clink.resources.av_robot
import com.mehmetbozkurt.clink.resources.av_rocket
import com.mehmetbozkurt.clink.resources.avatar_alien
import com.mehmetbozkurt.clink.resources.avatar_bird
import com.mehmetbozkurt.clink.resources.avatar_butterfly
import com.mehmetbozkurt.clink.resources.avatar_cactus
import com.mehmetbozkurt.clink.resources.avatar_cat
import com.mehmetbozkurt.clink.resources.avatar_crown
import com.mehmetbozkurt.clink.resources.avatar_dog
import com.mehmetbozkurt.clink.resources.avatar_fish
import com.mehmetbozkurt.clink.resources.avatar_ghost
import com.mehmetbozkurt.clink.resources.avatar_rabbit
import com.mehmetbozkurt.clink.resources.avatar_robot
import com.mehmetbozkurt.clink.resources.avatar_rocket
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private class AvatarArt(val icon: DrawableResource, val label: StringResource)

private val AvatarArts = mapOf(
    "cat" to AvatarArt(Res.drawable.av_cat, Res.string.avatar_cat),
    "dog" to AvatarArt(Res.drawable.av_dog, Res.string.avatar_dog),
    "bird" to AvatarArt(Res.drawable.av_bird, Res.string.avatar_bird),
    "fish" to AvatarArt(Res.drawable.av_fish, Res.string.avatar_fish),
    "rabbit" to AvatarArt(Res.drawable.av_rabbit, Res.string.avatar_rabbit),
    "butterfly" to AvatarArt(Res.drawable.av_butterfly, Res.string.avatar_butterfly),
    "ghost" to AvatarArt(Res.drawable.av_ghost, Res.string.avatar_ghost),
    "alien" to AvatarArt(Res.drawable.av_alien, Res.string.avatar_alien),
    "robot" to AvatarArt(Res.drawable.av_robot, Res.string.avatar_robot),
    "rocket" to AvatarArt(Res.drawable.av_rocket, Res.string.avatar_rocket),
    "crown" to AvatarArt(Res.drawable.av_crown, Res.string.avatar_crown),
    "cactus" to AvatarArt(Res.drawable.av_cactus, Res.string.avatar_cactus),
)

/** The avatar's name for screen readers ("Kedi"), or null for an unknown id. */
@Composable
fun avatarLabel(avatarId: String): String? = AvatarArts[avatarId]?.let { stringResource(it.label) }

/**
 * A profile avatar: the preset icon in ink on a neon circle (colors cycle like the player badges). An unknown id
 * (e.g. from a newer app version) falls back to the name's initial badge.
 */
@Composable
fun AvatarBadge(avatarId: String, name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val art = AvatarArts[avatarId]
    if (art == null) {
        InitialBadge(name = name, index = 0, modifier = modifier, size = size)
        return
    }
    Box(
        modifier = modifier.size(size).background(badgeColor(Avatars.ids.indexOf(avatarId)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(art.icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(size * 0.2f),
            colorFilter = ColorFilter.tint(ClinkColors.Ink),
        )
    }
}
