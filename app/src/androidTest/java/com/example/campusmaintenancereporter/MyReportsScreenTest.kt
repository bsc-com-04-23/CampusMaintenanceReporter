package com.example.campusmaintenancereporter

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        SampleData.reports.forEach { rule.onNodeWithText(it.title).assertExists() }
    }

    @Test
    fun statusChipsNarrowTheList() {
        show()
        rule.onAllNodesWithText("Resolved").onFirst().performClick()
        rule.onNodeWithText("Broken desk").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun anInitialFilterIsAlreadyApplied() {
        show(initialFilter = Status.CLOSED)
        rule.onNodeWithText("Cracked window").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun anEmptyListShowsAMessageAndAReportButton() {
        show(reports = emptyList())
        rule.onNodeWithText("No reports yet").assertExists()
        rule.onNodeWithText("Report a problem").performClick()
        assertTrue(reportClicked)
    }

    @Test
    fun aStatusWithNoReportsSaysSo() {
        show(reports = listOf(SampleData.reports[0])) // one In Progress report
        rule.onAllNodesWithText("Closed").onFirst().performClick()
        rule.onNodeWithText("Nothing with this status").assertExists()
    }
}