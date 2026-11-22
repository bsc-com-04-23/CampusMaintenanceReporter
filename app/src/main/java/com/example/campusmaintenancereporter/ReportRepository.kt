package com.example.campusmaintenancereporter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The single place reports live. Firestore replaces the inside of this class later. */
class ReportRepository {
    private val _reports = MutableStateFlow(SampleData.reports)
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    private var nextNumber = 1048

    fun add(newReport: NewReport, reporterEmail: String, reporterName: String): String {
        val id = "CMR-${nextNumber++}"
        val created = System.currentTimeMillis()
        val report = Report(
            id = id,
            title = newReport.title,
            category = newReport.category,
            location = newReport.location,
            description = newReport.description,
            priority = newReport.priority,
            status = Status.REPORTED,
            createdAt = created,
            timeline = listOf(TimelineEntry(formatDate(created, "d MMM, HH:mm"), reporterName, "Report submitted")),
            reporterEmail = reporterEmail,
            photoPath = newReport.photoPath
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
        escalate: Boolean = false
    ) {
        val time = formatDate(System.currentTimeMillis(), "d MMM, HH:mm")
        _reports.update { list ->
            list.map { r ->
                if (r.id != id) r else r.copy(
                    status = newStatus ?: r.status,
                    assignedTo = assignTo ?: r.assignedTo,
                    escalated = r.escalated || escalate,
                    timeline = r.timeline + TimelineEntry(time, author, text)
                )
            }
        }
    }
}