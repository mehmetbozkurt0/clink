package com.mehmetbozkurt.clink.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.cd_back
import com.mehmetbozkurt.clink.resources.cd_close
import com.mehmetbozkurt.clink.resources.ic_back
import com.mehmetbozkurt.clink.resources.ic_close
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Back (‹) on flow screens; Close (×) on game screens, which opens the End-game dialog. */
enum class TopBarNav { Back, Close }

/**
 * The shared top bar (design §6): a 48 dp navigation button, an optional title, and [trailing] content on the right
 * (a counter, a turn badge). Navigation does exactly what system back does on that screen.
 */
@Composable
fun TopBar(
    navigation: TopBarNav,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onNavigate),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(ClinkColors.WallRaised, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(if (navigation == TopBarNav.Back) Res.drawable.ic_back else Res.drawable.ic_close),
                    contentDescription = stringResource(if (navigation == TopBarNav.Back) Res.string.cd_back else Res.string.cd_close),
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(ClinkColors.Paper),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (title != null) {
            Text(text = title, color = ClinkColors.Paper, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}
