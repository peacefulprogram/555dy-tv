package io.github.peacefulprogram.dy555.compose.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.zIndex
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.R
import io.github.peacefulprogram.dy555.compose.util.FocusGroup

@OptIn(ExperimentalTvFoundationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun CustomTabRow(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabFocus: (Int) -> Unit = {}
) {
    var anyTabFocused by remember {
        mutableStateOf(false)
    }
    FocusGroup(modifier = modifier.onFocusChanged { anyTabFocused = it.hasFocus }) {
        TabRow(selectedTabIndex = selectedTabIndex,
            indicator = { tabPositions ->
                FocusBorderTabRowIndicator(
                    currentTabPosition = tabPositions[selectedTabIndex],
                    anyTabFocused = anyTabFocused
                )
            }) {
            tabs.forEachIndexed { tabIndex, tabName ->
                Tab(
                    selected = tabIndex == selectedTabIndex,
                    onFocus = { onTabFocus(tabIndex) },
                    modifier = Modifier.restorableFocus(),
                    colors = TabDefaults.underlinedIndicatorTabColors(
                        activeContentColor = colorResource(id = R.color.gray100),
                        selectedContentColor = colorResource(id = R.color.gray100),
                        focusedSelectedContentColor = colorResource(id = R.color.gray100)
                    )
                ) {
                    Text(
                        text = tabName,
                        modifier = Modifier.padding(8.dp, 4.dp),
                    )
                }
            }
        }
    }

}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FocusBorderTabRowIndicator(
    currentTabPosition: DpRect,
    anyTabFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset()
    )
    val width by animateDpAsState(targetValue = currentTabPosition.width)
    val height = currentTabPosition.height
    val unfocusedIndicatorWidth = 10.dp
    val leftOffset = if (anyTabFocused) {
        animateDpAsState(
            targetValue = currentTabPosition.left
        ).value
    } else {
        currentTabPosition.left + currentTabPosition.width / 2 - unfocusedIndicatorWidth / 2
    }
    val topOffset = currentTabPosition.top

    val borderColor by animateColorAsState(
        targetValue = if (anyTabFocused) MaterialTheme.colorScheme.border else MaterialTheme.colorScheme.border.copy(
            alpha = 0.5f
        )
    )
    if (anyTabFocused) {
        Box(
            modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = leftOffset, y = topOffset)
                .width(width)
                .height(height)
                .border(2.dp, borderColor, MaterialTheme.shapes.medium)
                .zIndex(-1f)
        )
    } else {
        Box(
            modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset(x = leftOffset)
                .width(unfocusedIndicatorWidth)
                .height(2.dp)
                .background(color = borderColor)
        )
    }
}