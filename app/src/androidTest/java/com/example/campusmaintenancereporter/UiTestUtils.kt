package com.example.campusmaintenancereporter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp

/**
 * Shows a screen inside a very tall box so lazy lists compose every item.
 * Use it for "does this exist / is it enabled" checks, not for taps.
 */
fun ComposeContentTestRule.setTall(content: @Composable () -> Unit) {
    setContent {
        MaintenanceTheme {
            Box(Modifier.requiredHeight(3000.dp)) { content() }
        }
    }
}

/** Scrolls a lazy list on a normal-size screen until the item containing this text is visible. */
fun ComposeContentTestRule.scrollListTo(text: String) {
    onNode(hasScrollAction()).performScrollToNode(hasText(text))
}
