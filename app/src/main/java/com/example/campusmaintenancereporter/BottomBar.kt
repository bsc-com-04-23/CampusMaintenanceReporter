package com.example.campusmaintenancereporter


import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.tooling.preview.Preview

data class NavTab(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun AppBottomBar(tabs: List<NavTab>, selected: Int) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = index == selected,
                onClick = tab.onClick,
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    MaintenanceTheme {
        AppBottomBar(
            tabs = listOf(
                NavTab("Home", Icons.Default.Home) {},
                NavTab("Report", Icons.Default.Add) {},
                NavTab("My Reports", Icons.AutoMirrored.Filled.List) {}
            ),
            selected = 0
        )
    }
}