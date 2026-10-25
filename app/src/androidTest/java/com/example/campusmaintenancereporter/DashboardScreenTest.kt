package com.example.campusmaintenancereporter

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(role: Role, userName: String, tab: JobsTab = JobsTab.ALL) {
        val content: @Composable () -> Unit = {
            MaintenanceDashboardScreen(
                role = role,
                userName = userName,
                reports = SampleData.reports,
                tab = tab,
                onOpen = {},
                onNewReport = {},
                onLogout = {}
            )
        }
        rule.setTall(content)
    }

    @Test
    fun overdueTileFiltersToOverdueReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Overdue").onFirst().performClick()
        rule.onNodeWithText("Broken classroom light").assertExists()
        rule.onNodeWithText("No internet in lab").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
        rule.onNodeWithText("Exposed wiring near socket").assertDoesNotExist()
    }

    @Test
    fun urgentFilterShowsOnlyUrgentReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Urgent").onFirst().performClick()
        rule.onNodeWithText("Exposed wiring near socket").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun reportedFilterShowsOnlyReportedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Reported").onFirst().performClick()
        rule.onNodeWithText("Broken classroom light").assertExists()
        rule.onNodeWithText("Exposed wiring near socket").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun inProgressFilterShowsOnlyInProgressJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("In Progress").onFirst().performClick()
        rule.onNodeWithText("Leaking tap").assertExists()
        rule.onNodeWithText("No internet in lab").assertExists()
        rule.onNodeWithText("Broken desk").assertDoesNotExist()
    }

    @Test
    fun resolvedFilterShowsOnlyResolvedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Resolved").onFirst().performClick()
        rule.onNodeWithText("Broken desk").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun newTileFiltersToReportedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("New").performClick()
        rule.onNodeWithText("Broken classroom light").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun supervisorSeesTheLogAReportButton() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Log a report", useUnmergedTree = true).assertExists()
    }

    @Test
    fun technicianDoesNotSeeTheLogAReportButton() {
        show(Role.MAINTENANCE, "J. Banda")
        rule.onNodeWithText("Log a report", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun titlesFollowTheRoleAndTab() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Supervisor Dashboard").assertExists()
    }

    @Test
    fun allJobsShowsEveryReport() {
        show(Role.SUPERVISOR, "Supervisor")
        SampleData.reports.forEach { rule.onNodeWithText(it.title).assertExists() }
    }

    @Test
    fun unassignedTabShowsOnlyUnassignedReports() {
        show(Role.SUPERVISOR, "Supervisor", JobsTab.UNASSIGNED)
        rule.onNodeWithText("Unassigned Jobs").assertExists()
        rule.onNodeWithText("Broken classroom light").assertExists()
        rule.onNodeWithText("Exposed wiring near socket").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun myJobsTabShowsOnlyJobsAssignedToTheUser() {
        show(Role.MAINTENANCE, "J. Banda", JobsTab.MINE)
        rule.onNodeWithText("Leaking tap").assertExists()
        rule.onNodeWithText("No internet in lab").assertDoesNotExist()
    }

    @Test
    fun myJobsIsEmptyWhenNothingIsAssigned() {
        show(Role.MAINTENANCE, "Nobody", JobsTab.MINE)
        rule.onNodeWithText("No jobs are assigned to you yet").assertExists()
    }

    @Test
    fun escalatedFilterShowsOnlyEscalatedReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Escalated").performClick()
        rule.onNodeWithText("No internet in lab").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun searchNarrowsTheList() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Search title, location, reference").performTextInput("wiring")
        rule.onNodeWithText("Exposed wiring near socket").assertExists()
        rule.onNodeWithText("Leaking tap").assertDoesNotExist()
    }

    @Test
    fun searchWithNoMatchShowsTheEmptyMessage() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Search title, location, reference").performTextInput("zzzzzz")
        rule.onNodeWithText("No matching reports").assertExists()
    }
}