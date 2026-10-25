package com.example.campusmaintenancereporter


/** Builds a report for tests. ageMs = how long ago it was created. */
fun testReport(
    priority: Priority = Priority.MEDIUM,
    status: Status = Status.REPORTED,
    ageMs: Long = 0L
) = Report(
    id = "CMR-T1",
    title = "Test report",
    category = "Plumbing",
    location = "Library",
    description = "Test description",
    priority = priority,
    status = status,
    createdAt = System.currentTimeMillis() - ageMs
)