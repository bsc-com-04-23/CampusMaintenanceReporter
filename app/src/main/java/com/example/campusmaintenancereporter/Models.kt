package com.example.campusmaintenancereporter

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val DAY_MS = 86_400_000L
const val HOUR_MS = 3_600_000L
const val ESCALATION_DAYS = 3

fun formatDate(millis: Long, pattern: String = "d MMM yyyy, HH:mm"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

enum class Role(val label: String) {
    STUDENT("Student"), STAFF("Staff"), MAINTENANCE("Maintenance"), SUPERVISOR("Supervisor");

    val isMaintenance: Boolean get() = this == MAINTENANCE || this == SUPERVISOR
}

enum class Status(val label: String, val color: Color) {
    REPORTED("Reported", Color(0xFF1565C0)),
    IN_PROGRESS("In Progress", Color(0xFFEF6C00)),
    RESOLVED("Resolved", Color(0xFF2E7D32)),
    CLOSED("Closed", Color(0xFF616161)),
    REOPENED("Reopened", Color(0xFFC62828))
}

/** targetDays = how long the maintenance team has to fix it before it counts as overdue. */
enum class Priority(val label: String, val color: Color, val hint: String, val targetDays: Int) {
    LOW("Low", Color(0xFF2E7D32), "Minor or cosmetic, e.g. a wobbly desk", 10),
    MEDIUM("Medium", Color(0xFFB58900), "Affects normal use but has a workaround, e.g. a dripping tap", 5),
    HIGH("High", Color(0xFFEF6C00), "Loss of an essential service, e.g. no water or no power in a room", 2),
    URGENT("Urgent", Color(0xFFC62828), "Risk to safety or serious damage. Also call campus security.", 1)
}

enum class JobsTab { ALL, MINE, UNASSIGNED }

data class TimelineEntry(val time: String, val author: String, val text: String)

data class Report(
    val id: String,
    val title: String,
    val category: String,
    val location: String,
    val description: String,
    val priority: Priority,
    val status: Status,
    val createdAt: Long,
    val assignedTo: String? = null,
    val timeline: List<TimelineEntry> = emptyList(),
    val reporterEmail: String = "student@campus.test",
    val dueAt: Long = createdAt + priority.targetDays * DAY_MS,
    val escalated: Boolean = false
) {
    val date: String get() = formatDate(createdAt)
    val dueLabel: String get() = formatDate(dueAt, "d MMM")
    val isOpen: Boolean get() = status != Status.RESOLVED && status != Status.CLOSED
    val overdue: Boolean get() = isOpen && System.currentTimeMillis() > dueAt
    val escalationOpensAt: Long get() = createdAt + ESCALATION_DAYS * DAY_MS
}

object CampusOptions {
    val categories = listOf(
        "Electrical", "Plumbing", "Furniture", "Building/Infrastructure",
        "Internet/Network", "Cleaning", "Safety", "Other"
    )
    val buildings = listOf(
        "Makata", "Umodzi", "Dunduzu", "Mwambo 1", "Wadonda",
        "Library", "Science Lab", "Sports Complex", "The Great Hall"
    )
    val technicians = listOf("J. Banda", "P. Phiri", "T. Mussa")
}

/** Dummy data so the UI has something to show. Replace with Firebase later. */
object SampleData {
    private val now = System.currentTimeMillis()
    private fun ago(days: Int, hours: Int = 0): Long = now - days * DAY_MS - hours * HOUR_MS
    private fun t(millis: Long): String = formatDate(millis, "d MMM, HH:mm")

    private val c1042 = ago(3)
    private val c1045 = ago(8)
    private val c1038 = ago(9)
    private val c1047 = ago(0, 5)
    private val c1030 = ago(10)
    private val c1021 = ago(30)

    val reports = listOf(
        Report(
            "CMR-1042", "Leaking tap", "Plumbing", "Mumba, Block 2, Room 14",
            "The tap in the laundry room drips constantly and the floor stays wet.",
            Priority.MEDIUM, Status.IN_PROGRESS, createdAt = c1042, assignedTo = "J. Banda",
            timeline = listOf(
                TimelineEntry(t(c1042), "Student", "Report submitted"),
                TimelineEntry(t(c1042 + 6 * HOUR_MS), "Supervisor", "Assigned to J. Banda"),
                TimelineEntry(t(c1042 + 2 * DAY_MS), "J. Banda", "Waiting for replacement washer")
            )
        ),
        Report(
            "CMR-1045", "Broken classroom light", "Electrical", "Chikowi 2, Toilet 4",
            "Two fluorescent tubes are out. The back half of the room is dark.",
            Priority.MEDIUM, Status.REPORTED, createdAt = c1045, reporterEmail = "staff@campus.test",
            timeline = listOf(TimelineEntry(t(c1045), "Staff", "Report submitted"))
        ),
        Report(
            "CMR-1038", "Broken desk", "Furniture", "Room A",
            "Desk leg has snapped and the desk cannot be used.",
            Priority.LOW, Status.RESOLVED, createdAt = c1038, assignedTo = "P. Phiri",
            timeline = listOf(
                TimelineEntry(t(c1038), "Student", "Report submitted"),
                TimelineEntry(t(c1038 + DAY_MS), "P. Phiri", "Accepted the job"),
                TimelineEntry(t(c1038 + 5 * DAY_MS), "P. Phiri", "Leg replaced and desk tested")
            )
        ),
        Report(
            "CMR-1047", "Exposed wiring near socket", "Safety", "Makata, Block 1, Room 3",
            "Wires are visible behind a broken socket cover.",
            Priority.URGENT, Status.REPORTED, createdAt = c1047,
            timeline = listOf(TimelineEntry(t(c1047), "Student", "Report submitted"))
        ),
        Report(
            "CMR-1030", "No internet in lab", "Internet/Network", "Science Lab",
            "Access point is down. No student can connect.",
            Priority.HIGH, Status.IN_PROGRESS, createdAt = c1030, assignedTo = "T. Mussa", escalated = true,
            timeline = listOf(
                TimelineEntry(t(c1030), "Student", "Report submitted"),
                TimelineEntry(t(c1030 + DAY_MS), "T. Mussa", "Accepted the job"),
                TimelineEntry(t(c1030 + 4 * DAY_MS), "Student", "Escalated: still not fixed")
            )
        ),
        Report(
            "CMR-1021", "Cracked window", "Building/Infrastructure", "Library, Reading Hall",
            "Window pane near the east door is cracked.",
            Priority.LOW, Status.CLOSED, createdAt = c1021, assignedTo = "P. Phiri",
            timeline = listOf(
                TimelineEntry(t(c1021), "Student", "Report submitted"),
                TimelineEntry(t(c1021 + 5 * DAY_MS), "P. Phiri", "Pane replaced"),
                TimelineEntry(t(c1021 + 6 * DAY_MS), "Student", "Repair confirmed")
            )
        )
    )
}

/**
 * DEMO ONLY. Stands in for the server until Firebase is added.
 * A real app never stores passwords or roles inside the app itself.
 */
object DemoAccounts {
    private data class Account(val password: String, val role: Role, val name: String)

    private val accounts = mapOf(
        "student@campus.test" to Account("student123", Role.STUDENT, "Student"),
        "staff@campus.test" to Account("staff123", Role.STAFF, "Staff"),
        "maintenance@campus.test" to Account("maint123", Role.MAINTENANCE, "J. Banda"),
        "phiri@campus.test" to Account("phiri123", Role.MAINTENANCE, "P. Phiri"),
        "mussa@campus.test" to Account("mussa123", Role.MAINTENANCE, "T. Mussa"),
        "supervisor@campus.test" to Account("super123", Role.SUPERVISOR, "Supervisor")
    )

    /** Returns the user's role if the credentials are right, otherwise null. */
    fun login(email: String, password: String): Role? {
        val account = accounts[email.trim().lowercase()] ?: return null
        return if (account.password == password) account.role else null
    }

    fun nameOf(email: String): String =
        accounts[email.trim().lowercase()]?.name ?: email.substringBefore("@")
}

data class NewReport(
    val location: String,
    val category: String,
    val title: String,
    val description: String,
    val priority: Priority
)

data class Session(val email: String, val role: Role)
/** Dashboard order: open jobs first, then overdue, then the most urgent priority. */
fun List<Report>.sortedForDashboard(): List<Report> =
    sortedWith(
        compareBy<Report> { !it.isOpen }
            .thenByDescending { it.overdue }
            .thenByDescending { it.priority.ordinal }
    )