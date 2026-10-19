package com.example.campusmaintenancereporter

import androidx.compose.ui.graphics.Color

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

enum class Priority(val label: String, val color: Color, val hint: String) {
    LOW("Low", Color(0xFF2E7D32), "Minor or cosmetic, e.g. a wobbly desk"),
    MEDIUM("Medium", Color(0xFFF9A825), "Affects normal use but has a workaround, e.g. a dripping tap"),
    HIGH("High", Color(0xFFEF6C00), "Loss of an essential service, e.g. no water or no power in a room"),
    URGENT("Urgent", Color(0xFFC62828), "Risk to safety or serious damage. Also call campus security.")
}

data class TimelineEntry(val time: String, val author: String, val text: String)

data class Report(
    val id: String,
    val title: String,
    val category: String,
    val location: String,
    val description: String,
    val priority: Priority,
    val status: Status,
    val date: String,
    val assignedTo: String? = null,
    val overdue: Boolean = false,
    val timeline: List<TimelineEntry> = emptyList(),
    val reporterEmail: String = "student@campus.test"
)

object CampusOptions {
    val categories = listOf(
        "Electrical", "Plumbing", "Furniture", "Building/Infrastructure",
        "Internet/Network", "Cleaning", "Safety", "Other"
    )
    val buildings = listOf(
        "Makata", "Umodzi", "Dunduzu", "Mwambo 1", "Wadonda",
        "Library", "Science Lab", "Sports Complex", "The Great Hall"
    )
}

/** Dummy data so the UI has something to show. Replace with Firebase later. */
object SampleData {
    val reports = listOf(
        Report(
            "CMR-1042", "Leaking tap", "Plumbing", "Mumba, Block 2, Room 14",
            "The tap in the laundry room drips constantly and the floor stays wet.",
            Priority.MEDIUM, Status.IN_PROGRESS, "1 Oct 2026, 08:15", "J. Banda",
            timeline = listOf(
                TimelineEntry("1 Oct, 08:15", "You", "Report submitted"),
                TimelineEntry("1 Oct, 14:40", "Supervisor", "Assigned to J. Banda"),
                TimelineEntry("3 Oct, 10:05", "J. Banda", "Waiting for replacement washer")
            )
        ),
        Report(
            "CMR-1045", "Broken classroom light", "Electrical", "Chikowi 2, Toilet 4",
            "Two fluorescent tubes are out. The back half of the room is dark.",
            Priority.MEDIUM, Status.REPORTED, "2 Oct 2026, 16:20", null, overdue = true,
            timeline = listOf(TimelineEntry("2 Oct, 16:20", "You", "Report submitted"))
        ),
        Report(
            "CMR-1038", "Broken desk", "Furniture", "Room A",
            "Desk leg has snapped and the desk cannot be used.",
            Priority.LOW, Status.RESOLVED, "28 Sep 2026, 09:00", "P. Phiri",
            timeline = listOf(
                TimelineEntry("28 Sep, 09:00", "You", "Report submitted"),
                TimelineEntry("29 Sep, 11:30", "P. Phiri", "Accepted the job"),
                TimelineEntry("4 Oct, 15:10", "P. Phiri", "Leg replaced and desk tested")
            )
        ),
        Report(
            "CMR-1047", "Exposed wiring near socket", "Safety", "Makata, Block 1, Room 3",
            "Wires are visible behind a broken socket cover.",
            Priority.URGENT, Status.REPORTED, "5 Oct 2026, 07:50", null,
            timeline = listOf(TimelineEntry("5 Oct, 07:50", "You", "Report submitted"))
        ),
        Report(
            "CMR-1030", "No internet in lab", "Internet/Network", "Science Lab",
            "Access point is down. No student can connect.",
            Priority.HIGH, Status.IN_PROGRESS, "25 Sep 2026, 10:10", "T. Mussa", overdue = true,
            timeline = listOf(
                TimelineEntry("25 Sep, 10:10", "You", "Report submitted"),
                TimelineEntry("26 Sep, 09:00", "T. Mussa", "Accepted the job")
            )
        ),
        Report(
            "CMR-1021", "Cracked window", "Building/Infrastructure", "Library, Reading Hall",
            "Window pane near the east door is cracked.",
            Priority.LOW, Status.CLOSED, "15 Sep 2026, 13:00", "P. Phiri",
            timeline = listOf(
                TimelineEntry("15 Sep, 13:00", "You", "Report submitted"),
                TimelineEntry("20 Sep, 12:00", "P. Phiri", "Pane replaced"),
                TimelineEntry("21 Sep, 08:30", "You", "Repair confirmed")
            )
        )
    )
}
/**
 * DEMO ONLY. Stands in for the server until Firebase is added.
 * A real app never stores passwords or roles inside the app itself.
 */
object DemoAccounts {
    private data class Account(val password: String, val role: Role)

    private val accounts = mapOf(
        "student@campus.test" to Account("student123", Role.STUDENT),
        "staff@campus.test" to Account("staff123", Role.STAFF),
        "maintenance@campus.test" to Account("maint123", Role.MAINTENANCE),
        "supervisor@campus.test" to Account("super123", Role.SUPERVISOR)
    )

    /** Returns the user's role if the credentials are right, otherwise null. */
    fun login(email: String, password: String): Role? {
        val account = accounts[email.trim().lowercase()] ?: return null
        return if (account.password == password) account.role else null
    }
}

data class NewReport(
    val location: String,
    val category: String,
    val title: String,
    val description: String,
    val priority: Priority
)

data class Session(val email: String, val role: Role)
