package io.github.x80070006.holowave.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.x80070006.holowave.ui.MainTab
import io.github.x80070006.holowave.ui.theme.DecoIcons
import io.github.x80070006.holowave.ui.theme.DecoType
import io.github.x80070006.holowave.ui.theme.HoloTheme

/**
 * Four tabs, always in the thumb zone. The active tab is marked by a primary-coloured
 * pill behind its icon and a primary label; nothing else moves.
 */
@Composable
fun HoloBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = HoloTheme.colors
    val haptic = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface)
            .navigationBarsPadding()
            .height(68.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            val active = tab == selected
            val tint by animateColorAsState(if (active) c.primary else c.textSecondary, tween(180), label = "tabTint")
            val pill by animateColorAsState(if (active) c.primary.copy(alpha = 0.18f) else Color.Transparent, tween(180), label = "tabPill")
            Column(
                Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) {
                        if (!active) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSelect(tab)
                    }
                    .semantics { this.selected = active },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.width(60.dp).height(32.dp).clip(RoundedCornerShape(50)).background(pill), contentAlignment = Alignment.Center) {
                    Icon(iconOf(tab), tab.label, Modifier.size(24.dp), tint = tint)
                }
                Spacer(Modifier.height(2.dp))
                Text(tab.label, style = DecoType.Small.copy(fontSize = DecoType.Small.fontSize * 0.92f), color = tint, maxLines = 1)
            }
        }
    }
}

private fun iconOf(tab: MainTab) = when (tab) {
    MainTab.HOME -> DecoIcons.Home
    MainTab.SEARCH -> DecoIcons.Search
    MainTab.LIBRARY -> DecoIcons.Shelf
    MainTab.SETTINGS -> DecoIcons.Tune
}
