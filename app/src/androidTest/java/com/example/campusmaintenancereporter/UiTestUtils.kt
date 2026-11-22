package com.example.campusmaintenancereporter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue

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

/**
 * How many nodes with this text are really on screen. A swipe page next to the visible one can
 * still be in the tree, just off to the side, and those nodes do not count.
 */
fun ComposeContentTestRule.shownCount(text: String): Int {
    waitForIdle()
    val root = onRoot().fetchSemanticsNode().boundsInRoot
    return onAllNodesWithText(text).fetchSemanticsNodes().count {
        val b = it.boundsInRoot
        b.right > root.left && b.left < root.right
    }
}

fun ComposeContentTestRule.assertShown(text: String) {
    assertTrue("Expected \"$text\" to be on screen", shownCount(text) > 0)
}

fun ComposeContentTestRule.assertNotShown(text: String) {
    assertTrue("Did not expect \"$text\" to be on screen", shownCount(text) == 0)
}