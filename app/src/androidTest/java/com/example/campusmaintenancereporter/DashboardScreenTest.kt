package com.example.campusmaintenancereporter

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(
        role: Role,
        userName: String,
        tab: JobsTab = JobsTab.ALL,
        reports: List<Report> = SampleData.reports
    ) {
        val content: @Composable () -> Unit = {
            MaintenanceDashboardScreen(
                role = role,
                userName = userName,
                reports = reports,
                tab = tab,
                onOpen = {},
                onNewReport = {},
                onLogout = {}
            )
        }
        rule.setTall(content)
    }

    // ---------- Filters ----------

    @Test
    fun overdueTileFiltersToOverdueReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Overdue").onFirst().performClick()
        rule.assertShown("Broken classroom light")
        rule.assertShown("No internet in lab")
        rule.assertNotShown("Leaking tap")
        rule.assertNotShown("Exposed wiring near socket")
    }

    @Test
    fun urgentFilterShowsOnlyUrgentReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Urgent").onFirst().performClick()
        rule.assertShown("Exposed wiring near socket")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun reportedFilterShowsOnlyReportedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Reported").onFirst().performClick()
        rule.assertShown("Broken classroom light")
        rule.assertShown("Exposed wiring near socket")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun inProgressFilterShowsOnlyInProgressJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("In Progress").onFirst().performClick()
        rule.assertShown("Leaking tap")
        rule.assertShown("No internet in lab")
        rule.assertNotShown("Broken desk")
    }

    @Test
    fun resolvedFilterShowsOnlyResolvedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onAllNodesWithText("Resolved").onFirst().performClick()
        rule.assertShown("Broken desk")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun escalatedFilterShowsOnlyEscalatedReports() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Escalated").performClick()
        rule.assertShown("No internet in lab")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun newTileFiltersToReportedJobs() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("New").performClick()
        rule.assertShown("Broken classroom light")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun swipingLeftMovesToTheNextFilter() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithTag("jobPager").performTouchInput { swipeLeft() }
        // The page after "All" is "Overdue"
        rule.assertShown("Broken classroom light")
        rule.assertNotShown("Leaking tap")
    }

    // ---------- Buttons and titles ----------

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
    fun technicianDashboardIsTitledMyJobs() {
        show(Role.MAINTENANCE, "J. Banda")
        rule.onNodeWithText("My Jobs").assertExists()
    }

    // ---------- Lists ----------

    @Test
    fun allJobsShowsEveryReport() {
        show(Role.SUPERVISOR, "Supervisor")
        SampleData.reports.forEach { rule.assertShown(it.title) }
    }

    @Test
    fun unassignedTabShowsOnlyUnassignedReports() {
        show(Role.SUPERVISOR, "Supervisor", JobsTab.UNASSIGNED)
        rule.onNodeWithText("Unassigned Jobs").assertExists()
        rule.assertShown("Broken classroom light")
        rule.assertShown("Exposed wiring near socket")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun myJobsTabShowsOnlyJobsAssignedToTheUser() {
        show(Role.MAINTENANCE, "J. Banda", JobsTab.MINE)
        rule.assertShown("Leaking tap")
        rule.assertNotShown("No internet in lab")
    }

    @Test
    fun myJobsIsEmptyWhenNothingIsAssigned() {
        show(Role.MAINTENANCE, "Nobody", JobsTab.MINE)
        rule.assertShown("No jobs are assigned to you yet")
    }

    @Test
    fun technicianWithNoJobsSeesAFriendlyMessage() {
        show(Role.MAINTENANCE, "J. Banda", reports = emptyList())
        rule.assertShown("No jobs are assigned to you yet")
    }

    // ---------- Search ----------

    @Test
    fun searchNarrowsTheList() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Search title, location, reference").performTextInput("wiring")
        rule.assertShown("Exposed wiring near socket")
        rule.assertNotShown("Leaking tap")
    }

    @Test
    fun searchWithNoMatchShowsTheEmptyMessage() {
        show(Role.SUPERVISOR, "Supervisor")
        rule.onNodeWithText("Search title, location, reference").performTextInput("zzzzzz")
        rule.assertShown("No matching reports")
    }
}