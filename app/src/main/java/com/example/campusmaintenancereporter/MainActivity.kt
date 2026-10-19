package com.example.campusmaintenancereporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaintenanceTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val session by vm.session.collectAsState()
    val reports by vm.visibleReports.collectAsState()
    val role = session?.role ?: Role.STUDENT

    val logout: () -> Unit = {
        vm.signOut()
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }
    val openDetails: (String) -> Unit = { id -> nav.navigate("details/$id") }

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen { email, pickedRole ->
                vm.signIn(email, pickedRole)
                nav.navigate(if (pickedRole.isMaintenance) "dashboard" else "home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(
                role = role,
                reports = reports,
                onReport = { nav.navigate("report") },
                onMyReports = { nav.navigate("myreports") },
                onStatusClick = { s -> nav.navigate("myreports?status=$s") },
                onOpen = openDetails,
                onLogout = logout
            )
        }
        composable("report") {
            ReportFormScreen(
                onBack = { nav.popBackStack() },
                onSubmit = { vm.submitReport(it) },
                onDone = { nav.popBackStack() }
            )
        }
        composable(
            route = "myreports?status={status}",
            arguments = listOf(navArgument("status") {
                type = NavType.StringType
                defaultValue = "ALL"
            })
        ) { entry ->
            val wanted = entry.arguments?.getString("status")
            MyReportsScreen(
                reports = reports,
                onBack = { nav.popBackStack() },
                onOpen = openDetails,
                initialFilter = Status.entries.firstOrNull { it.name == wanted }
            )
        }
        composable("dashboard") {
            MaintenanceDashboardScreen(
                role = role,
                reports = reports,
                onOpen = openDetails,
                onNewReport = { nav.navigate("report") },
                onLogout = logout
            )
        }
    }
}