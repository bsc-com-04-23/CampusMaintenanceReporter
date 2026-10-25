package com.example.campusmaintenancereporter

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var clicked: String? = null

    private fun show(reports: List<Report> = SampleData.reports) {
        clicked = null
        rule.setTall {
            HomeScreen(
                role = Role.STUDENT,
                userName = "Student",
                reports = reports,
                onReport = { clicked = "report" },
                onMyReports = { clicked = "myreports" },
                onStatusClick = { clicked = it },
                onOpen = { clicked = "open:$it" },
                onLogout = {}
            )
        }
    }

    @Test
    fun homeShowsTheGreetingAndTheTwoMainActions() {
        show()
        rule.onNodeWithText("Hello, Student").assertExists()
        rule.onNodeWithText("Report a Problem").assertExists()
        rule.onNodeWithText("My Reports").assertExists()
    }

    @Test
    fun homeShowsTheEmergencyNote() {
        show()
        rule.onNodeWithText("Emergency? Contact campus security first, then report it here.").assertExists()
    }

    @Test
    fun summaryTilesShowTheCounts() {
        show()
        rule.onNode(hasText("Submitted").and(hasText("6"))).assertExists()
        rule.onNode(hasText("In progress").and(hasText("2"))).assertExists()
        rule.onNode(hasText("Resolved").and(hasText("1"))).assertExists()
    }

    @Test
    fun tappingTheTilesPassesTheMatchingFilter() {
        show()
        rule.onNode(hasText("Submitted").and(hasText("6"))).performClick()
        assertEquals("ALL", clicked)
        rule.onNode(hasText("In progress").and(hasText("2"))).performClick()
        assertEquals("IN_PROGRESS", clicked)
        rule.onNode(hasText("Resolved").and(hasText("1"))).performClick()
        assertEquals("RESOLVED", clicked)
    }

    @Test
    fun theMainButtonsOpenTheirScreens() {
        show()
        rule.onNodeWithText("Report a Problem").performClick()
        assertEquals("report", clicked)
        rule.onNodeWithText("My Reports").performClick()
        assertEquals("myreports", clicked)
    }

    @Test
    fun aNewUserSeesAFriendlyEmptyMessage() {
        show(reports = emptyList())
        rule.onNodeWithText("No reports yet").assertExists()
    }

    @Test
    fun recentUpdatesShowAtMostThreeReports() {
        show()
        rule.onNodeWithText("Leaking tap").assertExists()
        rule.onNodeWithText("Broken classroom light").assertExists()
        rule.onNodeWithText("Broken desk").assertExists()
        rule.onNodeWithText("Exposed wiring near socket").assertDoesNotExist()
    }
}