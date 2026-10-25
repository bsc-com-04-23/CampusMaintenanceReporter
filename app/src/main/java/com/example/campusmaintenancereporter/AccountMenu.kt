package com.example.campusmaintenancereporter


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Top-bar account button: shows who is signed in and offers Sign out. */
@Composable
fun AccountMenu(userName: String, role: Role, onLogout: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Account")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(userName.ifBlank { "Signed in" }, fontWeight = FontWeight.SemiBold)
                Text(
                    role.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Sign out") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    confirm = true
                }
            )
        }
    }

    if (confirm) {
        SignOutDialog(onConfirm = { confirm = false; onLogout() }, onDismiss = { confirm = false })
    }
}

@Composable
fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Sign out?") },
        text = { Text("You will need to sign in again to see your reports.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } }
    )
}

/** Put on a main screen: the phone's back button asks to sign out instead of closing the app. */
@Composable
fun SignOutOnBack(onLogout: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    BackHandler { confirm = true }
    if (confirm) {
        SignOutDialog(onConfirm = { confirm = false; onLogout() }, onDismiss = { confirm = false })
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountMenuPreview() {
    MaintenanceTheme {
        AccountMenu(userName = "Supervisor", role = Role.SUPERVISOR, onLogout = {})
    }
}