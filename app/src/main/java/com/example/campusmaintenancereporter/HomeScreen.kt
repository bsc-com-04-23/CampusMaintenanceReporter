package com.example.campusmaintenancereporter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    role: Role,
    userName: String = "",
    reports: List<Report>,
    onReport: () -> Unit,
    onMyReports: () -> Unit,
    onStatusClick: (String) -> Unit,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    SignOutOnBack(onLogout)
    val mine = reports

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar("Campus Maintenance", actions = {
                AccountMenu(userName, role, onLogout)
            })
        },
        bottomBar = bottomBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hello, ${role.label}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "What would you like to do today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionCard(
                        "Report a Problem", Icons.Default.Add,
                        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
                        Modifier.weight(1f), onReport
                    )
                    ActionCard(
                        "My Reports", Icons.AutoMirrored.Filled.List,
                        MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.weight(1f), onMyReports
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Submitted", mine.size, Modifier.weight(1f), onClick = { onStatusClick("ALL") })
                    StatTile(
                        "In progress", mine.count { it.status == Status.IN_PROGRESS },
                        Modifier.weight(1f), Status.IN_PROGRESS.color, onClick = { onStatusClick("IN_PROGRESS") }
                    )
                    StatTile(
                        "Resolved", mine.count { it.status == Status.RESOLVED },
                        Modifier.weight(1f), Status.RESOLVED.color, onClick = { onStatusClick("RESOLVED") }
                    )
                }
            }
            item { EmergencyNotice(compact = true) }
            item { SectionTitle("Recent updates") }
            if (mine.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Info,
                        title = "No reports yet",
                        message = "Tap Report a Problem to log your first issue. You can follow it here."
                    )
                }
            } else {
                items(mine.take(3)) { report ->
                    ReportCard(report, onClick = { onOpen(report.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    AppCard(modifier = modifier, onClick = onClick, container = container) {
        Column(
            Modifier.fillMaxWidth().height(110.dp).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(32.dp))
            Text(title, color = content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    MaintenanceTheme {
        HomeScreen(
            role = Role.STUDENT,
            reports = SampleData.reports,
            onReport = {},
            onMyReports = {},
            onStatusClick = {},
            onOpen = {},
            onLogout = {}
        )
    }
}