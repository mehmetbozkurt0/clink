package com.mehmetbozkurt.clink.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mehmetbozkurt.clink.resources.Res
import com.mehmetbozkurt.clink.resources.next_button
import com.mehmetbozkurt.clink.resources.skip_button
import com.mehmetbozkurt.clink.resources.start_button
import com.mehmetbozkurt.clink.ui.components.PrimaryButton
import com.mehmetbozkurt.clink.ui.screens.ScreenColumn
import com.mehmetbozkurt.clink.ui.screens.ScreenTitle
import com.mehmetbozkurt.clink.ui.theme.ClinkColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** One card of a tour: a poster title, a short body and a live [preview] (usually inside a [PhoneFrame]). */
data class TourCard(val title: StringResource, val body: StringResource, val preview: @Composable () -> Unit)

/**
 * A swipeable tour (spec §5.2). [TourState] holds the logic; the pager and the buttons are kept in sync with it.
 * [onExit] is called once when the tour ends, however it ended; the caller decides what happens next. [lastLabel] is the
 * button on the last card ("Start" by default, "Done" for a replay that just returns).
 */
@Composable
fun OnboardingPager(cards: List<TourCard>, onExit: (TourExit) -> Unit, lastLabel: StringResource = Res.string.start_button) {
    // Saveable: the activity is recreated on rotation or a language change.
    var state by rememberSaveable(cards.size, stateSaver = TourStateSaver) { mutableStateOf(TourState(page = 0, pageCount = cards.size)) }
    val pager = rememberPagerState(pageCount = { cards.size })

    // Buttons and system back move the pager; a swipe that settles moves the state.
    LaunchedEffect(state.page) {
        if (pager.currentPage != state.page) pager.animateScrollToPage(state.page)
    }
    LaunchedEffect(pager.settledPage) { state = state.goTo(pager.settledPage) }
    LaunchedEffect(state.exit) { state.exit?.let(onExit) }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.exit == null,
        onBackCompleted = { state = state.back() },
    )

    val last = state.page == cards.lastIndex
    ScreenColumn {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(Res.string.skip_button).toUpperCase(Locale.current),
                color = ClinkColors.Paper,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 48.dp)
                    .clickable(role = Role.Button) { state = state.skip() }
                    .padding(horizontal = 8.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val card = cards[index]
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    card.preview()
                }
                ScreenTitle(stringResource(card.title))
                Text(
                    text = stringResource(card.body),
                    color = ClinkColors.Paper,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        PageDots(count = cards.size, current = state.page)
        PrimaryButton(
            text = stringResource(if (last) lastLabel else Res.string.next_button),
            onClick = { state = state.next() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val TourStateSaver = Saver<TourState, List<Int>>(
    save = { listOf(it.page, it.pageCount, it.exit?.ordinal ?: -1) },
    restore = { TourState(it[0], it[1], TourExit.entries.getOrNull(it[2])) },
)

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (index == current) ClinkColors.Acid else ClinkColors.Paper.copy(alpha = 0.3f)),
            )
        }
    }
}
