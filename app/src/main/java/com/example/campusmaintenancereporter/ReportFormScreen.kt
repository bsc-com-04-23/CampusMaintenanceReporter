package com.example.campusmaintenancereporter

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(onBack: () -> Unit, onSubmit: (NewReport) -> String, onDone: () -> Unit) {
    var building by rememberSaveable { mutableStateOf("") }
    var block by rememberSaveable { mutableStateOf("") }
    var room by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var reference by rememberSaveable { mutableStateOf<String?>(null) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }

    val isSafety = category == "Safety"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Shrinks the chosen photo and keeps it, replacing any earlier one
    fun attach(source: Uri, deleteAfter: File? = null) {
        scope.launch {
            photoBusy = true
            val saved = withContext(Dispatchers.IO) {
                val result = PhotoUtils.compressToAppStorage(context, source)
                deleteAfter?.delete()
                result
            }
            photoBusy = false
            if (saved != null) {
                PhotoUtils.delete(photoPath)
                photoPath = saved
            } else {
                Toast.makeText(context, "That photo could not be used. Please try another.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (path != null) {
            val file = File(path)
            if (taken) attach(Uri.fromFile(file), deleteAfter = file) else file.delete()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) attach(uri)
    }

    fun openCamera() {
        val file = PhotoUtils.newCameraFile(context)
        pendingCameraPath = file.absolutePath
        try {
            takePicture.launch(PhotoUtils.cameraUri(context, file))
        } catch (e: ActivityNotFoundException) {
            pendingCameraPath = null
            Toast.makeText(context, "No camera app was found on this device.", Toast.LENGTH_SHORT).show()
        }
    }

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
                Priority.entries.forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                }
            }
            Text(
                "${priority.hint}\nTarget fix time: ${priority.targetDays} ${if (priority.targetDays == 1) "day" else "days"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle("4. Photo (optional)")
            val attached = photoPath
            if (attached != null) {
                PhotoImage(attached, Modifier.fillMaxWidth().height(180.dp), onClick = { showPhoto = true })
                if (showPhoto) FullScreenPhoto(attached, onClose = { showPhoto = false })
                OutlinedButton(
                    onClick = { PhotoUtils.delete(attached); photoPath = null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Remove photo") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { openCamera() },
                        enabled = !photoBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Take photo") }
                    OutlinedButton(
                        onClick = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !photoBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("From gallery") }
                }
            }
            if (photoBusy) {
                Text(
                    "Preparing photo...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        reference = onSubmit(
                            NewReport(location, category, title.trim(), description.trim(), priority, photoPath)
                        )
                    }
                },
                enabled = !photoBusy,
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