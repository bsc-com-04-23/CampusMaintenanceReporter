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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceDashboardScreen(
    role: Role,
    reports: List<Report>,
    onOpen: (String) -> Unit,
    onNewReport: () -> Unit,
    onLogout: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Overdue", "Urgent", "Reported", "In Progress", "Resolved")
    val all = reports

    val shown = all
        .filter { r ->
            val matchesFilter = when (filter) {
                "Overdue" -> r.overdue
                "Urgent" -> r.priority == Priority.URGENT
                "Reported" -> r.status == Status.REPORTED
                "In Progress" -> r.status == Status.IN_PROGRESS
                "Resolved" -> r.status == Status.RESOLVED
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                listOf(r.title, r.location, r.id, r.category).any { it.contains(query, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
        // urgent and overdue first
        .sortedWith(compareByDescending<Report> { it.overdue }.thenByDescending { it.priority.ordinal })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                if (role == Role.SUPERVISOR) "Supervisor Dashboard" else "Maintenance Dashboard",
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewReport,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New report") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile("Overdue", all.count { it.overdue }, Modifier.weight(1f), Status.REOPENED.color, onClick = { filter = "Overdue" })
                StatTile("New", all.count { it.status == Status.REPORTED }, Modifier.weight(1f), Status.REPORTED.color, onClick = { filter = "Reported" })
                StatTile("In progress", all.count { it.status == Status.IN_PROGRESS }, Modifier.weight(1f), Status.IN_PROGRESS.color, onClick = { filter = "In Progress" })
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
                    Text("No matching reports", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            reports = SampleData.reports,
            onOpen = {},
            onNewReport = {},
            onLogout = {}
        )
    }
}
