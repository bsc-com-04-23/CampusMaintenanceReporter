package com.example.campusmaintenancereporter


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The single place reports live. Firestore replaces the inside of this class later. */
class ReportRepository {
    private val _reports = MutableStateFlow(SampleData.reports)
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    private var nextNumber = 1048

    fun add(newReport: NewReport, reporterEmail: String): String {
        val id = "CMR-${nextNumber++}"
        val report = Report(
            id = id,
            title = newReport.title,
            category = newReport.category,
            location = newReport.location,
            description = newReport.description,
            priority = newReport.priority,
            status = Status.REPORTED,
            date = now("d MMM yyyy, HH:mm"),
            timeline = listOf(TimelineEntry(now("d MMM, HH:mm"), "You", "Report submitted")),
            reporterEmail = reporterEmail
        )
        _reports.update { listOf(report) + it }
        return id
    }

    fun change(
        id: String,
        author: String,
        text: String,
        newStatus: Status? = null,
        assignTo: String? = null,
        clearOverdue: Boolean = false
    ) {
        _reports.update { list ->
            list.map { r ->
                if (r.id != id) r else r.copy(
                    status = newStatus ?: r.status,
                    assignedTo = r.assignedTo ?: assignTo,
                    overdue = if (clearOverdue) false else r.overdue,
                    timeline = r.timeline + TimelineEntry(now("d MMM, HH:mm"), author, text)
                )
            }
        }
    }

    private fun now(pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}