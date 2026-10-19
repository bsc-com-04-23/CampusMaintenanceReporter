package com.example.campusmaintenancereporter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(reports: List<Report>, onBack: () -> Unit, onOpen: (String) -> Unit, initialFilter: Status? = null) {
    var filter by remember { mutableStateOf(initialFilter) }
    val shown = reports.filter { filter == null || it.status == filter }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("My Reports", onBack) }
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
                    Text("No reports to show", color = MaterialTheme.colorScheme.onSurfaceVariant)
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