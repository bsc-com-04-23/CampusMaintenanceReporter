package com.example.campusmaintenancereporter

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyReportsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var reportClicked = false

    private fun show(reports: List<Report> = SampleData.reports, initialFilter: Status? = null) {
        reportClicked = false
        rule.setTall {
            MyReportsScreen(
                reports = reports,
                onBack = {},
                onOpen = {},
                initialFilter = initialFilter,
                onReport = { reportClicked = true }
            )
        }
    }

    @Test
    fun everyReportIsListedByDefault() {
        show()
        SampleData.reports.forEach { rule.assertShown(it.title) }
    }

    @Test
    fun statusChipsNarrowTheList() {
        show()
        rule.onAllNodesWithText("Resolved").onFirst().performClick()
        rule.assertShown("Broken desk")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun anInitialFilterIsAlreadyApplied() {
        show(initialFilter = Status.CLOSED)
        rule.assertShown("Cracked window")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun anEmptyListShowsAMessageAndAReportButton() {
        show(reports = emptyList())
        rule.assertShown("No reports yet")
        rule.onNodeWithText("Report a problem").performClick()
        assertTrue(reportClicked)
    }

    @Test
    fun aStatusWithNoReportsSaysSo() {
        show(reports = listOf(SampleData.reports[0])) // one In Progress report
        rule.onAllNodesWithText("Closed").onFirst().performClick()
        rule.assertShown("Nothing with this status")
    }

    @Test
    fun swipingLeftMovesToTheNextStatus() {
        show()
        rule.onNodeWithTag("reportPager").performTouchInput { swipeLeft() }
        // The page after "All" is "Reported"
        rule.assertShown("Broken classroom light")
        rule.assertNotShown("Leaking tap")
    }
}