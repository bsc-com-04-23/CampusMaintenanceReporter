package com.example.campusmaintenancereporter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CheckCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDashboardScreen(
    role: Role,
    userName: String,
    reports: List<Report>,
    tab: JobsTab,
    onOpen: (String) -> Unit,
    onNewReport: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    SignOutOnBack(onLogout)
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Overdue", "Urgent", "Escalated", "Reported", "In Progress", "Resolved")

    val title = when (tab) {
        JobsTab.MINE -> "My Jobs"
        JobsTab.UNASSIGNED -> "Unassigned Jobs"
        JobsTab.ALL -> if (role == Role.SUPERVISOR) "Supervisor Dashboard" else "Maintenance Dashboard"
    }

    val base = when (tab) {
        JobsTab.ALL -> reports
        JobsTab.MINE -> reports.filter { it.assignedTo == userName }
        JobsTab.UNASSIGNED -> reports.filter { it.assignedTo == null }
    }

    val shown = base
        .filter { r ->
            val matchesFilter = when (filter) {
                "Overdue" -> r.overdue
                "Urgent" -> r.priority == Priority.URGENT
                "Escalated" -> r.escalated
                "Reported" -> r.status == Status.REPORTED
                "In Progress" -> r.status == Status.IN_PROGRESS
                "Resolved" -> r.status == Status.RESOLVED
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    listOf(r.title, r.location, r.id, r.category).any { it.contains(query, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
        // open jobs first, then overdue, then most urgent
        .sortedForDashboard()

    val (emptyTitle, emptyMessage) = when {
        base.isNotEmpty() -> "No matching reports" to "Try a different search or filter."
        tab == JobsTab.MINE -> "No jobs are assigned to you yet" to "New jobs from your supervisor will show up here."
        tab == JobsTab.UNASSIGNED -> "Every job has been assigned" to "Nothing is waiting for a technician."
        else -> "No reports yet" to "Reports from students and staff will appear here."
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title, actions = {
                AccountMenu(userName, role, onLogout)
            })
        },
        floatingActionButton = {
            if (role == Role.SUPERVISOR) {
                ExtendedFloatingActionButton(
                    onClick = onNewReport,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Log a report") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile("Overdue", base.count { it.overdue }, Modifier.weight(1f), Status.REOPENED.color, onClick = { filter = "Overdue" })
                StatTile("New", base.count { it.status == Status.REPORTED }, Modifier.weight(1f), Status.REPORTED.color, onClick = { filter = "Reported" })
                StatTile("In progress", base.count { it.status == Status.IN_PROGRESS }, Modifier.weight(1f), Status.IN_PROGRESS.color, onClick = { filter = "In Progress" })
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search title, location, reference") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                }
            }
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = if (base.isNotEmpty()) Icons.Default.Search else Icons.Default.CheckCircle,
                        title = emptyTitle,
                        message = emptyMessage
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown) { report ->
                        ReportCard(report, onClick = { onOpen(report.id) }, showAssignee = true)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MaintenanceDashboardScreenPreview() {
    MaintenanceTheme {
        MaintenanceDashboardScreen(
            role = Role.SUPERVISOR,
            userName = "Supervisor",
            reports = SampleData.reports,
            tab = JobsTab.ALL,
            onOpen = {},
            onNewReport = {},
            onLogout = {}
        )
    }
}