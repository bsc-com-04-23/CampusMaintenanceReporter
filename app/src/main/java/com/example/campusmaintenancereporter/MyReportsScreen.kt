package com.example.campusmaintenancereporter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    reports: List<Report>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    initialFilter: Status? = null,
    onReport: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {}
) {
    var filter by remember { mutableStateOf(initialFilter) }
    val shown = reports.filter { filter == null || it.status == filter }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("My Reports", onBack) },
        bottomBar = bottomBar
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                Status.entries.forEach { s ->
                    FilterChip(selected = filter == s, onClick = { filter = s }, label = { Text(s.label) })
                }
            }
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (reports.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Info,
                            title = "No reports yet",
                            message = "Problems you report will be listed here so you can follow their progress.",
                            actionLabel = if (onReport != null) "Report a problem" else null,
                            onAction = onReport
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "Nothing with this status",
                            message = "Try another filter above."
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shown) { report ->
                        ReportCard(report, onClick = { onOpen(report.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MyReportsScreenPreview() {
    MaintenanceTheme {
        MyReportsScreen(reports = SampleData.reports, onBack = {}, onOpen = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Empty")
@Composable
private fun MyReportsEmptyPreview() {
    MaintenanceTheme {
        MyReportsScreen(reports = emptyList(), onBack = {}, onOpen = {}, onReport = {})
    }
}