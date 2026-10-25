package com.example.campusmaintenancereporter

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReportDetailsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun report(id: String) = SampleData.reports.first { it.id == id }

    private fun show(
        id: String,
        role: Role,
        userName: String,
        tall: Boolean = false,
        onStatus: (Status, String) -> Unit = { _, _ -> },
        onNote: (String) -> Unit = {},
        onEscalate: () -> Unit = {},
        onAssign: (String) -> Unit = {}
    ) {
        val content: @Composable () -> Unit = {
            ReportDetailsScreen(
                report = report(id),
                role = role,
                userName = userName,
                onBack = {},
                onStatus = onStatus,
                onNote = onNote,
                onEscalate = onEscalate,
                onAssign = onAssign
            )
        }
        if (tall) rule.setTall(content) else rule.setContent { MaintenanceTheme { content() } }
    }

    private fun clickButton(text: String) {
        rule.onAllNodesWithText(text).filterToOne(hasClickAction()).performClick()
    }

    // ---------- Reporter: confirm or reopen ----------

    @Test
    fun detailsShowEveryFieldOfTheReport() {
        show("CMR-1042", Role.STUDENT, "Student", tall = true)
        rule.onNodeWithText("CMR-1042").assertExists()
        rule.onNodeWithText("Leaking tap").assertExists()
        rule.onNodeWithText("In Progress").assertExists()
        rule.onNodeWithText("Medium").assertExists()
        rule.onNodeWithText("Plumbing").assertExists()
        rule.onNodeWithText("Mumba, Block 2, Room 14").assertExists()
        rule.onNodeWithText("Reported").assertExists()
        rule.onNodeWithText("Due").assertExists()
        rule.onNodeWithText("Assigned to").assertExists()
        rule.onNodeWithText("J. Banda").assertExists()
        rule.onNodeWithText("The tap in the laundry room drips constantly and the floor stays wet.").assertExists()
        rule.onNodeWithText("Photo").assertExists()
    }

    @Test
    fun anUnassignedReportSaysNotYetAssigned() {
        show("CMR-1047", Role.STUDENT, "Student", tall = true)
        rule.onNodeWithText("Not yet assigned").assertExists()
    }

    @Test
    fun theTimelineShowsEveryUpdateWithItsAuthor() {
        show("CMR-1042", Role.STUDENT, "Student", tall = true)
        rule.onNodeWithText("Report submitted").assertExists()
        rule.onNodeWithText("Assigned to J. Banda").assertExists()
        rule.onNodeWithText("Waiting for replacement washer").assertExists()
        rule.onNodeWithText("You · ", substring = true).assertExists()
        rule.onNodeWithText("Supervisor · ", substring = true).assertExists()
        rule.onNodeWithText("J. Banda · ", substring = true).assertExists()
    }

    @Test
    fun reporterCanConfirmAResolvedRepair() {
        var captured: Pair<Status, String>? = null
        show("CMR-1038", Role.STUDENT, "Student", onStatus = { s, t -> captured = s to t })
        rule.scrollListTo("Confirm repair")
        rule.onNodeWithText("Confirm repair").performClick()
        assertEquals(Status.CLOSED to "Repair confirmed", captured)
    }

    @Test
    fun reporterCanReopenAResolvedRepair() {
        var captured: Pair<Status, String>? = null
        show("CMR-1038", Role.STUDENT, "Student", onStatus = { s, t -> captured = s to t })
        rule.scrollListTo("Reopen report")
        rule.onNodeWithText("Reopen report").performClick()
        assertEquals(Status.REOPENED to "Reopened: problem still exists", captured)
    }

    @Test
    fun confirmButtonsAreHiddenWhileTheJobIsNotResolved() {
        show("CMR-1047", Role.STUDENT, "Student", tall = true)
        rule.onNodeWithText("Confirm repair").assertDoesNotExist()
        rule.onNodeWithText("Reopen report").assertDoesNotExist()
    }

    // ---------- Reporter: still not fixed ----------

    @Test
    fun stillNotFixedIsLockedForANewReport() {
        show("CMR-1047", Role.STUDENT, "Student", tall = true)
        rule.onNodeWithText("Still not fixed").assertIsNotEnabled()
        rule.onNodeWithText("Available in", substring = true).assertExists()
    }

    @Test
    fun stillNotFixedWorksOnceThreeDaysHavePassed() {
        var called = false
        show("CMR-1045", Role.STAFF, "Staff", onEscalate = { called = true })
        rule.scrollListTo("Still not fixed")
        rule.onNodeWithText("Still not fixed").assertIsEnabled().performClick()
        assertTrue(called)
    }

    @Test
    fun anAlreadyEscalatedReportCannotBeEscalatedAgain() {
        show("CMR-1030", Role.STUDENT, "Student", tall = true)
        // "Supervisor notified" appears as both a detail row and the disabled button
        rule.onAllNodesWithText("Supervisor notified").assertCountEquals(2)
        rule.onAllNodesWithText("Supervisor notified").filterToOne(isNotEnabled()).assertExists()
    }

    // ---------- Technician ----------

    @Test
    fun technicianSeesTheUpdateCardOnTheirOwnJob() {
        show("CMR-1042", Role.MAINTENANCE, "J. Banda", tall = true)
        rule.onNodeWithText("Update this job").assertExists()
    }

    @Test
    fun technicianCanMarkTheirJobResolvedWithADefaultNote() {
        var captured: Pair<Status, String>? = null
        show("CMR-1042", Role.MAINTENANCE, "J. Banda", onStatus = { s, t -> captured = s to t })
        rule.scrollListTo("Update this job")
        clickButton("Resolved")
        assertEquals(Status.RESOLVED to "Problem fixed", captured)
    }

    @Test
    fun technicianNoteIsPassedAlongWithTheStatus() {
        var captured: Pair<Status, String>? = null
        show("CMR-1042", Role.MAINTENANCE, "J. Banda", onStatus = { s, t -> captured = s to t })
        rule.scrollListTo("Update this job")
        rule.onNodeWithText("Progress note").performTextInput("Washer replaced")
        clickButton("Resolved")
        assertEquals(Status.RESOLVED to "Washer replaced", captured)
    }

    @Test
    fun addNoteOnlyIsDisabledUntilSomethingIsTyped() {
        var note: String? = null
        show("CMR-1042", Role.MAINTENANCE, "J. Banda", onNote = { note = it })
        rule.scrollListTo("Update this job")
        rule.onNodeWithText("Add note only").assertIsNotEnabled()
        rule.onNodeWithText("Progress note").performTextInput("Waiting for parts")
        rule.onNodeWithText("Add note only").assertIsEnabled().performClick()
        assertEquals("Waiting for parts", note)
    }

    @Test
    fun technicianCannotUpdateAnotherTechniciansJob() {
        show("CMR-1042", Role.MAINTENANCE, "P. Phiri", tall = true)
        rule.onNodeWithText("This job is assigned to J. Banda", substring = true).assertExists()
        rule.onNodeWithText("Update this job").assertDoesNotExist()
    }

    // ---------- Supervisor ----------

    @Test
    fun supervisorSeesTheAssignCardOnAnOpenJob() {
        show("CMR-1047", Role.SUPERVISOR, "Supervisor", tall = true)
        rule.onNodeWithText("Assign technician").assertExists()
    }

    @Test
    fun technicianDoesNotSeeTheAssignCard() {
        show("CMR-1042", Role.MAINTENANCE, "J. Banda", tall = true)
        rule.onNodeWithText("Assign technician").assertDoesNotExist()
    }

    @Test
    fun supervisorCanPickATechnicianAndAssign() {
        var assigned: String? = null
        show("CMR-1047", Role.SUPERVISOR, "Supervisor", onAssign = { assigned = it })
        rule.scrollListTo("Assign technician")
        rule.onNodeWithText("Assign").assertIsNotEnabled()
        rule.onNodeWithText("Technician").performClick()
        rule.onNodeWithText("P. Phiri").performClick()
        rule.onNodeWithText("Assign").assertIsEnabled().performClick()
        assertEquals("P. Phiri", assigned)
    }

    @Test
    fun resolvedJobsHaveNoAssignOrUpdateCards() {
        show("CMR-1038", Role.SUPERVISOR, "Supervisor", tall = true)
        rule.onNodeWithText("Assign technician").assertDoesNotExist()
        rule.onNodeWithText("Update this job").assertDoesNotExist()
    }

    // ---------- Missing report ----------

    @Test
    fun anUnknownReportShowsANotFoundMessage() {
        rule.setContent {
            MaintenanceTheme {
                ReportDetailsScreen(
                    report = null, role = Role.STUDENT, userName = "Student", onBack = {},
                    onStatus = { _, _ -> }, onNote = {}, onEscalate = {}, onAssign = {}
                )
            }
        }
        rule.onNodeWithText("Report not found").assertExists()
    }
}