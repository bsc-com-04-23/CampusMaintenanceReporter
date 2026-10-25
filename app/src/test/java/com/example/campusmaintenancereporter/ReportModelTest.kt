package com.example.campusmaintenancereporter


import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportModelTest {

    @Test
    fun categoriesMatchTheProjectList() {
        assertEquals(
            listOf(
                "Electrical", "Plumbing", "Furniture", "Building/Infrastructure",
                "Internet/Network", "Cleaning", "Safety", "Other"
            ),
            CampusOptions.categories
        )
    }

    @Test
    fun priorityLevelsAreLowMediumHighUrgent() {
        assertEquals(listOf("Low", "Medium", "High", "Urgent"), Priority.entries.map { it.label })
    }

    @Test
    fun everyPriorityHasGuidanceText() {
        Priority.entries.forEach { assertTrue(it.hint.isNotBlank()) }
    }

    @Test
    fun statusesMatchTheWorkflow() {
        assertEquals(
            listOf("Reported", "In Progress", "Resolved", "Closed", "Reopened"),
            Status.entries.map { it.label }
        )
    }

    @Test
    fun dashboardSortPutsOverdueJobsBeforeOnTimeJobs() {
        val onTime = testReport(priority = Priority.URGENT, ageMs = 0)
        val late = testReport(priority = Priority.LOW, ageMs = 20 * DAY_MS)
        val sorted = listOf(onTime, late).sortedForDashboard()
        assertEquals(listOf(Priority.LOW, Priority.URGENT), sorted.map { it.priority })
    }

    @Test
    fun dashboardSortPutsMoreUrgentJobsFirst() {
        val low = testReport(priority = Priority.LOW)
        val urgent = testReport(priority = Priority.URGENT)
        val sorted = listOf(low, urgent).sortedForDashboard()
        assertEquals(listOf(Priority.URGENT, Priority.LOW), sorted.map { it.priority })
    }

    @Test
    fun dashboardSortPutsFinishedJobsLast() {
        val finished = testReport(priority = Priority.URGENT, status = Status.CLOSED, ageMs = 30 * DAY_MS)
        val open = testReport(priority = Priority.LOW)
        val sorted = listOf(finished, open).sortedForDashboard()
        assertEquals(listOf(Status.REPORTED, Status.CLOSED), sorted.map { it.status })
    }

    @Test
    fun targetDaysMatchThePriorityRules() {
        assertEquals(1, Priority.URGENT.targetDays)
        assertEquals(2, Priority.HIGH.targetDays)
        assertEquals(5, Priority.MEDIUM.targetDays)
        assertEquals(10, Priority.LOW.targetDays)
    }

    @Test
    fun dueDateIsCreationTimePlusTargetDays() {
        Priority.entries.forEach { p ->
            val report = testReport(priority = p)
            assertEquals(report.createdAt + p.targetDays * DAY_MS, report.dueAt)
        }
    }

    @Test
    fun freshReportIsNotOverdue() {
        assertFalse(testReport(priority = Priority.URGENT).overdue)
    }

    @Test
    fun openReportPastItsDueDateIsOverdue() {
        // Medium = 5 days, so 6 days old is late
        assertTrue(testReport(priority = Priority.MEDIUM, ageMs = 6 * DAY_MS).overdue)
    }

    @Test
    fun openReportInsideItsTargetIsNotOverdue() {
        assertFalse(testReport(priority = Priority.MEDIUM, ageMs = 4 * DAY_MS).overdue)
    }

    @Test
    fun urgentReportBecomesOverdueAfterOneDay() {
        assertTrue(testReport(priority = Priority.URGENT, ageMs = 2 * DAY_MS).overdue)
    }

    @Test
    fun resolvedAndClosedReportsAreNeverOverdue() {
        assertFalse(testReport(status = Status.RESOLVED, ageMs = 30 * DAY_MS).overdue)
        assertFalse(testReport(status = Status.CLOSED, ageMs = 30 * DAY_MS).overdue)
    }

    @Test
    fun reopenedReportCanBeOverdueAgain() {
        assertTrue(testReport(status = Status.REOPENED, ageMs = 30 * DAY_MS).overdue)
    }

    @Test
    fun onlyUnfinishedStatusesCountAsOpen() {
        Status.entries.forEach { s ->
            val expected = s != Status.RESOLVED && s != Status.CLOSED
            assertEquals(expected, testReport(status = s).isOpen)
        }
    }

    @Test
    fun escalationOpensThreeDaysAfterCreation() {
        val report = testReport()
        assertEquals(3, ESCALATION_DAYS)
        assertEquals(report.createdAt + 3 * DAY_MS, report.escalationOpensAt)
    }

    @Test
    fun onlyMaintenanceAndSupervisorAreMaintenanceRoles() {
        assertTrue(Role.MAINTENANCE.isMaintenance)
        assertTrue(Role.SUPERVISOR.isMaintenance)
        assertFalse(Role.STUDENT.isMaintenance)
        assertFalse(Role.STAFF.isMaintenance)
    }
}
