package com.example.campusmaintenancereporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailsScreen(
    report: Report?,
    role: Role,
    userName: String,
    onBack: () -> Unit,
    onStatus: (Status, String) -> Unit,
    onNote: (String) -> Unit,
    onEscalate: () -> Unit,
    onAssign: (String) -> Unit,
    loadPhoto: suspend (String) -> String? = { null }
) {
    if (report == null) {
        Scaffold(topBar = { AppTopBar("Report Details", onBack) }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Report not found")
            }
        }
        return
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    var showPhoto by remember { mutableStateOf(false) }
    var photoPath by remember(report.id) { mutableStateOf(report.photoPath) }
    LaunchedEffect(report.id, report.hasPhoto) {
        if (report.hasPhoto && photoPath == null) photoPath = loadPhoto(report.id)
    }

    val isSupervisor = role == Role.SUPERVISOR
    val isTechnician = role == Role.MAINTENANCE
    val isReporter = !role.isMaintenance
    val isMyJob = report.assignedTo == userName
    val canUpdate = isSupervisor || (isTechnician && isMyJob)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(report.id, onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                AppCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(report.status)
                            if (report.overdue) {
                                Spacer(Modifier.width(8.dp))
                                OverdueBadge()
                            }
                        }
                        Text(report.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        PriorityTag(report.priority)
                    }
                }
            }

            // Details
            item {
                val dueText = when {
                    !report.isOpen -> "Completed"
                    report.overdue -> "${report.dueLabel} (overdue)"
                    else -> report.dueLabel
                }
                AppCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("Category", report.category)
                        InfoRow("Location", report.location)
                        InfoRow("Reported", report.date)
                        InfoRow("Due", dueText)
                        InfoRow("Assigned to", report.assignedTo ?: "Not yet assigned")
                        if (report.escalated) InfoRow("Escalated", "Supervisor notified")
                        HorizontalDivider()
                        Text(report.description, style = MaterialTheme.typography.bodyMedium)

                        // The reporter's photo, or a grey box when there is none
                        val photo = photoPath
                        if (photo != null) {
                            PhotoImage(
                                photo,
                                Modifier.fillMaxWidth().height(220.dp),
                                onClick = { showPhoto = true }
                            )
                            Text(
                                "Tap the photo to view it full size",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (showPhoto) FullScreenPhoto(photo, onClose = { showPhoto = false })
                        } else {
                            Box(
                                Modifier.fillMaxWidth().height(150.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text(if (report.hasPhoto) "Loading photo..." else "Photo", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }

            // Supervisor: assign a technician
            if (isSupervisor && report.isOpen) {
                item { AssignCard(report, onAssign, toast) }
            }

            // Technician (own jobs only) or supervisor: update the job
            if (canUpdate && report.isOpen) {
                item { UpdateCard(onStatus, onNote, toast) }
            }

            // Technician looking at a job that is not theirs
            if (isTechnician && !isMyJob && report.isOpen) {
                item {
                    AppCard {
                        Text(
                            if (report.assignedTo == null) {
                                "This job has not been assigned yet. A supervisor will assign it."
                            } else {
                                "This job is assigned to ${report.assignedTo}. Only they or a supervisor can update it."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Reporter: confirm or reopen
            if (isReporter && report.status == Status.RESOLVED) {
                item {
                    AppCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("Is it really fixed?")
                            Text(
                                "Maintenance says this problem is resolved. Please confirm so the report can be closed.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = {
                                    onStatus(Status.CLOSED, "Repair confirmed")
                                    toast("Report closed. Thank you!")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Confirm repair") }
                            OutlinedButton(
                                onClick = {
                                    onStatus(Status.REOPENED, "Reopened: problem still exists")
                                    toast("Report reopened")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Reopen report") }
                        }
                    }
                }
            }

            // Reporter: "Still not fixed" (unlocks after 3 days)
            if (isReporter && report.isOpen) {
                item { EscalateCard(report, onEscalate, toast) }
            }

            // Timeline
            item { SectionTitle("Timeline") }
            itemsIndexed(report.timeline) { index, entry ->
                TimelineRow(entry, userName, isLast = index == report.timeline.lastIndex)
            }
        }
    }
}

@Composable
private fun AssignCard(report: Report, onAssign: (String) -> Unit, toast: (String) -> Unit) {
    var technician by remember { mutableStateOf(report.assignedTo ?: "") }
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Assign technician")
            DropdownField("Technician", technician, CampusOptions.technicians, onSelect = { technician = it })
            Button(
                onClick = {
                    onAssign(technician)
                    toast("Assigned to $technician")
                },
                enabled = technician.isNotEmpty() && technician != report.assignedTo,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (report.assignedTo == null) "Assign" else "Reassign") }
        }
    }
}

@Composable
private fun UpdateCard(
    onStatus: (Status, String) -> Unit,
    onNote: (String) -> Unit,
    toast: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Update this job")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Progress note") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onStatus(Status.IN_PROGRESS, note.ifBlank { "Job accepted, work started" })
                        note = ""
                        toast("Marked In Progress")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("In Progress") }
                Button(
                    onClick = {
                        onStatus(Status.RESOLVED, note.ifBlank { "Problem fixed" })
                        note = ""
                        toast("Marked Resolved")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) { Text("Resolved") }
            }
            OutlinedButton(
                onClick = {
                    onNote(note)
                    note = ""
                    toast("Note added")
                },
                enabled = note.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add note only") }
        }
    }
}

@Composable
private fun EscalateCard(report: Report, onEscalate: () -> Unit, toast: (String) -> Unit) {
    val msLeft = report.escalationOpensAt - System.currentTimeMillis()
    val ready = msLeft <= 0
    val daysLeft = ((msLeft + DAY_MS - 1) / DAY_MS).toInt()

    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    onEscalate()
                    toast("Supervisor has been notified")
                },
                enabled = ready && !report.escalated,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (report.escalated) "Supervisor notified" else "Still not fixed") }
            Text(
                when {
                    report.escalated -> "A supervisor has been told this is still not fixed."
                    ready -> "Use this if nothing has happened. A supervisor will be notified and it is recorded on the timeline."
                    else -> "Available in $daysLeft ${if (daysLeft == 1) "day" else "days"} (3 days after reporting)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, userName: String, isLast: Boolean) {
    val author = if (entry.author == userName) "You" else entry.author
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 4.dp).size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            if (!isLast) {
                Box(
                    Modifier.width(2.dp).weight(1f)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        }
        Column(Modifier.padding(start = 8.dp, bottom = 16.dp)) {
            Text(entry.text, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$author · ${entry.time}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReportDetailsScreenPreview() {
    MaintenanceTheme {
        ReportDetailsScreen(
            report = SampleData.reports[2],
            role = Role.STUDENT,
            userName = "Student",
            onBack = {},
            onStatus = { _, _ -> },
            onNote = {},
            onEscalate = {},
            onAssign = {}
        )
    }
}