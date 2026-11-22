package com.example.campusmaintenancereporter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReportRepositoryTest {

    @Test
    fun addKeepsThePhotoPath() {
        repo.add(
            NewReport("Library", "Plumbing", "Leaking tap", "Drips", Priority.LOW, photoPath = "/data/photo.jpg"),
            "student@campus.test", "Student"
        )
        assertEquals("/data/photo.jpg", repo.reports.value.first().photoPath)
    }

    @Test
    fun aReportWithoutAPhotoHasNoPath() {
        repo.add(newReport(), "student@campus.test", "Student")
        assertNull(repo.reports.value.first().photoPath)
    }

    @Test
    fun newReportRecordsItsCreationTime() {
        val before = System.currentTimeMillis()
        repo.add(newReport(), "student@campus.test", "Student")
        val after = System.currentTimeMillis()

        val report = repo.reports.value.first()
        assertTrue(report.createdAt in before..after)
        assertTrue(report.date.isNotBlank())
        assertTrue(report.timeline.first().time.isNotBlank())
    }

    @Test
    fun everyChangeRecordsATimeOnTheTimeline() {
        repo.change("CMR-1047", "Supervisor", "Looking into it", newStatus = Status.IN_PROGRESS)
        assertTrue(find("CMR-1047").timeline.last().time.isNotBlank())
    }

    private lateinit var repo: ReportRepository

    @Before
    fun setUp() {
        repo = ReportRepository()
    }

    private fun newReport(priority: Priority = Priority.MEDIUM) =
        NewReport("Library", "Plumbing", "Leaking tap", "The tap drips", priority)

    private fun find(id: String) = repo.reports.value.first { it.id == id }

    @Test
    fun startsWithTheSampleReports() {
        assertEquals(SampleData.reports.size, repo.reports.value.size)
    }

    @Test
    fun addPutsTheNewReportFirstAsReported() {
        val id = repo.add(newReport(), "student@campus.test", "Student")
        val first = repo.reports.value.first()

        assertEquals(id, first.id)
        assertEquals(Status.REPORTED, first.status)
        assertEquals("student@campus.test", first.reporterEmail)
        assertNull(first.assignedTo)
        assertEquals(SampleData.reports.size + 1, repo.reports.value.size)
    }

    @Test
    fun referenceNumbersAreUniqueAndCountUp() {
        val a = repo.add(newReport(), "student@campus.test", "Student")
        val b = repo.add(newReport(), "student@campus.test", "Student")
        assertNotEquals(a, b)
        assertEquals("CMR-1048", a)
        assertEquals("CMR-1049", b)
    }

    @Test
    fun addRecordsTheSubmissionOnTheTimeline() {
        repo.add(newReport(), "student@campus.test", "Student")
        val timeline = repo.reports.value.first().timeline
        assertEquals(1, timeline.size)
        assertEquals("Report submitted", timeline.first().text)
        assertEquals("Student", timeline.first().author)
    }

    @Test
    fun addSetsTheDueDateFromThePriority() {
        repo.add(newReport(Priority.URGENT), "student@campus.test", "Student")
        val report = repo.reports.value.first()
        assertEquals(report.createdAt + DAY_MS, report.dueAt)
    }

    @Test
    fun changeUpdatesStatusAndAppendsToTheTimeline() {
        val before = find("CMR-1047").timeline.size
        repo.change("CMR-1047", "Supervisor", "Looking into it", newStatus = Status.IN_PROGRESS)

        val after = find("CMR-1047")
        assertEquals(Status.IN_PROGRESS, after.status)
        assertEquals(before + 1, after.timeline.size)
        assertEquals("Looking into it", after.timeline.last().text)
        assertEquals("Supervisor", after.timeline.last().author)
    }

    @Test
    fun changeLeavesOtherReportsUntouched() {
        val othersBefore = repo.reports.value.filter { it.id != "CMR-1047" }
        repo.change("CMR-1047", "Supervisor", "Looking into it", newStatus = Status.IN_PROGRESS)
        val othersAfter = repo.reports.value.filter { it.id != "CMR-1047" }
        assertEquals(othersBefore, othersAfter)
    }

    @Test
    fun changeCanAssignATechnician() {
        repo.change("CMR-1047", "Supervisor", "Assigned to P. Phiri", assignTo = "P. Phiri")
        assertEquals("P. Phiri", find("CMR-1047").assignedTo)
    }

    @Test
    fun changeWithoutAnAssigneeKeepsTheExistingTechnician() {
        repo.change("CMR-1042", "J. Banda", "Note")
        assertEquals("J. Banda", find("CMR-1042").assignedTo)
    }

    @Test
    fun escalatedFlagSticksOnce() {
        repo.change("CMR-1045", "Staff", "Escalated", escalate = true)
        repo.change("CMR-1045", "Staff", "Another note", escalate = false)
        assertTrue(find("CMR-1045").escalated)
    }

    @Test
    fun changingAnUnknownReportChangesNothing() {
        val before = repo.reports.value
        repo.change("CMR-NOPE", "Supervisor", "x", newStatus = Status.CLOSED)
        assertEquals(before, repo.reports.value)
    }
}