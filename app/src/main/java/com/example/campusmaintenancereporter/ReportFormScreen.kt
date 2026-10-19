package com.example.campusmaintenancereporter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(onBack: () -> Unit, onSubmit: (NewReport) -> String, onDone: () -> Unit) {
    var building by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var photoAdded by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf<String?>(null) }

    val isSafety = category == "Safety"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar("Report a Problem", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionTitle("1. Where is the problem?")
            DropdownField(
                "Building or hostel", building, CampusOptions.buildings,
                onSelect = { building = it }, isError = tried && building.isEmpty()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    block, { block = it }, label = { Text("Block") },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    room, { room = it }, label = { Text("Room") },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
            }

            SectionTitle("2. What is wrong?")
            DropdownField(
                "Issue type", category, CampusOptions.categories,
                onSelect = {
                    category = it
                    if (it == "Safety") priority = Priority.HIGH
                },
                isError = tried && category.isEmpty()
            )
            if (isSafety) EmergencyNotice()
            OutlinedTextField(
                title, { title = it },
                label = { Text("Title (e.g. Leaking tap)") },
                isError = tried && title.isBlank(),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                description, { description = it },
                label = { Text("Describe the problem") },
                isError = tried && description.isBlank(),
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("3. How urgent is it?")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                }
            }
            Text(
                priority.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle("4. Photo (optional)")
            // TODO: replace with a real photo picker / camera + compression later
            OutlinedButton(
                onClick = { photoAdded = !photoAdded },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(if (photoAdded) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (photoAdded) "Photo attached (tap to remove)" else "Add a photo")
            }
            Text(
                "Please do not photograph people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    tried = true
                    if (building.isNotEmpty() && category.isNotEmpty() && title.isNotBlank() && description.isNotBlank()) {
                        val location = listOf(building, block, room).filter { it.isNotBlank() }.joinToString(", ")
                        reference = onSubmit(NewReport(location, category, title.trim(), description.trim(), priority))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Submit report", style = MaterialTheme.typography.titleMedium) }
        }
    }

    reference?.let { ref ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color.White,
            title = { Text("Report submitted") },
            text = {
                Column {
                    Text("Your reference number")
                    Text(ref, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Status: Reported. You can follow progress under My Reports.")
                }
            },
            confirmButton = { TextButton(onClick = onDone) { Text("Done") } }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReportFormScreenPreview() {
    MaintenanceTheme {
        ReportFormScreen(onBack = {}, onSubmit = { "CMR-0000" }, onDone = {})
    }
}