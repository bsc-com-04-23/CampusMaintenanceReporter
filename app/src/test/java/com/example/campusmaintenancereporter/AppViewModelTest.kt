package com.example.campusmaintenancereporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private lateinit var vm: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = AppViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun signInStudent() = vm.signIn("student@campus.test", Role.STUDENT)
    private fun signInStaff() = vm.signIn("staff@campus.test", Role.STAFF)
    private fun signInBanda() = vm.signIn("maintenance@campus.test", Role.MAINTENANCE)
    private fun signInPhiri() = vm.signIn("phiri@campus.test", Role.MAINTENANCE)
    private fun signInMussa() = vm.signIn("mussa@campus.test", Role.MAINTENANCE)
    private fun signInSupervisor() = vm.signIn("supervisor@campus.test", Role.SUPERVISOR)

    /** Looks a report up through the current session's view of the data. */
    private fun report(id: String): Report = vm.visibleReports.value.first { it.id == id }
    private fun ids(): List<String> = vm.visibleReports.value.map { it.id }

    // ---------- Who sees what ----------

    @Test
    fun nobodySignedInSeesNoReports() {
        assertTrue(vm.visibleReports.value.isEmpty())
    }

    @Test
    fun studentSeesOnlyOwnReports() {
        signInStudent()
        val shown = vm.visibleReports.value
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.reporterEmail == "student@campus.test" })
        assertEquals(SampleData.reports.count { it.reporterEmail == "student@campus.test" }, shown.size)
    }

    @Test
    fun staffSeesOnlyOwnReports() {
        signInStaff()
        assertEquals(listOf("CMR-1045"), ids())
    }

    @Test
    fun supervisorSeesEverything() {
        signInSupervisor()
        assertEquals(SampleData.reports.size, vm.visibleReports.value.size)
    }

    @Test
    fun technicianSeesOnlyTheirOwnJobs() {
        signInBanda()
        assertEquals(listOf("CMR-1042"), ids())
        signInPhiri()
        assertEquals(listOf("CMR-1038", "CMR-1021"), ids())
        signInMussa()
        assertEquals(listOf("CMR-1030"), ids())
    }

    @Test
    fun technicianDoesNotSeeUnassignedJobs() {
        signInBanda()
        assertFalse("CMR-1045" in ids())
        assertFalse("CMR-1047" in ids())
    }

    @Test
    fun signOutHidesAllReports() {
        signInSupervisor()
        vm.signOut()
        assertTrue(vm.visibleReports.value.isEmpty())
    }

    @Test
    fun submittedReportBelongsToTheSubmitterOnly() {
        signInStudent()
        val id = vm.submitReport(NewReport("Library", "Plumbing", "Leaking tap", "Drips", Priority.LOW))
        assertEquals(id, vm.visibleReports.value.first().id)

        signInStaff()
        assertTrue(vm.visibleReports.value.none { it.id == id })

        signInSupervisor()
        assertTrue(vm.visibleReports.value.any { it.id == id })
    }

    // ---------- Assigning ----------

    @Test
    fun supervisorCanAssignATechnician() {
        signInSupervisor()
        vm.assign("CMR-1047", "P. Phiri")
        val updated = report("CMR-1047")
        assertEquals("P. Phiri", updated.assignedTo)
        assertEquals("Assigned to P. Phiri", updated.timeline.last().text)
        assertEquals("Supervisor", updated.timeline.last().author)
    }

    @Test
    fun technicianCannotAssign() {
        signInBanda()
        vm.assign("CMR-1042", "P. Phiri")
        assertEquals("J. Banda", report("CMR-1042").assignedTo)
    }

    // ---------- Doing the work ----------

    @Test
    fun technicianCannotStartAnUnassignedJob() {
        signInBanda()
        vm.updateStatus("CMR-1047", Status.IN_PROGRESS, "On my way")

        signInSupervisor()
        val job = report("CMR-1047")
        assertEquals(Status.REPORTED, job.status)
        assertNull(job.assignedTo)
    }

    @Test
    fun supervisorUpdatingAJobDoesNotTakeOwnership() {
        signInSupervisor()
        vm.updateStatus("CMR-1047", Status.IN_PROGRESS, "Started")
        val updated = report("CMR-1047")
        assertEquals(Status.IN_PROGRESS, updated.status)
        assertNull(updated.assignedTo)
    }

    @Test
    fun technicianCannotUpdateAnotherTechniciansJob() {
        signInPhiri()
        vm.updateStatus("CMR-1042", Status.RESOLVED, "Done")

        signInSupervisor()
        assertEquals(Status.IN_PROGRESS, report("CMR-1042").status)
        assertEquals("J. Banda", report("CMR-1042").assignedTo)
    }

    @Test
    fun technicianCannotCloseOrReopen() {
        signInBanda()
        vm.updateStatus("CMR-1042", Status.CLOSED, "Closing")
        vm.updateStatus("CMR-1042", Status.REOPENED, "Reopening")
        assertEquals(Status.IN_PROGRESS, report("CMR-1042").status)
    }

    @Test
    fun notesAreRecordedWithoutChangingTheStatus() {
        signInBanda()
        vm.addNote("CMR-1042", "Parts ordered")
        val updated = report("CMR-1042")
        assertEquals(Status.IN_PROGRESS, updated.status)
        assertEquals("Parts ordered", updated.timeline.last().text)
        assertEquals("J. Banda", updated.timeline.last().author)
    }

    // ---------- Reporter confirm / reopen ----------

    @Test
    fun reporterCanConfirmOwnResolvedReport() {
        signInStudent()
        vm.updateStatus("CMR-1038", Status.CLOSED, "Repair confirmed")
        assertEquals(Status.CLOSED, report("CMR-1038").status)
    }

    @Test
    fun reporterCanReopenOwnResolvedReport() {
        signInStudent()
        vm.updateStatus("CMR-1038", Status.REOPENED, "Reopened: problem still exists")
        assertEquals(Status.REOPENED, report("CMR-1038").status)
    }

    @Test
    fun reporterCannotConfirmAReportThatIsNotResolved() {
        signInStudent()
        vm.updateStatus("CMR-1047", Status.CLOSED, "Closing")
        assertEquals(Status.REPORTED, report("CMR-1047").status)
    }

    @Test
    fun reporterCannotSetWorkStatuses() {
        signInStudent()
        vm.updateStatus("CMR-1047", Status.RESOLVED, "Fixed")
        vm.updateStatus("CMR-1038", Status.IN_PROGRESS, "Working")
        assertEquals(Status.REPORTED, report("CMR-1047").status)
        assertEquals(Status.RESOLVED, report("CMR-1038").status)
    }

    @Test
    fun reporterCannotAddNotes() {
        signInStudent()
        val before = report("CMR-1047").timeline.size
        vm.addNote("CMR-1047", "Sneaky note")
        assertEquals(before, report("CMR-1047").timeline.size)
    }

    // ---------- Still not fixed (3-day rule) ----------

    @Test
    fun escalationIsBlockedWithinThreeDays() {
        signInStudent()
        val before = report("CMR-1047").timeline.size // 5 hours old
        vm.escalate("CMR-1047")
        val after = report("CMR-1047")
        assertFalse(after.escalated)
        assertEquals(before, after.timeline.size)
    }

    @Test
    fun escalationIsAllowedAfterThreeDays() {
        signInStaff()
        vm.escalate("CMR-1045") // 8 days old
        val after = report("CMR-1045")
        assertTrue(after.escalated)
        assertEquals("Escalated: still not fixed", after.timeline.last().text)
        assertEquals("Staff", after.timeline.last().author)
    }

    @Test
    fun escalationOnlyWorksOnce() {
        signInStaff()
        vm.escalate("CMR-1045")
        vm.escalate("CMR-1045")
        val count = report("CMR-1045").timeline.count { it.text == "Escalated: still not fixed" }
        assertEquals(1, count)
    }

    @Test
    fun reporterCannotEscalateSomeoneElsesReport() {
        signInStudent()
        vm.escalate("CMR-1045") // belongs to staff
        signInSupervisor()
        assertFalse(report("CMR-1045").escalated)
    }

    @Test
    fun maintenanceCannotEscalate() {
        signInSupervisor()
        vm.escalate("CMR-1045")
        assertFalse(report("CMR-1045").escalated)
    }

    // ---------- Whole life of a report ----------

    @Test
    fun supervisorCannotConfirmOrReopenAReportersRepair() {
        signInSupervisor()
        vm.updateStatus("CMR-1038", Status.CLOSED, "Closing")
        vm.updateStatus("CMR-1038", Status.REOPENED, "Reopening")
        assertEquals(Status.RESOLVED, report("CMR-1038").status)
    }

    @Test
    fun aReportCanBeTakenFromSubmittedToClosed() {
        signInStudent()
        val id = vm.submitReport(NewReport("Library", "Plumbing", "Leaking tap", "Drips", Priority.LOW))

        signInSupervisor()
        vm.assign(id, "J. Banda")

        signInBanda()
        vm.updateStatus(id, Status.IN_PROGRESS, "Work started")
        vm.updateStatus(id, Status.RESOLVED, "Fixed")

        signInStudent()
        vm.updateStatus(id, Status.CLOSED, "Repair confirmed")

        val done = report(id)
        assertEquals(Status.CLOSED, done.status)
        assertEquals("J. Banda", done.assignedTo)
        assertEquals(
            listOf("Report submitted", "Assigned to J. Banda", "Work started", "Fixed", "Repair confirmed"),
            done.timeline.map { it.text }
        )
    }

    @Test
    fun aReopenedReportCanBeWorkedOnAgain() {
        signInStudent()
        vm.updateStatus("CMR-1038", Status.REOPENED, "Reopened: problem still exists")

        signInPhiri() // CMR-1038 is assigned to P. Phiri
        vm.updateStatus("CMR-1038", Status.IN_PROGRESS, "Back on it")
        vm.updateStatus("CMR-1038", Status.RESOLVED, "Fixed properly")
        assertEquals(Status.RESOLVED, report("CMR-1038").status)
    }

    @Test
    fun aClosedReportCannotBeChangedByMaintenance() {
        signInPhiri()
        vm.updateStatus("CMR-1021", Status.IN_PROGRESS, "Trying to restart")
        assertEquals(Status.CLOSED, report("CMR-1021").status)
    }
}